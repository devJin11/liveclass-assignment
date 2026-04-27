package com.liveclass.assignment.domain.enrollment.service;

import com.liveclass.assignment.domain.classmate.entity.Classmate;
import com.liveclass.assignment.domain.classmate.repository.ClassmateRepository;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus;
import com.liveclass.assignment.domain.classroom.repository.ClassRoomRepository;
import com.liveclass.assignment.domain.enrollment.dto.response.EnrollmentDetailResponse;
import com.liveclass.assignment.domain.enrollment.dto.response.EnrollmentResponse;
import com.liveclass.assignment.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment;
import com.liveclass.assignment.domain.enrollment.repository.EnrollmentRepository;
import com.liveclass.assignment.global.exception.BadRequestException;
import com.liveclass.assignment.global.exception.ForbiddenException;
import com.liveclass.assignment.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus.OPEN;
import static com.liveclass.assignment.global.exception.CustomExceptionCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

  private static final int EXPIRED_ENROLLMENT_BATCH_SIZE = 50;

  private final EnrollmentRepository enrollmentRepository;
  private final ClassRoomRepository classRoomRepository;
  private final ClassmateRepository classmateRepository;

  /**
   * 수강 신청.
   *
   * 없음 -> PENDING
   * CANCELLED -> PENDING
   *
   * 성공 시 class_room.enrollment_count + 1
   */
  @Transactional
  public EnrollmentResponse enroll(Long classRoomId, Long classmateId) {
    LocalDateTime now = LocalDateTime.now();

    Classmate classmate = getClassmateOrThrow(classmateId);

    // 1. 먼저 class_room 정원 증가를 원자적으로 시도한다.
    //    여기서 class_room row lock 경합이 먼저 발생하도록 락 순서를 통일한다.
    increaseEnrollmentCountOrThrow(classRoomId);

    // 2. 그 다음 동일 강의/동일 클래스메이트 신청 이력을 확인한다.
    Enrollment existingEnrollment = enrollmentRepository.
        findByClassRoomIdAndClassmateIdForUpdate(classRoomId, classmateId)
        .orElse(null);

    // 3) 이미 수강 신청 내역(PENDING or CONFIRMED) 존재하면 예외
    if (existingEnrollment != null && existingEnrollment.isSeatOccupied()) {
      throw new BadRequestException(DUPLICATED_ENROLLMENT);
    }

    Enrollment enrollment;

    if (existingEnrollment != null) { // 2) 수강 신청 내역이 존재하면서 CANCELLED 상태이면 재 수강신청
      existingEnrollment.reapply(now);
      enrollment = existingEnrollment;
    } else { // 3) 수강 신청 내역이 존재하지 않으면 신규 수강 신청 흐름
      ClassRoom classRoom = classRoomRepository.getReferenceById(classRoomId);
      enrollment = Enrollment.createEnrollment(classRoom, classmate, now);
      enrollmentRepository.save(enrollment);
    }

    return EnrollmentResponse.from(enrollment);
  }

  /**
   * 결제 확정.
   *
   * PENDING -> CONFIRMED
   * enrollment_count 변화 없음.
   */
  @Transactional
  public EnrollmentResponse confirm(Long enrollmentId, Long classmateId) {
    Enrollment enrollment = getEnrollmentOrThrow(enrollmentId);

    validateEnrollmentOwner(enrollment, classmateId);

    enrollment.confirm(LocalDateTime.now());

    return EnrollmentResponse.from(enrollment);
  }

  /**
   * 사용자 직접 취소.
   *
   * PENDING -> CANCELLED
   * CONFIRMED -> CANCELLED
   *
   * 좌석 점유 상태였으면 enrollment_count -1
   */
  @Transactional
  public EnrollmentResponse cancel(Long enrollmentId, Long classmateId) {
    Enrollment enrollment = getEnrollmentOrThrow(enrollmentId);

    validateEnrollmentOwner(enrollment, classmateId);

    // 좌석 점유 상태인지 여부(PENDING or CONFIRMED면 TRUE)
    boolean wasSeatOccupied = enrollment.isSeatOccupied();

    // 결제 취소 상태로 변경
    enrollment.cancelByUser(LocalDateTime.now());

    if (wasSeatOccupied) { // PENDING or CONFIRMED 상태였으면
      decreaseEnrollmentCount(enrollment.getClassRoomId());
    }

    return EnrollmentResponse.from(enrollment);
  }

  /**
   * 내 수강 신청 목록 조회.
   */
  public Page<MyEnrollmentResponse> getMyEnrollments(Long classmateId, Pageable pageable) {
    validateClassmateExists(classmateId);
    return enrollmentRepository.findMyEnrollments(classmateId, pageable);
  }

  /**
   * 결제 대기 만료 자동 취소.
   *
   * 스케줄러에서 호출한다.
   */
  @Transactional
  public int cancelExpiredPendingEnrollments() {
    LocalDateTime now = LocalDateTime.now();

    List<Enrollment> expiredEnrollments = enrollmentRepository.findExpiredPendingEnrollmentsForUpdate(
        now,
        PageRequest.of(0, EXPIRED_ENROLLMENT_BATCH_SIZE)
        );

    int cancelledCount = 0;

    for (Enrollment enrollment : expiredEnrollments) {
      boolean wasSeatOccupied = enrollment.isSeatOccupied();

      enrollment.cancelByPaymentExpired(now);

      // PENDING 상태이면서 결제 기간이 만료 되었다
      if (wasSeatOccupied) {
        decreaseEnrollmentCount(enrollment.getClassRoomId());
      }

      cancelledCount++;
    }

    return cancelledCount;
  }

  /**
   *  수강 신청 단건 상세 조회
   * */
  public EnrollmentDetailResponse getEnrollmentDetail(Long enrollmentId, Long classmateId) {
    EnrollmentDetailResponse response = enrollmentRepository.findEnrollmentDetail(enrollmentId)
        .orElseThrow(() -> new NotFoundException(ENROLLMENT_NOT_FOUND));

    if (!response.classmateId().equals(classmateId)) {
      throw new ForbiddenException(ENROLLMENT_FORBIDDEN);
    }

    return response;
  }

  private void increaseEnrollmentCountOrThrow(Long classRoomId) {
    int updated = classRoomRepository.tryIncreaseEnrollmentCount(classRoomId);

    if (updated == 1) {
      return;
    }

    ClassRoomStatus status = classRoomRepository.findStatusById(classRoomId);

    if (status == null) {
      throw new NotFoundException(CLASS_ROOM_NOT_FOUND);
    }

    if (status != OPEN) {
      throw new BadRequestException(CLASS_ROOM_NOT_OPEN);
    }

    throw new BadRequestException(CLASS_ROOM_CAPACITY_EXCEEDED);
  }

  private void decreaseEnrollmentCount(Long classRoomId) {
    int updated = classRoomRepository.decreaseEnrollmentCount(classRoomId);

    if (updated != 1) {
      throw new BadRequestException(CLASS_ROOM_ENROLLMENT_COUNT_INVALID);
    }
  }

  private Enrollment getEnrollmentOrThrow(Long enrollmentId) {
    return enrollmentRepository.findByIdWithClassRoomAndClassmateForUpdate(enrollmentId)
        .orElseThrow(() -> new NotFoundException(ENROLLMENT_NOT_FOUND));
  }

  private Classmate getClassmateOrThrow(Long classmateId) {
    return classmateRepository.findById(classmateId)
        .orElseThrow(() -> new NotFoundException(CLASSMATE_NOT_FOUND));
  }

  private void validateClassmateExists(Long classmateId) {
    if (!classmateRepository.existsById(classmateId)) {
      throw new NotFoundException(CLASSMATE_NOT_FOUND);
    }
  }

  private void validateEnrollmentOwner(Enrollment enrollment, Long classmateId) {
    if (!enrollment.isOwnedBy(classmateId)) {
      throw new ForbiddenException(ENROLLMENT_FORBIDDEN);
    }
  }


}
