package com.liveclass.assignment.domain.classroom.controller;


import com.liveclass.assignment.domain.classroom.dto.request.CreatorIdRequest;
import com.liveclass.assignment.domain.classroom.dto.request.ClassRoomRequest;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomDetailResponse;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomEnrollmentResponse;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomSummaryResponse;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus;
import com.liveclass.assignment.domain.classroom.service.ClassRoomService;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus;
import com.liveclass.assignment.global.common.PageResponse;
import com.liveclass.assignment.global.common.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/class-rooms")
public class ClassRoomController {

  private final ClassRoomService classRoomService;

  // 1. 인증/인가는 Spring Security를 사용하지 않고,
  // 2. creatorId 또는 classmateId를 요청 파라미터/바디로 전달받아 간략히 처리한다.
  // 3. 단, 단순화를 위해 Creator API는 권한(Role) 검증 없이 creatorId로 존재여부 및 소유권만 검증.
  // 4. Creator/Classmate 공통 조회 API는 role 파라미터로 조회 정책을 분기한다.


  // ============ 1. Creator API ============ //


  /**
   * 강의 등록 API
   *
   * Creator만 가능.
   * 생성된 강의는 최초 DRAFT 상태가 된다.
   */
  @PostMapping
  public ResponseEntity<Void> createClassRoom(@Valid @RequestBody ClassRoomRequest request){
    Long classRoomId = classRoomService.createClassRoom(request);
    return ResponseEntity.created(URI.create("/api/class-rooms/" + classRoomId)).build();
  }

  /**
   * 강의 수정 API
   *
   * Creator만 가능.
   * DRAFT 상태의 강의만 수정 가능.
   */
  @PutMapping("/{classRoomId}")
  public ResponseEntity<Void> updateClassRoom(@PathVariable("classRoomId") Long classRoomId,
                                              @Valid @RequestBody ClassRoomRequest request){
    classRoomService.updateClassRoom(classRoomId, request);
    return ResponseEntity.noContent().build();
  }

  /**
   * 강의 모집 시작 API
   *
   * Creator만 가능.
   * DRAFT -> OPEN.
   */
  @PatchMapping("/{classRoomId}/open")
  public ResponseEntity<Void> openClassRoom(@PathVariable("classRoomId") Long classRoomId,
                                            @Valid @RequestBody CreatorIdRequest request){
    classRoomService.openClassRoom(classRoomId, request.creatorId());
    return ResponseEntity.noContent().build();
  }

  /**
   * 강의 모집 마감 API
   *
   * Creator만 가능.
   * OPEN -> CLOSED.
   */
  @PatchMapping("/{classRoomId}/close")
  public ResponseEntity<Void> closeClassRoom(@PathVariable("classRoomId") Long classRoomId,
                                            @Valid @RequestBody CreatorIdRequest request){
    classRoomService.closeClassRoom(classRoomId, request.creatorId());
    return ResponseEntity.noContent().build();
  }

  /**
   * 강의별 수강생 목록 조회 API
   *
   * Creator만 가능.
   * 본인이 개설한 강의의 수강 신청 목록만 조회 가능.
   *
   * 예:
   * GET /api/class-rooms/1/enrollments?creatorId=1&page=0&size=20
   * GET /api/class-rooms/1/enrollments?creatorId=1&status=CONFIRMED&page=0&size=20
   */
  @GetMapping("/{classRoomId}/enrollments")
  public ResponseEntity<PageResponse<ClassRoomEnrollmentResponse>> getClassRoomEnrollments(@PathVariable("classRoomId") Long classRoomId,
                                                                                           @RequestParam("creatorId") Long creatorId,
                                                                                           @RequestParam(required = false, name = "status") EnrollmentStatus status,
                                                                                           Pageable pageable){
    Page<ClassRoomEnrollmentResponse> responsePage = classRoomService.getClassRoomEnrollments(classRoomId, creatorId, status, pageable);
    return ResponseEntity.ok(PageResponse.from(responsePage));
  }

  // ============ 2. classmate, creator 공통 API ============ //

  /**
   * 강의 목록 조회 API
   *
   * Creator:
   * - 본인 강의 조회
   * - DRAFT, OPEN, CLOSED 조회 가능
   *
   * Classmate:
   * - OPEN, CLOSED 조회 가능
   * - DRAFT 조회 불가
   *
   * 예:
   * GET /api/class-rooms?role=CREATOR&creatorId=1&status=DRAFT&page=0&size=20
   * GET /api/class-rooms?role=CLASSMATE&status=OPEN&page=0&size=20
   */
  @GetMapping
  public ResponseEntity<PageResponse<ClassRoomSummaryResponse>> getClassRooms(@RequestParam("role") Role role,
                                                                              @RequestParam(required = false, name = "creatorId") Long creatorId,
                                                                              @RequestParam(required = false, name = "status") ClassRoomStatus status,
                                                                              Pageable pageable) {
    Page<ClassRoomSummaryResponse> responsePage = classRoomService.getClassRooms(role, creatorId, status, pageable);
    return ResponseEntity.ok(PageResponse.from(responsePage));
  }

  /**
   * 강의 상세 조회 API
   *
   * Creator:
   * - 본인 강의라면 DRAFT, OPEN, CLOSED 조회 가능
   *
   * Classmate:
   * - OPEN, CLOSED 조회 가능
   * - DRAFT 조회 불가
   *
   * 예:
   * GET /api/class-rooms/1?role=CREATOR&creatorId=1
   * GET /api/class-rooms/1?role=CLASSMATE
   */
  @GetMapping("/{classRoomId}")
  public ResponseEntity<ClassRoomDetailResponse> getClassRoomDetail(@PathVariable("classRoomId") Long classRoomId,
                                                                    @RequestParam("role") Role role,
                                                                    @RequestParam(required = false, name = "creatorId") Long creatorId) {
    ClassRoomDetailResponse response = classRoomService.getClassRoomDetail(classRoomId, role, creatorId);
    return ResponseEntity.ok(response);
  }

}
