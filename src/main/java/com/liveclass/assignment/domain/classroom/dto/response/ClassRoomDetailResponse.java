package com.liveclass.assignment.domain.classroom.dto.response;

import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus;

import java.time.LocalDateTime;

public record ClassRoomDetailResponse(
    Long classRoomId,
    Long creatorId,
    String creatorName,
    String title,
    String description,
    Long price,
    Integer capacity,
    Integer enrollmentCount,
    ClassRoomStatus status,
    LocalDateTime startAt,
    LocalDateTime endAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

  public static ClassRoomDetailResponse from(ClassRoom classRoom) {
    return new ClassRoomDetailResponse(
        classRoom.getId(),
        classRoom.getCreator().getId(),
        classRoom.getCreator().getName(),
        classRoom.getTitle(),
        classRoom.getDescription(),
        classRoom.getPrice(),
        classRoom.getCapacity(),
        classRoom.getEnrollmentCount(),
        classRoom.getStatus(),
        classRoom.getStartAt(),
        classRoom.getEndAt(),
        classRoom.getCreatedAt(),
        classRoom.getUpdatedAt()
    );
  }

}