package com.liveclass.assignment.domain.classroom.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomDetailResponse;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomSummaryResponse;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus;
import com.liveclass.assignment.domain.classroom.service.ClassRoomService;
import com.liveclass.assignment.global.common.Role;
import com.liveclass.assignment.global.exception.BadRequestException;
import com.liveclass.assignment.global.exception.CustomExceptionCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClassRoomController.class)
class ClassRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ClassRoomService classRoomService;

    @Test
    @DisplayName("강의 등록 API 성공 시 201과 Location 헤더를 반환한다")
    void createClassRoom_success() throws Exception {
        given(classRoomService.createClassRoom(any())).willReturn(100L);

        mockMvc.perform(post("/api/class-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/class-rooms/100"));
    }

    @Test
    @DisplayName("강의 등록 API에서 필수값이 누락되면 400 validation 응답을 반환한다")
    void createClassRoom_validationFail() throws Exception {
        mockMvc.perform(post("/api/class-rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CustomExceptionCode.INVALID_REQUEST.getCode()))
                .andExpect(jsonPath("$.errors.creatorId").value("크리에이터 ID는 필수입니다."))
                .andExpect(jsonPath("$.errors.title").value("강의 제목은 필수입니다."));
    }

    @Test
    @DisplayName("강의 모집 시작 API 성공 시 204를 반환한다")
    void openClassRoom_success() throws Exception {
        mockMvc.perform(patch("/api/class-rooms/{classRoomId}/open", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("creatorId", 1L))))
                .andExpect(status().isNoContent());

        verify(classRoomService).openClassRoom(100L, 1L);
    }

    @Test
    @DisplayName("강의 상세 조회 API 성공 시 상세 응답을 반환한다")
    void getClassRoomDetail_success() throws Exception {
        given(classRoomService.getClassRoomDetail(100L, Role.CLASSMATE, null))
                .willReturn(new ClassRoomDetailResponse(
                        100L,
                        1L,
                        "creator_1",
                        "강의",
                        "설명",
                        10_000L,
                        10,
                        0,
                        ClassRoomStatus.OPEN,
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(10),
                        LocalDateTime.now(),
                        LocalDateTime.now()
                ));

        mockMvc.perform(get("/api/class-rooms/{classRoomId}", 100L)
                        .param("role", "CLASSMATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.classRoomId").value(100L))
                .andExpect(jsonPath("$.creatorId").value(1L))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("서비스에서 BadRequestException이 발생하면 400 예외 응답을 반환한다")
    void getClassRoomDetail_badRequest() throws Exception {
        given(classRoomService.getClassRoomDetail(100L, Role.CLASSMATE, null))
                .willThrow(new BadRequestException(CustomExceptionCode.CLASS_ROOM_DRAFT_NOT_VIEWABLE));

        mockMvc.perform(get("/api/class-rooms/{classRoomId}", 100L)
                        .param("role", "CLASSMATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CustomExceptionCode.CLASS_ROOM_DRAFT_NOT_VIEWABLE.getCode()))
                .andExpect(jsonPath("$.message").value(CustomExceptionCode.CLASS_ROOM_DRAFT_NOT_VIEWABLE.getMessage()));
    }

    @Test
    @DisplayName("강의 목록 조회 API 성공 시 필요한 페이징 필드만 반환한다")
    void getClassRooms_success() throws Exception {
        ClassRoomSummaryResponse response = new ClassRoomSummaryResponse(
            1L,
            1L,
            "creator_1",
            "Spring Boot 입문",
            50_000L,
            30,
            0,
            ClassRoomStatus.OPEN,
            LocalDateTime.of(2026, 5, 1, 0, 0),
            LocalDateTime.of(2026, 6, 1, 0, 0)
        );

        Page<ClassRoomSummaryResponse> page = new PageImpl<>(
            List.of(response),
            PageRequest.of(0, 10),
            1
        );

        given(classRoomService.getClassRooms(
            eq(Role.CLASSMATE),
            isNull(),
            isNull(),
            any(Pageable.class)
        )).willReturn(page);

        mockMvc.perform(get("/api/class-rooms")
                .param("role", "CLASSMATE")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())

            // content
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].classRoomId").value(1L))
            .andExpect(jsonPath("$.content[0].creatorId").value(1L))
            .andExpect(jsonPath("$.content[0].creatorName").value("creator_1"))
            .andExpect(jsonPath("$.content[0].title").value("Spring Boot 입문"))
            .andExpect(jsonPath("$.content[0].price").value(50_000L))
            .andExpect(jsonPath("$.content[0].capacity").value(30))
            .andExpect(jsonPath("$.content[0].enrollmentCount").value(0))
            .andExpect(jsonPath("$.content[0].status").value("OPEN"))
            .andExpect(jsonPath("$.content[0].startAt").value("2026-05-01T00:00:00"))
            .andExpect(jsonPath("$.content[0].endAt").value("2026-06-01T00:00:00"))

            // PageResponse 필드
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1))

            // PageImpl 기본 응답 필드는 없어야 함
            .andExpect(jsonPath("$.pageable").doesNotExist())
            .andExpect(jsonPath("$.sort").doesNotExist())
            .andExpect(jsonPath("$.first").doesNotExist())
            .andExpect(jsonPath("$.last").doesNotExist())
            .andExpect(jsonPath("$.number").doesNotExist())
            .andExpect(jsonPath("$.numberOfElements").doesNotExist())
            .andExpect(jsonPath("$.empty").doesNotExist());
    }

    @Test
    @DisplayName("강의 모집 시작 API에서 creatorId가 없으면 400 validation 응답을 반환한다")
    void openClassRoom_validationFail() throws Exception {
        mockMvc.perform(patch("/api/class-rooms/{classRoomId}/open", 100L)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value(CustomExceptionCode.INVALID_REQUEST.getCode()))
            .andExpect(jsonPath("$.errors.creatorId").exists());
    }

    private Map<String, Object> validRequest() {
        return Map.of(
                "creatorId", 1L,
                "title", "테스트 강의",
                "description", "테스트 설명",
                "price", 10_000L,
                "capacity", 10,
                "startAt", "2026-05-01T10:00:00",
                "endAt", "2026-05-31T10:00:00"
        );
    }
}
