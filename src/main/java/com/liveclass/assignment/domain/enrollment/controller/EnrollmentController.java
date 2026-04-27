package com.liveclass.assignment.domain.enrollment.controller;

import com.liveclass.assignment.domain.enrollment.dto.request.ClassmateIdRequest;
import com.liveclass.assignment.domain.enrollment.dto.response.EnrollmentDetailResponse;
import com.liveclass.assignment.domain.enrollment.dto.response.EnrollmentResponse;
import com.liveclass.assignment.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.liveclass.assignment.domain.enrollment.service.EnrollmentService;
import com.liveclass.assignment.global.common.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class EnrollmentController {

  private final EnrollmentService enrollmentService;

  /**
   * 수강 신청.
   *
   * OPEN 상태의 강의에만 신청 가능.
   * 성공 시 PENDING 상태가 된다.
   */
  @PostMapping("/api/class-rooms/{classRoomId}/enrollments")
  public ResponseEntity<EnrollmentResponse> enroll(@PathVariable("classRoomId") Long classRoomId,
                                                   @Valid @RequestBody ClassmateIdRequest request) {
    EnrollmentResponse response = enrollmentService.enroll(classRoomId, request.classmateId());

    return ResponseEntity
        .created(URI.create("/api/enrollments/" + response.enrollmentId()))
        .body(response);
  }

  /**
   * 결제 확정.
   *
   * PENDING -> CONFIRMED.
   */
  @PatchMapping("/api/enrollments/{enrollmentId}/confirm")
  public ResponseEntity<EnrollmentResponse> confirm(@PathVariable("enrollmentId") Long enrollmentId,
                                                    @Valid @RequestBody ClassmateIdRequest request) {
    EnrollmentResponse response = enrollmentService.confirm(enrollmentId, request.classmateId());
    return ResponseEntity.ok(response);
  }

  /**
   * 수강 취소.
   *
   * PENDING -> CANCELLED
   * CONFIRMED -> CANCELLED
   */
  @PatchMapping("/api/enrollments/{enrollmentId}/cancel")
  public ResponseEntity<EnrollmentResponse> cancel(@PathVariable("enrollmentId") Long enrollmentId,
                                                   @Valid @RequestBody ClassmateIdRequest request) {
    EnrollmentResponse response = enrollmentService.cancel(enrollmentId, request.classmateId());
    return ResponseEntity.ok(response);
  }

  /**
   * 내 수강 신청 목록 조회.
   */
  @GetMapping("/api/enrollments/me")
  public ResponseEntity<PageResponse<MyEnrollmentResponse>> getMyEnrollments(@RequestParam("classmateId") Long classmateId,
                                                                             Pageable pageable) {
    Page<MyEnrollmentResponse> responsePage = enrollmentService.getMyEnrollments(classmateId, pageable);
    return ResponseEntity.ok(PageResponse.from(responsePage));
  }

  /**
   *  수강 신청 상세 조회(단건 조회)
   * */
  @GetMapping("/api/enrollments/{enrollmentId}")
  public ResponseEntity<EnrollmentDetailResponse> getEnrollmentDetail(@PathVariable("enrollmentId") Long enrollmentId,
                                                                      @RequestParam("classmateId") Long classmateId) {
    EnrollmentDetailResponse response = enrollmentService.getEnrollmentDetail(enrollmentId, classmateId);
    return ResponseEntity.ok(response);
  }



}