package com.liveclass.assignment.domain.enrollment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus;

import java.time.LocalDateTime;

public record EnrollmentResponse(
    Long enrollmentId,
    Long classRoomId,
    Long classmateId,
    EnrollmentStatus status,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime paymentExpiredAt,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime confirmedAt,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime cancelledAt,

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt
) {

  public static EnrollmentResponse from(Enrollment enrollment) {
    return new EnrollmentResponse(
        enrollment.getId(),
        enrollment.getClassRoom().getId(),
        enrollment.getClassmate().getId(),
        enrollment.getStatus(),
        enrollment.getPaymentExpiredAt(),
        enrollment.getConfirmedAt(),
        enrollment.getCancelledAt(),
        enrollment.getCreatedAt()
    );
  }
}
