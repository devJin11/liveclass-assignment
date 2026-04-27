package com.liveclass.assignment.domain.classroom.entity;


import com.liveclass.assignment.domain.creator.entity.Creator;
import com.liveclass.assignment.global.common.BaseEntity;
import com.liveclass.assignment.global.exception.BadRequestException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static com.liveclass.assignment.global.exception.CustomExceptionCode.*;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassRoom extends BaseEntity {

  public enum ClassRoomStatus {
    DRAFT,
    OPEN,
    CLOSED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "class_room_id", nullable = false)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "creator_id", nullable = false)
  private Creator creator;

  @Column(name = "title", nullable = false, length = 100)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "price", nullable = false)
  private Long price;

  @Column(name = "capacity", nullable = false)
  private int capacity;

  @Column(name = "enrollment_count", nullable = false)
  private int enrollmentCount;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ClassRoomStatus status;

  @Column(name = "start_at", nullable = false)
  private LocalDateTime startAt;

  @Column(name = "end_at", nullable = false)
  private LocalDateTime endAt;

  @Builder
  private ClassRoom(
      Creator creator,
      String title,
      String description,
      Long price,
      int capacity,
      LocalDateTime startAt,
      LocalDateTime endAt
  ) {
    this.creator = creator;
    this.title = title;
    this.description = description;
    this.price = price;
    this.capacity = capacity;
    this.enrollmentCount = 0;
    this.status = ClassRoomStatus.DRAFT;
    this.startAt = startAt;
    this.endAt = endAt;
  }

  // 강의 open
  public void open() {
    if (this.status != ClassRoomStatus.DRAFT) {
      throw new BadRequestException(CLASS_ROOM_OPEN_ONLY_DRAFT);
    }

    this.status = ClassRoomStatus.OPEN;
  }

  // 강의 close
  public void close() {
    if (this.status != ClassRoomStatus.OPEN) {
      throw new BadRequestException(CLASS_ROOM_CLOSE_ONLY_OPEN);
    }

    this.status = ClassRoomStatus.CLOSED;
  }

  // 강의 수정 메서드
  public void update(
      String title,
      String description,
      Long price,
      int capacity,
      LocalDateTime startAt,
      LocalDateTime endAt
  ) {
    if (this.status != ClassRoomStatus.DRAFT) {
      throw new BadRequestException(CLASS_ROOM_UPDATE_ONLY_DRAFT);
    }

    if (!startAt.isBefore(endAt)) {
      throw new BadRequestException(CLASS_ROOM_INVALID_PERIOD);
    }

    this.title = title;
    this.description = description;
    this.price = price;
    this.capacity = capacity;
    this.startAt = startAt;
    this.endAt = endAt;
  }

  // 크리에이터 본인 검증
  public boolean isOwnedBy(Long creatorId) {
    return this.creator.getId().equals(creatorId);
  }

  // 강의 open 상태인지
  public boolean isOpen() {
    return this.status == ClassRoomStatus.OPEN;
  }



}
