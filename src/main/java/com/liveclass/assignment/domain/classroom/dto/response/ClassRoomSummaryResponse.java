package com.liveclass.assignment.domain.classroom.dto.response;

import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus;

import java.time.LocalDateTime;

public record ClassRoomSummaryResponse(
    Long classRoomId,
    Long creatorId,
    String creatorName,
    String title,
    Long price,
    Integer capacity,
    Integer enrollmentCount,
    ClassRoomStatus status,
    LocalDateTime startAt,
    LocalDateTime endAt
) {

  public static ClassRoomSummaryResponse from(ClassRoom classRoom) {
    return new ClassRoomSummaryResponse(
        classRoom.getId(),
        classRoom.getCreator().getId(),
        classRoom.getCreator().getName(),
        classRoom.getTitle(),
        classRoom.getPrice(),
        classRoom.getCapacity(),
        classRoom.getEnrollmentCount(),
        classRoom.getStatus(),
        classRoom.getStartAt(),
        classRoom.getEndAt()
    );
  }
}
