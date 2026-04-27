package com.liveclass.assignment.domain.enrollment.repository;

import com.liveclass.assignment.domain.enrollment.dto.response.EnrollmentDetailResponse;
import com.liveclass.assignment.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

  /**
   * 동일 수강생의 같은 강의 신청 여부 확인.
   *
   * 중복 신청/재신청 동시 요청을 제어하기 위해 Enrollment row만 비관적 락으로 잠근다.
   * fetch join을 사용하지 않아 class_room, classmate row까지 락이 확산되는 것을 줄인다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select e
      from Enrollment e
      where e.classRoom.id = :classRoomId
        and e.classmate.id = :classmateId
      """)
  Optional<Enrollment> findByClassRoomIdAndClassmateIdForUpdate(@Param("classRoomId") Long classRoomId,
                                                                @Param("classmateId") Long classmateId);

  /**
   * 결제 확정/취소용 단건 조회.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
    select e
    from Enrollment e
    join fetch e.classRoom cr
    join fetch e.classmate cm
    where e.id = :enrollmentId
    """)
  Optional<Enrollment> findByIdWithClassRoomAndClassmateForUpdate(@Param("enrollmentId") Long enrollmentId);

  /**
   * 내 수강 신청 목록 조회.
   */
  @Query(
      value = """
          select new com.liveclass.assignment.domain.enrollment.dto.response.MyEnrollmentResponse(
              e.id,
              cr.id,
              cr.title,
              cr.price,
              e.status,
              e.paymentExpiredAt,
              e.confirmedAt,
              e.cancelledAt,
              e.createdAt
          )
          from Enrollment e
          join e.classRoom cr
          where e.classmate.id = :classmateId
          order by e.createdAt desc
          """,
      countQuery = """
          select count(e)
          from Enrollment e
          where e.classmate.id = :classmateId
          """
  )
  Page<MyEnrollmentResponse> findMyEnrollments(@Param("classmateId") Long classmateId, Pageable pageable);

  /**
   * 결제 만료 자동 취소 대상 조회.
   *
   * Enrollment row만 비관적 락으로 잠근다.
   * fetch join을 제거하여 class_room, classmate row까지 락이 확산되는 것을 줄인다.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select e
      from Enrollment e
      where e.status = com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus.PENDING
        and e.paymentExpiredAt <= :now
      order by e.paymentExpiredAt asc
      """)
  List<Enrollment> findExpiredPendingEnrollmentsForUpdate(@Param("now") LocalDateTime now, Pageable pageable);

  /**
   *  수강 신청 단건 상세 조회
   * */
  @Query("""
    select new com.liveclass.assignment.domain.enrollment.dto.response.EnrollmentDetailResponse(
        e.id,
        cr.id,
        cr.title,
        cr.price,
        cm.id,
        cm.name,
        e.status,
        e.paymentExpiredAt,
        e.confirmedAt,
        e.cancelledAt,
        e.cancelReason,
        e.createdAt,
        e.updatedAt
    )
    from Enrollment e
    join e.classRoom cr
    join e.classmate cm
    where e.id = :enrollmentId
    """)
  Optional<EnrollmentDetailResponse> findEnrollmentDetail(@Param("enrollmentId") Long enrollmentId);

}