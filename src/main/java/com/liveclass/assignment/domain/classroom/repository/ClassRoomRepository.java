package com.liveclass.assignment.domain.classroom.repository;

import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomDetailResponse;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomEnrollmentResponse;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomSummaryResponse;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;


public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

  boolean existsByIdAndCreator_Id(Long classRoomId, Long creatorId);

  /**
   * 상태 변경용 단건 조회.
   *
   * 강의 수정, 모집 시작, 모집 마감에서
   * 도메인 메서드 호출과 소유권 검증을 위해 사용한다.
   */
  @Query("""
      select cr
      from ClassRoom cr
      join fetch cr.creator c
      where cr.id = :classRoomId
      """)
  Optional<ClassRoom> findByIdWithCreator(Long classRoomId);


  /**
   * Creator 강의 목록 조회.
   *
   * status가 null이면 본인 강의 전체 조회.
   */
  @Query(
      value = """
          select new com.liveclass.assignment.domain.classroom.dto.response.ClassRoomSummaryResponse(
              cr.id,
              c.id,
              c.name,
              cr.title,
              cr.price,
              cr.capacity,
              cr.enrollmentCount,
              cr.status,
              cr.startAt,
              cr.endAt
          )
          from ClassRoom cr
          join cr.creator c
          where c.id = :creatorId
            and (:status is null or cr.status = :status)
          order by cr.createdAt desc
          """,
      countQuery = """
          select count(cr)
          from ClassRoom cr
          join cr.creator c
          where c.id = :creatorId
            and (:status is null or cr.status = :status)
          """
  )
  Page<ClassRoomSummaryResponse> findClassRoomSummariesByCreator(
      Long creatorId,
      ClassRoomStatus status,
      Pageable pageable
  );

  /**
   * Classmate 강의 목록 조회.
   *
   * OPEN, CLOSED만 조회 가능.
   * status가 null이면 OPEN/CLOSED 전체 조회.
   */
  @Query(
      value = """
          select new com.liveclass.assignment.domain.classroom.dto.response.ClassRoomSummaryResponse(
              cr.id,
              c.id,
              c.name,
              cr.title,
              cr.price,
              cr.capacity,
              cr.enrollmentCount,
              cr.status,
              cr.startAt,
              cr.endAt
          )
          from ClassRoom cr
          join cr.creator c
          where cr.status in :statuses
          order by cr.createdAt desc
          """,
      countQuery = """
          select count(cr)
          from ClassRoom cr
          where cr.status in :statuses
          """
  )
  Page<ClassRoomSummaryResponse> findClassRoomSummariesForClassmate(
      Collection<ClassRoomStatus> statuses,
      Pageable pageable
  );

  /**
   * 강의 상세 조회 DTO projection.
   */
  @Query("""
      select new com.liveclass.assignment.domain.classroom.dto.response.ClassRoomDetailResponse(
          cr.id,
          c.id,
          c.name,
          cr.title,
          cr.description,
          cr.price,
          cr.capacity,
          cr.enrollmentCount,
          cr.status,
          cr.startAt,
          cr.endAt,
          cr.createdAt,
          cr.updatedAt
      )
      from ClassRoom cr
      join cr.creator c
      where cr.id = :classRoomId
      """)
  Optional<ClassRoomDetailResponse> findClassRoomDetail(Long classRoomId);

  /**
   * 강의별 수강 신청 목록 조회.
   *
   * status가 null이면 전체 상태 조회.
   */
  @Query(
      value = """
          select new com.liveclass.assignment.domain.classroom.dto.response.ClassRoomEnrollmentResponse(
              e.id,
              cm.id,
              cm.name,
              e.status,
              e.paymentExpiredAt,
              e.confirmedAt,
              e.cancelledAt,
              e.createdAt
          )
          from Enrollment e
          join e.classmate cm
          where e.classRoom.id = :classRoomId
            and (:status is null or e.status = :status)
          order by e.createdAt desc
          """,
      countQuery = """
          select count(e)
          from Enrollment e
          where e.classRoom.id = :classRoomId
            and (:status is null or e.status = :status)
          """
  )
  Page<ClassRoomEnrollmentResponse> findClassRoomEnrollments(
      Long classRoomId,
      EnrollmentStatus status,
      Pageable pageable
  );

  // ============================== //

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    update ClassRoom cr
       set cr.enrollmentCount = cr.enrollmentCount + 1
     where cr.id = :classRoomId
       and cr.status = com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus.OPEN
       and cr.enrollmentCount < cr.capacity
    """)
  int tryIncreaseEnrollmentCount(@Param("classRoomId") Long classRoomId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
    update ClassRoom cr
       set cr.enrollmentCount = cr.enrollmentCount - 1
     where cr.id = :classRoomId
       and cr.enrollmentCount > 0
    """)
  int decreaseEnrollmentCount(@Param("classRoomId") Long classRoomId);

  @Query("""
    select cr.status
    from ClassRoom cr
    where cr.id = :classRoomId
    """)
  ClassRoomStatus findStatusById(@Param("classRoomId") Long classRoomId);




}
