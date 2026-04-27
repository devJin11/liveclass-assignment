package com.liveclass.assignment.domain.classroom.dto.response;

import com.liveclass.assignment.domain.enrollment.entity.Enrollment;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus;

import java.time.LocalDateTime;

public record ClassRoomEnrollmentResponse(
    Long enrollmentId,
    Long classmateId,
    String classmateName,
    EnrollmentStatus status,
    LocalDateTime paymentExpiredAt,
    LocalDateTime confirmedAt,
    LocalDateTime cancelledAt,
    LocalDateTime createdAt
) {

  public static ClassRoomEnrollmentResponse from(Enrollment enrollment) {
    return new ClassRoomEnrollmentResponse(
        enrollment.getId(),
        enrollment.getClassmate().getId(),
        enrollment.getClassmate().getName(),
        enrollment.getStatus(),
        enrollment.getPaymentExpiredAt(),
        enrollment.getConfirmedAt(),
        enrollment.getCancelledAt(),
        enrollment.getCreatedAt()
    );
  }
}