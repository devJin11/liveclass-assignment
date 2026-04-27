package com.liveclass.assignment.domain.enrollment.entity;

import com.liveclass.assignment.domain.classmate.entity.Classmate;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.global.exception.BadRequestException;
import com.liveclass.assignment.global.exception.CustomExceptionCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.liveclass.assignment.domain.enrollment.entity.Enrollment.CancelReason.PAYMENT_EXPIRED;
import static com.liveclass.assignment.domain.enrollment.entity.Enrollment.CancelReason.USER_CANCELLED;
import static com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnrollmentTest {

    @Test
    @DisplayName("수강 신청 생성 시 PENDING 상태이고 결제 만료 시각은 현재 시각 기준 10분 뒤다")
    void createEnrollment() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);

        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), mock(Classmate.class), now);

        assertThat(enrollment.getStatus()).isEqualTo(PENDING);
        assertThat(enrollment.getPaymentExpiredAt()).isEqualTo(now.plusMinutes(10));
        assertThat(enrollment.getConfirmedAt()).isNull();
        assertThat(enrollment.getCancelledAt()).isNull();
        assertThat(enrollment.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("결제 만료 전 PENDING 신청은 CONFIRMED 상태로 확정할 수 있다")
    void confirm_success() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), mock(Classmate.class), now);

        enrollment.confirm(now.plusMinutes(9));

        assertThat(enrollment.getStatus()).isEqualTo(CONFIRMED);
        assertThat(enrollment.getConfirmedAt()).isEqualTo(now.plusMinutes(9));
        assertThat(enrollment.getCancelledAt()).isNull();
        assertThat(enrollment.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("PENDING이 아닌 신청은 결제 확정할 수 없다")
    void confirm_fail_whenNotPending() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), mock(Classmate.class), now);
        enrollment.confirm(now.plusMinutes(1));

        assertBadRequestCode(() -> enrollment.confirm(now.plusMinutes(2)), CustomExceptionCode.ENROLLMENT_CONFIRM_ONLY_PENDING);
    }

    @Test
    @DisplayName("결제 가능 시간이 만료된 신청은 결제 확정할 수 없다")
    void confirm_fail_whenPaymentExpired() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), mock(Classmate.class), now);

        assertBadRequestCode(() -> enrollment.confirm(now.plusMinutes(10)), CustomExceptionCode.ENROLLMENT_PAYMENT_EXPIRED);
    }

    @Test
    @DisplayName("PENDING 신청은 사용자 취소할 수 있다")
    void cancelByUser_pending_success() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), mock(Classmate.class), now);

        enrollment.cancelByUser(now.plusMinutes(1));

        assertThat(enrollment.getStatus()).isEqualTo(CANCELLED);
        assertThat(enrollment.getCancelledAt()).isEqualTo(now.plusMinutes(1));
        assertThat(enrollment.getCancelReason()).isEqualTo(USER_CANCELLED);
    }

    @Test
    @DisplayName("CONFIRMED 신청은 확정 후 7일 이내 사용자 취소할 수 있다")
    void cancelByUser_confirmed_success() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        LocalDateTime confirmedAt = now.plusMinutes(1);

        Enrollment enrollment = Enrollment.createEnrollment(
            mock(ClassRoom.class),
            mock(Classmate.class),
            now
        );

        enrollment.confirm(confirmedAt);

        enrollment.cancelByUser(confirmedAt.plusDays(7));

        assertThat(enrollment.getStatus()).isEqualTo(CANCELLED);
        assertThat(enrollment.getCancelReason()).isEqualTo(USER_CANCELLED);
    }

    @Test
    @DisplayName("이미 취소된 신청은 다시 사용자 취소할 수 없다")
    void cancelByUser_fail_whenAlreadyCancelled() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), mock(Classmate.class), now);
        enrollment.cancelByUser(now.plusMinutes(1));

        assertBadRequestCode(() -> enrollment.cancelByUser(now.plusMinutes(2)), CustomExceptionCode.ENROLLMENT_ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("CONFIRMED 후 7일이 지나면 사용자 취소할 수 없다")
    void cancelByUser_fail_whenCancelPeriodExpired() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        LocalDateTime confirmedAt = now.plusMinutes(1);

        Enrollment enrollment = Enrollment.createEnrollment(
            mock(ClassRoom.class),
            mock(Classmate.class),
            now
        );

        enrollment.confirm(confirmedAt);

        assertBadRequestCode(
            () -> enrollment.cancelByUser(confirmedAt.plusDays(7).plusNanos(1)),
            CustomExceptionCode.ENROLLMENT_CANCEL_PERIOD_EXPIRED
        );
    }

    @Test
    @DisplayName("결제 만료 시간이 지난 PENDING 신청은 자동 취소할 수 있다")
    void cancelByPaymentExpired_success() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), mock(Classmate.class), now);

        enrollment.cancelByPaymentExpired(now.plusMinutes(10));

        assertThat(enrollment.getStatus()).isEqualTo(CANCELLED);
        assertThat(enrollment.getCancelReason()).isEqualTo(PAYMENT_EXPIRED);
    }

    @Test
    @DisplayName("결제 만료 시간이 지나지 않은 신청은 자동 취소할 수 없다")
    void cancelByPaymentExpired_fail_whenNotExpired() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), mock(Classmate.class), now);

        assertBadRequestCode(() -> enrollment.cancelByPaymentExpired(now.plusMinutes(9)), CustomExceptionCode.ENROLLMENT_PAYMENT_NOT_EXPIRED);
    }

    @Test
    @DisplayName("CANCELLED 상태의 신청은 재신청하면 PENDING 상태로 복구된다")
    void reapply_success() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 28, 12, 0);
        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), mock(Classmate.class), now);
        enrollment.cancelByUser(now.plusMinutes(1));

        enrollment.reapply(now.plusMinutes(2));

        assertThat(enrollment.getStatus()).isEqualTo(PENDING);
        assertThat(enrollment.getPaymentExpiredAt()).isEqualTo(now.plusMinutes(12));
        assertThat(enrollment.getCancelledAt()).isNull();
        assertThat(enrollment.getCancelReason()).isNull();
    }

    @Test
    @DisplayName("CANCELLED 상태가 아니면 재신청할 수 없다")
    void reapply_fail_whenNotCancelled() {
        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), mock(Classmate.class), LocalDateTime.now());

        assertBadRequestCode(() -> enrollment.reapply(LocalDateTime.now()), CustomExceptionCode.ENROLLMENT_NOT_CANCELLED);
    }

    @Test
    @DisplayName("요청 classmateId가 신청 소유자와 같으면 true를 반환한다")
    void isOwnedBy() {
        Classmate classmate = mock(Classmate.class);
        when(classmate.getId()).thenReturn(1L);
        Enrollment enrollment = Enrollment.createEnrollment(mock(ClassRoom.class), classmate, LocalDateTime.now());

        assertThat(enrollment.isOwnedBy(1L)).isTrue();
        assertThat(enrollment.isOwnedBy(2L)).isFalse();
    }

    private void assertBadRequestCode(ThrowingCallable callable, CustomExceptionCode code) {
        assertThatThrownBy(callable)
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo(code.getCode());
    }
}
