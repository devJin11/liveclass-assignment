package com.liveclass.assignment.domain.classroom.entity;

import com.liveclass.assignment.domain.creator.entity.Creator;
import com.liveclass.assignment.global.exception.BadRequestException;
import com.liveclass.assignment.global.exception.CustomExceptionCode;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClassRoomTest {

    private static final LocalDateTime BASE_TIME =
        LocalDateTime.of(2026, 5, 1, 10, 0);

    @Test
    @DisplayName("강의 생성 시 최초 상태는 DRAFT이고 신청 인원은 0명이다")
    void create() {
        ClassRoom classRoom = classRoom();

        assertThat(classRoom.getStatus()).isEqualTo(DRAFT);
        assertThat(classRoom.getEnrollmentCount()).isZero();
    }

    @Test
    @DisplayName("DRAFT 상태의 강의는 OPEN 상태로 변경할 수 있다")
    void open_success() {
        ClassRoom classRoom = classRoom();

        classRoom.open();

        assertThat(classRoom.getStatus()).isEqualTo(OPEN);
    }

    @Test
    @DisplayName("DRAFT가 아닌 강의는 모집 시작할 수 없다")
    void open_fail_whenNotDraft() {
        ClassRoom classRoom = classRoom();
        classRoom.open();

        assertBadRequestCode(classRoom::open, CustomExceptionCode.CLASS_ROOM_OPEN_ONLY_DRAFT);
    }

    @Test
    @DisplayName("OPEN 상태의 강의는 CLOSED 상태로 변경할 수 있다")
    void close_success() {
        ClassRoom classRoom = classRoom();
        classRoom.open();

        classRoom.close();

        assertThat(classRoom.getStatus()).isEqualTo(CLOSED);
    }

    @Test
    @DisplayName("OPEN이 아닌 강의는 모집 마감할 수 없다")
    void close_fail_whenNotOpen() {
        ClassRoom classRoom = classRoom();

        assertBadRequestCode(classRoom::close, CustomExceptionCode.CLASS_ROOM_CLOSE_ONLY_OPEN);
    }

    @Test
    @DisplayName("DRAFT 상태의 강의는 수정할 수 있다")
    void update_success() {
        ClassRoom classRoom = classRoom();
        LocalDateTime startAt = LocalDateTime.now().plusDays(3);
        LocalDateTime endAt = LocalDateTime.now().plusDays(10);

        classRoom.update("수정 제목", "수정 설명", 20_000L, 20, startAt, endAt);

        assertThat(classRoom.getTitle()).isEqualTo("수정 제목");
        assertThat(classRoom.getDescription()).isEqualTo("수정 설명");
        assertThat(classRoom.getPrice()).isEqualTo(20_000L);
        assertThat(classRoom.getCapacity()).isEqualTo(20);
        assertThat(classRoom.getStartAt()).isEqualTo(startAt);
        assertThat(classRoom.getEndAt()).isEqualTo(endAt);
    }

    @Test
    @DisplayName("DRAFT가 아닌 강의는 수정할 수 없다")
    void update_fail_whenNotDraft() {
        ClassRoom classRoom = classRoom();
        classRoom.open();

        assertBadRequestCode(
                () -> classRoom.update("수정", "수정", 10_000L, 20, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2)),
                CustomExceptionCode.CLASS_ROOM_UPDATE_ONLY_DRAFT
        );
    }

    @Test
    @DisplayName("강의 시작 시각이 종료 시각보다 이전이 아니면 수정할 수 없다")
    void update_fail_whenInvalidPeriod() {
        ClassRoom classRoom = classRoom();
        LocalDateTime sameTime = LocalDateTime.now().plusDays(1);

        assertBadRequestCode(
                () -> classRoom.update("수정", "수정", 10_000L, 20, sameTime, sameTime),
                CustomExceptionCode.CLASS_ROOM_INVALID_PERIOD
        );
    }

    @Test
    @DisplayName("요청 creatorId가 강의 소유자와 같으면 true를 반환한다")
    void isOwnedBy() {
        Creator creator = mock(Creator.class);
        when(creator.getId()).thenReturn(1L);

        ClassRoom classRoom = ClassRoom.builder()
                .creator(creator)
                .title("강의")
                .description("설명")
                .price(10_000L)
                .capacity(10)
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(2))
                .build();

        assertThat(classRoom.isOwnedBy(1L)).isTrue();
        assertThat(classRoom.isOwnedBy(2L)).isFalse();
    }

    private ClassRoom classRoom() {
        return ClassRoom.builder()
            .creator(mock(Creator.class))
            .title("테스트 강의")
            .description("테스트 설명")
            .price(10_000L)
            .capacity(10)
            .startAt(BASE_TIME.plusDays(1))
            .endAt(BASE_TIME.plusDays(2))
            .build();
    }

    private void assertBadRequestCode(ThrowingCallable callable, CustomExceptionCode code) {
        assertThatThrownBy(callable)
                .isInstanceOf(BadRequestException.class)
                .extracting("code")
                .isEqualTo(code.getCode());
    }
}
