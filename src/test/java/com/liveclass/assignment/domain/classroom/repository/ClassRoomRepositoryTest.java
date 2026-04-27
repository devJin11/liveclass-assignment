package com.liveclass.assignment.domain.classroom.repository;

import com.liveclass.assignment.domain.classmate.entity.Classmate;
import com.liveclass.assignment.domain.classmate.repository.ClassmateRepository;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomDetailResponse;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomEnrollmentResponse;
import com.liveclass.assignment.domain.classroom.dto.response.ClassRoomSummaryResponse;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.domain.creator.entity.Creator;
import com.liveclass.assignment.domain.creator.repository.CreatorRepository;
import com.liveclass.assignment.domain.enrollment.entity.Enrollment;
import com.liveclass.assignment.domain.enrollment.repository.EnrollmentRepository;
import com.liveclass.assignment.support.AbstractJpaRepositoryTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus.*;
import static com.liveclass.assignment.domain.enrollment.entity.Enrollment.EnrollmentStatus.PENDING;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClassRoomRepositoryTest extends AbstractJpaRepositoryTest {

    @Autowired
    private ClassRoomRepository classRoomRepository;

    @Autowired
    private CreatorRepository creatorRepository;

    @Autowired
    private ClassmateRepository classmateRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("creatorId와 status로 Creator 강의 목록 projection을 조회한다")
    void findClassRoomSummariesByCreator() {
        ClassRoom draft = createClassRoom(1L, DRAFT);
        createClassRoom(1L, OPEN);

        Page<ClassRoomSummaryResponse> page = classRoomRepository.findClassRoomSummariesByCreator(
                1L,
                DRAFT,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().classRoomId()).isEqualTo(draft.getId());
        assertThat(page.getContent().getFirst().status()).isEqualTo(DRAFT);
    }

    @Test
    @DisplayName("Classmate 강의 목록은 전달된 상태 목록 기준으로 projection 조회된다")
    void findClassRoomSummariesForClassmate() {
        createClassRoom(1L, DRAFT);
        ClassRoom open = createClassRoom(1L, OPEN);
        ClassRoom closed = createClassRoom(1L, CLOSED);

        Page<ClassRoomSummaryResponse> page = classRoomRepository.findClassRoomSummariesForClassmate(
                List.of(OPEN, CLOSED),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(ClassRoomSummaryResponse::classRoomId)
                .containsExactlyInAnyOrder(open.getId(), closed.getId());
    }

    @Test
    @DisplayName("강의 상세 DTO projection을 조회한다")
    void findClassRoomDetail() {
        ClassRoom classRoom = createClassRoom(1L, OPEN);

        ClassRoomDetailResponse detail = classRoomRepository.findClassRoomDetail(classRoom.getId()).orElseThrow();

        assertThat(detail.classRoomId()).isEqualTo(classRoom.getId());
        assertThat(detail.creatorId()).isEqualTo(1L);
        assertThat(detail.status()).isEqualTo(OPEN);
    }

    @Test
    @DisplayName("강의별 수강 신청 목록 projection을 조회한다")
    void findClassRoomEnrollments() {
        ClassRoom classRoom = createClassRoom(1L, OPEN);
        Classmate classmate = classmateRepository.getReferenceById(1L);
        Enrollment enrollment = enrollmentRepository.saveAndFlush(
                Enrollment.createEnrollment(classRoom, classmate, LocalDateTime.now())
        );

        Page<ClassRoomEnrollmentResponse> page = classRoomRepository.findClassRoomEnrollments(
                classRoom.getId(),
                PENDING,
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().enrollmentId()).isEqualTo(enrollment.getId());
        assertThat(page.getContent().getFirst().classmateId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("OPEN 상태이고 정원이 남아 있으면 enrollment_count를 1 증가시킨다")
    void tryIncreaseEnrollmentCount_success() {
        ClassRoom classRoom = createClassRoom(1L, OPEN);

        int updated = classRoomRepository.tryIncreaseEnrollmentCount(classRoom.getId());

        assertThat(updated).isEqualTo(1);
        assertThat(classRoomRepository.findById(classRoom.getId()).orElseThrow().getEnrollmentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("정원이 가득 차면 enrollment_count 증가에 실패한다")
    void tryIncreaseEnrollmentCount_fail_whenFull() {
        ClassRoom classRoom = createClassRoom(1L, OPEN);

        for (int i = 0; i < 10; i++) {
            int updated = classRoomRepository.tryIncreaseEnrollmentCount(classRoom.getId());
            assertThat(updated).isEqualTo(1);
        }

        int updated = classRoomRepository.tryIncreaseEnrollmentCount(classRoom.getId());

        assertThat(updated).isZero();
    }

    private ClassRoom createClassRoom(long creatorId, ClassRoom.ClassRoomStatus status) {
        Creator creator = creatorRepository.getReferenceById(creatorId);
        ClassRoom classRoom = ClassRoom.builder()
                .creator(creator)
                .title("테스트 강의")
                .description("테스트 설명")
                .price(10_000L)
                .capacity(10)
                .startAt(LocalDateTime.now().plusDays(1))
                .endAt(LocalDateTime.now().plusDays(10))
                .build();

        if (status == OPEN) {
            classRoom.open();
        }
        if (status == CLOSED) {
            classRoom.open();
            classRoom.close();
        }
        return classRoomRepository.saveAndFlush(classRoom);
    }
}
