package com.liveclass.assignment.support;

import com.liveclass.assignment.global.config.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
public abstract class AbstractJpaRepositoryTest {

    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
        .withDatabaseName("enrollment_jpa_test")
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

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        jdbcTemplate.execute("TRUNCATE TABLE enrollment");
        jdbcTemplate.execute("TRUNCATE TABLE class_room");
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
