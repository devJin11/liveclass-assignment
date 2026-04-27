package com.liveclass.assignment.domain.classroom.service;

import com.liveclass.assignment.domain.classroom.dto.request.ClassRoomRequest;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomDetailResponse;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.global.common.Role;
import com.liveclass.assignment.global.exception.BadRequestException;
import com.liveclass.assignment.global.exception.CustomExceptionCode;
import com.liveclass.assignment.global.exception.ForbiddenException;
import com.liveclass.assignment.global.exception.NotFoundException;
import com.liveclass.assignment.support.AbstractIntegrationTest;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClassRoomServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ClassRoomService classRoomService;

    @Test
    @DisplayName("강의를 생성하면 DRAFT 상태로 저장된다")
    void createClassRoom_success() {
        Long classRoomId = classRoomService.createClassRoom(request(1L, "신규 강의"));

        ClassRoom classRoom = classRoomRepository.findById(classRoomId).orElseThrow();
        assertThat(classRoom.getStatus()).isEqualTo(DRAFT);
        assertThat(classRoom.getCreator().getId()).isEqualTo(1L);
        assertThat(classRoom.getTitle()).isEqualTo("신규 강의");
        assertThat(classRoom.getEnrollmentCount()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 크리에이터로 강의를 생성하면 404 예외가 발생한다")
    void createClassRoom_fail_whenCreatorNotFound() {
        assertExceptionCode(
                () -> classRoomService.createClassRoom(request(999L, "신규 강의")),
                NotFoundException.class,
                CustomExceptionCode.CREATOR_NOT_FOUND
        );
    }

    @Test
    @DisplayName("강의 소유자는 DRAFT 상태의 강의를 수정할 수 있다")
    void updateClassRoom_success() {
        ClassRoom classRoom = createClassRoom(1L, 10, DRAFT);

        classRoomService.updateClassRoom(classRoom.getId(), request(1L, "수정 강의"));

        ClassRoom updated = classRoomRepository.findById(classRoom.getId()).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("수정 강의");
    }

    @Test
    @DisplayName("강의 소유자가 아니면 강의를 수정할 수 없다")
    void updateClassRoom_fail_whenNotOwner() {
        ClassRoom classRoom = createClassRoom(1L, 10, DRAFT);

        assertExceptionCode(
                () -> classRoomService.updateClassRoom(classRoom.getId(), request(2L, "수정 강의")),
                ForbiddenException.class,
                CustomExceptionCode.CLASS_ROOM_FORBIDDEN
        );
    }

    @Test
    @DisplayName("강의 소유자는 DRAFT 강의를 OPEN 상태로 변경할 수 있다")
    void openClassRoom_success() {
        ClassRoom classRoom = createClassRoom(1L, 10, DRAFT);

        classRoomService.openClassRoom(classRoom.getId(), 1L);

        assertThat(classRoomRepository.findById(classRoom.getId()).orElseThrow().getStatus()).isEqualTo(OPEN);
    }

    @Test
    @DisplayName("OPEN 강의는 CLOSED 상태로 변경할 수 있다")
    void closeClassRoom_success() {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);

        classRoomService.closeClassRoom(classRoom.getId(), 1L);

        assertThat(classRoomRepository.findById(classRoom.getId()).orElseThrow().getStatus()).isEqualTo(CLOSED);
    }

    @Test
    @DisplayName("Creator는 본인 강의의 DRAFT 상세를 조회할 수 있다")
    void getClassRoomDetail_creator_success() {
        ClassRoom classRoom = createClassRoom(1L, 10, DRAFT);

        ClassRoomDetailResponse response = classRoomService.getClassRoomDetail(classRoom.getId(), Role.CREATOR, 1L);

        assertThat(response.classRoomId()).isEqualTo(classRoom.getId());
        assertThat(response.creatorId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(DRAFT);
    }

    @Test
    @DisplayName("Classmate는 DRAFT 강의 상세를 조회할 수 없다")
    void getClassRoomDetail_classmate_fail_whenDraft() {
        ClassRoom classRoom = createClassRoom(1L, 10, DRAFT);

        assertExceptionCode(
                () -> classRoomService.getClassRoomDetail(classRoom.getId(), Role.CLASSMATE, null),
                BadRequestException.class,
                CustomExceptionCode.CLASS_ROOM_DRAFT_NOT_VIEWABLE
        );
    }

    @Test
    @DisplayName("Classmate 강의 목록 조회에서 DRAFT 상태 필터는 허용되지 않는다")
    void getClassRooms_classmate_fail_whenDraftFilter() {
        assertExceptionCode(
                () -> classRoomService.getClassRooms(Role.CLASSMATE, null, DRAFT, PageRequest.of(0, 10)),
                BadRequestException.class,
                CustomExceptionCode.CLASS_ROOM_DRAFT_NOT_VIEWABLE
        );
    }

    @Test
    @DisplayName("Creator 강의 목록 조회에서 creatorId가 존재하지 않으면 404 예외가 발생한다")
    void getClassRooms_creator_fail_whenCreatorNotFound() {
        assertExceptionCode(
                () -> classRoomService.getClassRooms(Role.CREATOR, 999L, null, PageRequest.of(0, 10)),
                NotFoundException.class,
                CustomExceptionCode.CREATOR_NOT_FOUND
        );
    }

    private ClassRoomRequest request(Long creatorId, String title) {
        return new ClassRoomRequest(
                creatorId,
                title,
                "강의 설명",
                10_000L,
                10,
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(10)
        );
    }

    private <T extends Throwable> void assertExceptionCode(
            ThrowingCallable callable,
            Class<T> exceptionType,
            CustomExceptionCode code
    ) {
        assertThatThrownBy(callable)
                .isInstanceOf(exceptionType)
                .extracting("code")
                .isEqualTo(code.getCode());
    }
}
