package com.liveclass.assignment.domain.enrollment.repository;

import com.liveclass.assignment.domain.classmate.entity.Classmate;
import com.liveclass.assignment.domain.classmate.repository.ClassmateRepository;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.domain.classroom.repository.ClassRoomRepository;
import com.liveclass.assignment.domain.creator.entity.Creator;
import com.liveclass.assignment.domain.creator.repository.CreatorRepository;
import com.liveclass.assignment.domain.enrollment.dto.response.EnrollmentDetailResponse;
import com.liveclass.assignment.domain.enrollment.dto.response.MyEnrollmentResponse;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment;
import com.liveclass.assignment.support.AbstractJpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EnrollmentRepositoryTest extends AbstractJpaRepositoryTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private CreatorRepository creatorRepository;

    @Autowired
    private ClassmateRepository classmateRepository;

    @Test
    @DisplayName("강의 ID와 클래스메이트 ID로 수강 신청을 조회한다")
    void findByClassRoomIdAndClassmateIdForUpdate() {
        ClassRoom classRoom = createOpenClassRoom(1L, 10);
        Classmate classmate = classmateRepository.getReferenceById(1L);
        Enrollment enrollment = enrollmentRepository.saveAndFlush(
                Enrollment.createEnrollment(classRoom, classmate, LocalDateTime.now())
        );

        var found = enrollmentRepository.findByClassRoomIdAndClassmateIdForUpdate(classRoom.getId(), 1L);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(enrollment.getId());
    }

    @Test
    @DisplayName("내 수강 신청 목록을 최신순으로 조회한다")
    void findMyEnrollments() {
        ClassRoom classRoom = createOpenClassRoom(1L, 10);
        Classmate classmate = classmateRepository.getReferenceById(1L);
        enrollmentRepository.saveAndFlush(Enrollment.createEnrollment(classRoom, classmate, LocalDateTime.now()));

        Page<MyEnrollmentResponse> page = enrollmentRepository.findMyEnrollments(1L, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).classRoomId()).isEqualTo(classRoom.getId());
        assertThat(page.getContent().get(0).status()).isEqualTo(PENDING);
    }

    @Test
    @DisplayName("결제 만료된 PENDING 신청 목록을 조회한다")
    void findExpiredPendingEnrollmentsForUpdate() {
        ClassRoom classRoom = createOpenClassRoom(1L, 10);
        Classmate classmate = classmateRepository.getReferenceById(1L);
        Enrollment enrollment = enrollmentRepository.saveAndFlush(
                Enrollment.createEnrollment(classRoom, classmate, LocalDateTime.now().minusMinutes(20))
        );

        List<Enrollment> expired = enrollmentRepository.findExpiredPendingEnrollmentsForUpdate(
                LocalDateTime.now(),
                PageRequest.of(0, 50)
        );

        assertThat(expired).extracting(Enrollment::getId).containsExactly(enrollment.getId());
    }

    @Test
    @DisplayName("수강 신청 상세 DTO projection을 조회한다")
    void findEnrollmentDetail() {
        ClassRoom classRoom = createOpenClassRoom(1L, 10);
        Classmate classmate = classmateRepository.getReferenceById(1L);
        Enrollment enrollment = enrollmentRepository.saveAndFlush(
                Enrollment.createEnrollment(classRoom, classmate, LocalDateTime.now())
        );

        EnrollmentDetailResponse detail = enrollmentRepository.findEnrollmentDetail(enrollment.getId()).orElseThrow();

        assertThat(detail.enrollmentId()).isEqualTo(enrollment.getId());
        assertThat(detail.classRoomId()).isEqualTo(classRoom.getId());
        assertThat(detail.classmateId()).isEqualTo(1L);
        assertThat(detail.status()).isEqualTo(PENDING);
    }

    private ClassRoom createOpenClassRoom(long creatorId, int capacity) {
        Creator creator = creatorRepository.getReferenceById(creatorId);
        ClassRoom classRoom = ClassRoom.builder()
                .creator(creator)
                .title("테스트 강의")
                .description("테스트 설명")
                .price(10_000L)
                .capacity(capacity)
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(10))
                .build();
        classRoom.open();
        return classRoomRepository.saveAndFlush(classRoom);
    }
}
