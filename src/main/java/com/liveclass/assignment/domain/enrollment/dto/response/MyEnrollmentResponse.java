package com.liveclass.assignment.domain.enrollment.dto.response;


import java.time.LocalDateTime;

import static com.liveclass.assignment.domain.enrollment.entity.Enrollment.*;

public record MyEnrollmentResponse(
    Long enrollmentId,
    Long classRoomId,
    String classRoomTitle,
    Long price,
    EnrollmentStatus status,
    LocalDateTime paymentExpiredAt,
    LocalDateTime confirmedAt,
    LocalDateTime cancelledAt,
    LocalDateTime createdAt
) {
}
