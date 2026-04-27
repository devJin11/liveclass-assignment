package com.liveclass.assignment.domain.classroom.service;

import com.liveclass.assignment.domain.classroom.dto.request.ClassRoomRequest;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomDetailResponse;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomEnrollmentResponse;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomSummaryResponse;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus;
import com.liveclass.assignment.domain.classroom.repository.ClassRoomRepository;
import com.liveclass.assignment.domain.creator.entity.Creator;
import com.liveclass.assignment.domain.creator.repository.CreatorRepository;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus;
import com.liveclass.assignment.global.common.Role;
import com.liveclass.assignment.global.exception.BadRequestException;
import com.liveclass.assignment.global.exception.ForbiddenException;
import com.liveclass.assignment.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus.*;
import static com.liveclass.assignment.global.common.Role.CLASSMATE;
import static com.liveclass.assignment.global.common.Role.CREATOR;
import static com.liveclass.assignment.global.exception.CustomExceptionCode.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassRoomService {

  private static final List<ClassRoomStatus> CLASSMATE_VIEWABLE_STATUSES =
      List.of(OPEN, CLOSED);

  private final ClassRoomRepository classRoomRepository;
  private final CreatorRepository creatorRepository;

  // 강의 등록
  @Transactional
  public Long createClassRoom(ClassRoomRequest request) {
    // Creator 조회, 없으면 예외
    Creator creator = getCreatorOrThrow(request.creatorId());

    ClassRoom classRoom = ClassRoom.builder()
        .creator(creator)
        .title(request.title())
        .description(request.description())
        .price(request.price())
        .capacity(request.capacity())
        .startAt(request.startAt())
        .endAt(request.endAt())
        .build();

    return classRoomRepository.save(classRoom).getId();
  }

  // 강의 수정
  @Transactional
  public void updateClassRoom(Long classRoomId, ClassRoomRequest request) {
    // ClassRoom + Creator fetch 조회, 없으면 예외
    ClassRoom classRoom = getClassRoomWithCreatorOrThrow(classRoomId);

    // 본인 강의 맞는지 검증
    validateClassRoomOwner(classRoom, request.creatorId());

    classRoom.update(
        request.title(),
        request.description(),
        request.price(),
        request.capacity(),
        request.startAt(),
        request.endAt()
    );
  }

  // 강의 Open
  @Transactional
  public void openClassRoom(Long classRoomId, Long creatorId) {
    // ClassRoom + Creator fetch 조회, 없으면 예외
    ClassRoom classRoom = getClassRoomWithCreatorOrThrow(classRoomId);

    // 본인 강의 맞는지 검증
    validateClassRoomOwner(classRoom, creatorId);

    classRoom.open();
  }

  // 강의 Close
  @Transactional
  public void closeClassRoom(Long classRoomId, Long creatorId) {
    // ClassRoom + Creator fetch 조회, 없으면 예외
    ClassRoom classRoom = getClassRoomWithCreatorOrThrow(classRoomId);

    // 본인 강의 맞는지 검증
    validateClassRoomOwner(classRoom, creatorId);

    classRoom.close();
  }

  // Creator -> 강의별 수강생 목록 조회
  public Page<ClassRoomEnrollmentResponse> getClassRoomEnrollments(
      Long classRoomId,
      Long creatorId,
      EnrollmentStatus status,
      Pageable pageable
  ) {
    validateClassRoomOwnerExists(classRoomId, creatorId);

    return classRoomRepository.findClassRoomEnrollments(
        classRoomId,
        status,
        pageable
    );
  }

  // Creator, ClassMate 공통 -> 강의 목록 조회
  public Page<ClassRoomSummaryResponse> getClassRooms(
      Role role,
      Long creatorId,
      ClassRoomStatus status,
      Pageable pageable
  ) {
    if (role == CREATOR) {
      return getClassRoomsForCreator(creatorId, status, pageable);
    }

    if (role == CLASSMATE) {
      return getClassRoomsForClassmate(status, pageable);
    }

    throw new BadRequestException(INVALID_ROLE);
  }

  // 강의 상세 조회 : creator는 본인의 강의만, classmate는 DRAFT 상태의 강의 조회 불가
  public ClassRoomDetailResponse getClassRoomDetail(
      Long classRoomId,
      Role role,
      Long creatorId
  ) {
    // 일단 ClassRoomDetailResponse 로딩
    ClassRoomDetailResponse response = classRoomRepository.findClassRoomDetail(classRoomId)
        .orElseThrow(() -> new NotFoundException(CLASS_ROOM_NOT_FOUND));

    if (role == CREATOR) {
      validateClassRoomOwner(response, creatorId);
      return response;
    }

    if (role == CLASSMATE) {
      validateClassmateCanView(response);
      return response;
    }

    throw new BadRequestException(INVALID_ROLE);
  }

  private Page<ClassRoomSummaryResponse> getClassRoomsForCreator(
      Long creatorId,
      ClassRoomStatus status,
      Pageable pageable
  ) {
    /*
     * creatorId가 실제 존재하는지 검증한다.
     * 존재하지 않는 creatorId로 조회하면 단순히 빈 페이지가 나올 수도 있지만,
     * 본 프로젝트에서는 요청 식별자의 유효성을 명확히 검증한다.
     */
    if (!creatorRepository.existsById(creatorId)) {
      throw new NotFoundException(CREATOR_NOT_FOUND);
    }

    return classRoomRepository.findClassRoomSummariesByCreator(
        creatorId,
        status,
        pageable
    );
  }

  private Page<ClassRoomSummaryResponse> getClassRoomsForClassmate(
      ClassRoomStatus status,
      Pageable pageable
  ) {
    validateClassmateListStatus(status);

    List<ClassRoomStatus> statuses =
        status == null
            ? CLASSMATE_VIEWABLE_STATUSES
            : List.of(status);

    return classRoomRepository.findClassRoomSummariesForClassmate(
        statuses,
        pageable
    );
  }

  private Creator getCreatorOrThrow(Long creatorId) {
    return creatorRepository.findById(creatorId)
        .orElseThrow(() -> new NotFoundException(CREATOR_NOT_FOUND));
  }

  private ClassRoom getClassRoomWithCreatorOrThrow(Long classRoomId) {
    return classRoomRepository.findByIdWithCreator(classRoomId)
        .orElseThrow(() -> new NotFoundException(CLASS_ROOM_NOT_FOUND));
  }

  private void validateClassRoomOwner(ClassRoom classRoom, Long creatorId) {
    if (!classRoom.isOwnedBy(creatorId)) {
      throw new ForbiddenException(CLASS_ROOM_FORBIDDEN);
    }
  }

  private void validateClassRoomOwner(
      ClassRoomDetailResponse response,
      Long creatorId
  ) {
    if (!response.creatorId().equals(creatorId)) {
      throw new ForbiddenException(CLASS_ROOM_FORBIDDEN);
    }
  }

  private void validateClassmateCanView(ClassRoomDetailResponse response) {
    if (response.status() == DRAFT) {
      throw new BadRequestException(CLASS_ROOM_DRAFT_NOT_VIEWABLE);
    }
  }

  private void validateClassmateListStatus(ClassRoomStatus status) {
    if (status == DRAFT) {
      throw new BadRequestException(CLASS_ROOM_DRAFT_NOT_VIEWABLE);
    }
  }

  private void validateClassRoomOwnerExists(Long classRoomId, Long creatorId) {
    if (!classRoomRepository.existsByIdAndCreator_Id(classRoomId, creatorId)) {
      throw new ForbiddenException(CLASS_ROOM_FORBIDDEN);
    }
  }



}