package com.liveclass.assignment.domain.enrollment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liveclass.assignment.domain.enrollment.dto.response.EnrollmentResponse;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus;
import com.liveclass.assignment.domain.enrollment.service.EnrollmentService;
import com.liveclass.assignment.global.exception.CustomExceptionCode;
import com.liveclass.assignment.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnrollmentController.class)
class EnrollmentControllerTest {

    private static final LocalDateTime BASE_TIME =
        LocalDateTime.of(2026, 4, 28, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Test
    @DisplayName("수강 신청 API 성공 시 201, Location, 응답 body를 반환한다")
    void enroll_success() throws Exception {
        EnrollmentResponse response = response(100L, 1L, 1L, EnrollmentStatus.PENDING);
        given(enrollmentService.enroll(1L, 1L)).willReturn(response);

        mockMvc.perform(post("/api/class-rooms/{classRoomId}/enrollments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("classmateId", 1L))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/enrollments/100"))
                .andExpect(jsonPath("$.enrollmentId").value(100L))
                .andExpect(jsonPath("$.classRoomId").value(1L))
                .andExpect(jsonPath("$.classmateId").value(1L))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("수강 신청 API에서 classmateId가 없으면 400 validation 응답을 반환한다")
    void enroll_validationFail() throws Exception {
        mockMvc.perform(post("/api/class-rooms/{classRoomId}/enrollments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.totalErrorCount").value(1))
                .andExpect(jsonPath("$.code").value(CustomExceptionCode.INVALID_REQUEST.getCode()))
                .andExpect(jsonPath("$.errors.classmateId").value("클래스메이트 ID는 필수입니다."));
    }

    @Test
    @DisplayName("수강 신청 API에서 JSON 타입이 맞지 않으면 400 타입 오류 응답을 반환한다")
    void enroll_invalidJsonType() throws Exception {
        mockMvc.perform(post("/api/class-rooms/{classRoomId}/enrollments", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classmateId\": \"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(CustomExceptionCode.INVALID_TYPE_REQUEST.getCode()))
                .andExpect(jsonPath("$.message").value(CustomExceptionCode.INVALID_TYPE_REQUEST.getMessage()));
    }

    @Test
    @DisplayName("서비스에서 NotFoundException이 발생하면 404 예외 응답을 반환한다")
    void enroll_notFound() throws Exception {
        given(enrollmentService.enroll(999L, 1L))
                .willThrow(new NotFoundException(CustomExceptionCode.CLASS_ROOM_NOT_FOUND));

        mockMvc.perform(post("/api/class-rooms/{classRoomId}/enrollments", 999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("classmateId", 1L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(CustomExceptionCode.CLASS_ROOM_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(CustomExceptionCode.CLASS_ROOM_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("결제 확정 API 성공 시 200과 CONFIRMED 응답을 반환한다")
    void confirm_success() throws Exception {
        given(enrollmentService.confirm(100L, 1L))
                .willReturn(response(100L, 1L, 1L, EnrollmentStatus.CONFIRMED));

        mockMvc.perform(patch("/api/enrollments/{enrollmentId}/confirm", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("classmateId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentId").value(100L))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("수강 취소 API 성공 시 200과 CANCELLED 응답을 반환한다")
    void cancel_success() throws Exception {
        given(enrollmentService.cancel(100L, 1L))
                .willReturn(response(100L, 1L, 1L, EnrollmentStatus.CANCELLED));

        mockMvc.perform(patch("/api/enrollments/{enrollmentId}/cancel", 100L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("classmateId", 1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentId").value(100L))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    private EnrollmentResponse response(
        Long enrollmentId,
        Long classRoomId,
        Long classmateId,
        EnrollmentStatus status
    ) {
        return new EnrollmentResponse(
            enrollmentId,
            classRoomId,
            classmateId,
            status,
            BASE_TIME.plusMinutes(10),
            status == EnrollmentStatus.CONFIRMED ? BASE_TIME.plusMinutes(1) : null,
            status == EnrollmentStatus.CANCELLED ? BASE_TIME.plusMinutes(2) : null,
            BASE_TIME
        );
    }
}
