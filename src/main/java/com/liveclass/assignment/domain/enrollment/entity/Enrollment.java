package com.liveclass.assignment.domain.enrollment.entity;


import com.liveclass.assignment.domain.classmate.entity.Classmate;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.global.common.BaseEntity;
import com.liveclass.assignment.global.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.liveclass.assignment.global.exception.CustomExceptionCode.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment extends BaseEntity {

  private static final long PAYMENT_EXPIRE_MINUTES = 10; // 결제 대기 상태는 10분
  private static final long CANCEL_AVAILABLE_DAYS = 7; // 7일 이내로 취소 가능

  public enum EnrollmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED
  }

  public enum CancelReason {
    USER_CANCELLED,
    PAYMENT_EXPIRED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "enrollment_id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "class_room_id", nullable = false)
  private ClassRoom classRoom;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "classmate_id", nullable = false)
  private Classmate classmate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private EnrollmentStatus status;

  @Column(name = "payment_expired_at", nullable = false)
  private LocalDateTime paymentExpiredAt;

  @Column(name = "confirmed_at", nullable = true)
  private LocalDateTime confirmedAt;

  @Column(name = "cancelled_at", nullable = true)
  private LocalDateTime cancelledAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "cancel_reason", nullable = true, length = 30)
  private CancelReason cancelReason;

  /**
   *  정적 팩토리 메서드로 생성
   *  -> 결제 만료 정책은 필수 도메인 규칙이기 때문.
   * */
  public static Enrollment createEnrollment(
      ClassRoom classRoom,
      Classmate classmate,
      LocalDateTime now
  ) {
    Enrollment enrollment = new Enrollment();
    enrollment.classRoom = classRoom;
    enrollment.classmate = classmate;
    enrollment.status = EnrollmentStatus.PENDING;
    enrollment.paymentExpiredAt = now.plusMinutes(PAYMENT_EXPIRE_MINUTES);
    return enrollment;
  }

  // 결제 확정 상태로 전이
  public void confirm(LocalDateTime now) {
    if (this.status != EnrollmentStatus.PENDING) {
      throw new BadRequestException(ENROLLMENT_CONFIRM_ONLY_PENDING);
    }

    if (!now.isBefore(this.paymentExpiredAt)) {
      throw new BadRequestException(ENROLLMENT_PAYMENT_EXPIRED);
    }

    this.status = EnrollmentStatus.CONFIRMED;
    this.confirmedAt = now;
    this.cancelledAt = null;
    this.cancelReason = null;
  }

  // 사용자가 직접 취소
  public void cancelByUser(LocalDateTime now) {
    if (this.status == EnrollmentStatus.CANCELLED) {
      throw new BadRequestException(ENROLLMENT_ALREADY_CANCELLED);
    }

    if (this.status == EnrollmentStatus.CONFIRMED
        && now.isAfter(this.confirmedAt.plusDays(CANCEL_AVAILABLE_DAYS))) {
      throw new BadRequestException(ENROLLMENT_CANCEL_PERIOD_EXPIRED);
    }

    this.status = EnrollmentStatus.CANCELLED;
    this.cancelledAt = now;
    this.cancelReason = CancelReason.USER_CANCELLED;
  }

  // 10분 지나서 자동 수강 취소(by 스케줄러)
  public void cancelByPaymentExpired(LocalDateTime now) {
    if (this.status != EnrollmentStatus.PENDING) {
      throw new BadRequestException(ENROLLMENT_AUTO_CANCEL_ONLY_PENDING);
    }

    if (now.isBefore(this.paymentExpiredAt)) {
      throw new BadRequestException(ENROLLMENT_PAYMENT_NOT_EXPIRED);
    }

    this.status = EnrollmentStatus.CANCELLED;
    this.cancelledAt = now;
    this.cancelReason = CancelReason.PAYMENT_EXPIRED;
  }

  // 재 수강 신청
  public void reapply(LocalDateTime now) {
    if (this.status != EnrollmentStatus.CANCELLED) {
      throw new BadRequestException(ENROLLMENT_NOT_CANCELLED);
    }

    this.status = EnrollmentStatus.PENDING;
    this.paymentExpiredAt = now.plusMinutes(PAYMENT_EXPIRE_MINUTES);
    this.confirmedAt = null;
    this.cancelledAt = null;
    this.cancelReason = null;
  }

  // 요청 classmateId가 본인인지 검증
  public boolean isOwnedBy(Long classmateId) {
    return this.classmate.getId().equals(classmateId);
  }

  // 좌석 점유 상태인지
  public boolean isSeatOccupied() {
    return this.status == EnrollmentStatus.PENDING
        || this.status == EnrollmentStatus.CONFIRMED;
  }

  public Long getClassRoomId() {
    return this.classRoom.getId();
  }


}
