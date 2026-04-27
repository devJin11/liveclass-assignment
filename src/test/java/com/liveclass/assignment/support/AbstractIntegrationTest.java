package com.liveclass.assignment.support;

import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus;
import com.liveclass.assignment.domain.classroom.repository.ClassRoomRepository;
import com.liveclass.assignment.domain.creator.entity.Creator;
import com.liveclass.assignment.domain.creator.repository.CreatorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("enrollment_test")
        .withUsername("test")
        .withPassword("test")
        .withUrlParam("serverTimezone", "Asia/Seoul")
        .withUrlParam("characterEncoding", "UTF-8");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected CreatorRepository creatorRepository;

    @Autowired
    protected ClassRoomRepository classRoomRepository;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE enrollment");
        jdbcTemplate.execute("TRUNCATE TABLE class_room");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    protected ClassRoom createClassRoom(long creatorId, int capacity, ClassRoomStatus status) {
        Creator creator = creatorRepository.getReferenceById(creatorId);

        ClassRoom classRoom = ClassRoom.builder()
            .creator(creator)
            .title("테스트 강의")
            .description("테스트 강의 설명")
            .price(10_000L)
            .capacity(capacity)
            .startAt(LocalDateTime.now().plusDays(1))
            .endAt(LocalDateTime.now().plusDays(30))
            .build();

        if (status == ClassRoomStatus.OPEN) {
            classRoom.open();
        }

        if (status == ClassRoomStatus.CLOSED) {
            classRoom.open();
            classRoom.close();
        }

        return classRoomRepository.saveAndFlush(classRoom);
    }

    protected int enrollmentCountOf(Long classRoomId) {
        return jdbcTemplate.queryForObject(
            "SELECT enrollment_count FROM class_room WHERE class_room_id = ?",
            Integer.class,
            classRoomId
        );
    }

    protected int enrollmentRowCountOf(Long classRoomId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM enrollment WHERE class_room_id = ?",
            Integer.class,
            classRoomId
        );
    }
}