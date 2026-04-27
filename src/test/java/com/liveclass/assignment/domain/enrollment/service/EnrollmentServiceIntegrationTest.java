package com.liveclass.assignment.domain.enrollment.service;

import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.domain.enrollment.dto.response.EnrollmentResponse;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment;
import com.liveclass.assignment.domain.enrollment.repository.EnrollmentRepository;
import com.liveclass.assignment.global.exception.BadRequestException;
import com.liveclass.assignment.global.exception.CustomExceptionCode;
import com.liveclass.assignment.global.exception.ForbiddenException;
import com.liveclass.assignment.global.exception.NotFoundException;
import com.liveclass.assignment.support.AbstractIntegrationTest;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus.*;
import static com.liveclass.assignment.domain.enrollment.entity.Enrollment.CancelReason.PAYMENT_EXPIRED;
import static com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnrollmentServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("OPEN 강의에 수강 신청하면 PENDING 신청이 생성되고 강의 신청 수가 1 증가한다")
    void enroll_success() {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);

        EnrollmentResponse response = enrollmentService.enroll(classRoom.getId(), 1L);

        assertThat(response.enrollmentId()).isNotNull();
        assertThat(response.status()).isEqualTo(PENDING);
        assertThat(response.classRoomId()).isEqualTo(classRoom.getId());
        assertThat(response.classmateId()).isEqualTo(1L);
        assertThat(enrollmentCountOf(classRoom.getId())).isEqualTo(1);
        assertThat(enrollmentRowCountOf(classRoom.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 클래스메이트가 신청하면 404 예외가 발생한다")
    void enroll_fail_whenClassmateNotFound() {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);

        assertExceptionCode(
                () -> enrollmentService.enroll(classRoom.getId(), 999L),
                NotFoundException.class,
                CustomExceptionCode.CLASSMATE_NOT_FOUND
        );
    }

    @Test
    @DisplayName("존재하지 않는 강의에 신청하면 404 예외가 발생한다")
    void enroll_fail_whenClassRoomNotFound() {
        assertExceptionCode(
                () -> enrollmentService.enroll(999L, 1L),
                NotFoundException.class,
                CustomExceptionCode.CLASS_ROOM_NOT_FOUND
        );
    }

    @Test
    @DisplayName("OPEN 상태가 아닌 강의에는 신청할 수 없다")
    void enroll_fail_whenClassRoomNotOpen() {
        ClassRoom draftClassRoom = createClassRoom(1L, 10, DRAFT);

        assertExceptionCode(
                () -> enrollmentService.enroll(draftClassRoom.getId(), 1L),
                BadRequestException.class,
                CustomExceptionCode.CLASS_ROOM_NOT_OPEN
        );
    }

    @Test
    @DisplayName("정원이 가득 찬 강의에는 신청할 수 없다")
    void enroll_fail_whenCapacityExceeded() {
        ClassRoom classRoom = createClassRoom(1L, 1, OPEN);
        enrollmentService.enroll(classRoom.getId(), 1L);

        assertExceptionCode(
                () -> enrollmentService.enroll(classRoom.getId(), 2L),
                BadRequestException.class,
                CustomExceptionCode.CLASS_ROOM_CAPACITY_EXCEEDED
        );

        assertThat(enrollmentCountOf(classRoom.getId())).isEqualTo(1);
        assertThat(enrollmentRowCountOf(classRoom.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 좌석을 점유 중인 신청이 있으면 중복 신청할 수 없다")
    void enroll_fail_whenDuplicatedPendingEnrollment() {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);
        enrollmentService.enroll(classRoom.getId(), 1L);

        assertExceptionCode(
                () -> enrollmentService.enroll(classRoom.getId(), 1L),
                BadRequestException.class,
                CustomExceptionCode.DUPLICATED_ENROLLMENT
        );

        assertThat(enrollmentCountOf(classRoom.getId())).isEqualTo(1);
        assertThat(enrollmentRowCountOf(classRoom.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("CANCELLED 신청이 있으면 같은 신청 row를 재사용해 재신청 처리한다")
    void enroll_reapply_success() {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);
        EnrollmentResponse first = enrollmentService.enroll(classRoom.getId(), 1L);
        enrollmentService.cancel(first.enrollmentId(), 1L);

        EnrollmentResponse reapplied = enrollmentService.enroll(classRoom.getId(), 1L);

        assertThat(reapplied.enrollmentId()).isEqualTo(first.enrollmentId());
        assertThat(reapplied.status()).isEqualTo(PENDING);
        assertThat(enrollmentCountOf(classRoom.getId())).isEqualTo(1);
        assertThat(enrollmentRowCountOf(classRoom.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("본인 신청이고 결제 가능 시간이 지나지 않았으면 결제 확정할 수 있다")
    void confirm_success() {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);
        EnrollmentResponse enrolled = enrollmentService.enroll(classRoom.getId(), 1L);

        EnrollmentResponse confirmed = enrollmentService.confirm(enrolled.enrollmentId(), 1L);

        assertThat(confirmed.status()).isEqualTo(CONFIRMED);
        assertThat(confirmed.confirmedAt()).isNotNull();
        assertThat(enrollmentCountOf(classRoom.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 클래스메이트의 신청은 결제 확정할 수 없다")
    void confirm_fail_whenNotOwner() {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);
        EnrollmentResponse enrolled = enrollmentService.enroll(classRoom.getId(), 1L);

        assertExceptionCode(
                () -> enrollmentService.confirm(enrolled.enrollmentId(), 2L),
                ForbiddenException.class,
                CustomExceptionCode.ENROLLMENT_FORBIDDEN
        );
    }

    @Test
    @DisplayName("PENDING 또는 CONFIRMED 신청을 취소하면 CANCELLED 상태가 되고 강의 신청 수가 1 감소한다")
    void cancel_success() {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);
        EnrollmentResponse enrolled = enrollmentService.enroll(classRoom.getId(), 1L);
        enrollmentService.confirm(enrolled.enrollmentId(), 1L);

        EnrollmentResponse cancelled = enrollmentService.cancel(enrolled.enrollmentId(), 1L);

        assertThat(cancelled.status()).isEqualTo(CANCELLED);
        assertThat(cancelled.cancelledAt()).isNotNull();
        assertThat(enrollmentCountOf(classRoom.getId())).isZero();
    }

    @Test
    @DisplayName("내 수강 신청 목록 조회 시 클래스메이트가 없으면 404 예외가 발생한다")
    void getMyEnrollments_fail_whenClassmateNotFound() {
        assertExceptionCode(
                () -> enrollmentService.getMyEnrollments(999L, org.springframework.data.domain.PageRequest.of(0, 10)),
                NotFoundException.class,
                CustomExceptionCode.CLASSMATE_NOT_FOUND
        );
    }

    @Test
    @DisplayName("수강 신청 상세 조회 시 소유자가 아니면 403 예외가 발생한다")
    void getEnrollmentDetail_fail_whenNotOwner() {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);
        EnrollmentResponse enrolled = enrollmentService.enroll(classRoom.getId(), 1L);

        assertExceptionCode(
                () -> enrollmentService.getEnrollmentDetail(enrolled.enrollmentId(), 2L),
                ForbiddenException.class,
                CustomExceptionCode.ENROLLMENT_FORBIDDEN
        );
    }

    @Test
    @DisplayName("결제 만료된 PENDING 신청은 스케줄러용 서비스에서 자동 취소되고 강의 신청 수가 감소한다")
    void cancelExpiredPendingEnrollments_success() {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);
        EnrollmentResponse enrolled = enrollmentService.enroll(classRoom.getId(), 1L);
        jdbcTemplate.update(
                "UPDATE enrollment SET payment_expired_at = ? WHERE enrollment_id = ?",
                LocalDateTime.now().minusMinutes(1),
                enrolled.enrollmentId()
        );

        int cancelledCount = enrollmentService.cancelExpiredPendingEnrollments();

        Enrollment enrollment = enrollmentRepository.findById(enrolled.enrollmentId()).orElseThrow();
        assertThat(cancelledCount).isEqualTo(1);
        assertThat(enrollment.getStatus()).isEqualTo(CANCELLED);
        assertThat(enrollment.getCancelReason()).isEqualTo(PAYMENT_EXPIRED);
        assertThat(enrollmentCountOf(classRoom.getId())).isZero();
    }

    private <T extends Throwable> void assertExceptionCode(
            ThrowingCallable callable,
            Class<T> exceptionType,
            CustomExceptionCode code
    ) {
        assertThatThrownBy(callable)
                .isInstanceOf(exceptionType)
                .extracting("code")
                .isEqualTo(code.getCode());
    }
}
