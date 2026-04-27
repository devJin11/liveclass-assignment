package com.liveclass.assignment.domain.enrollment.dto.response;

import com.liveclass.assignment.domain.enrollment.entity.Enrollment.CancelReason;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus;

import java.time.LocalDateTime;

public record EnrollmentDetailResponse(
    Long enrollmentId,
    Long classRoomId,
    String classRoomTitle,
    Long price,
    Long classmateId,
    String classmateName,
    EnrollmentStatus status,
    LocalDateTime paymentExpiredAt,
    LocalDateTime confirmedAt,
    LocalDateTime cancelledAt,
    CancelReason cancelReason,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}