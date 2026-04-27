package com.liveclass.assignment.domain.enrollment.service;

import com.liveclass.assignment.domain.classroom.entity.ClassRoom;
import com.liveclass.assignment.support.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.liveclass.assignment.domain.classroom.entity.ClassRoom.ClassRoomStatus.OPEN;
import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private EnrollmentService enrollmentService;

    @Test
    @DisplayName("정원 5명 강의에 10명이 동시에 신청해도 성공자는 5명이고 enrollment_count도 5다")
    void enroll_concurrently_capacityLimit() throws Exception {
        ClassRoom classRoom = createClassRoom(1L, 5, OPEN);
        int requestCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();

        for (long classmateId = 1; classmateId <= requestCount; classmateId++) {
            long currentClassmateId = classmateId;
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    enrollmentService.enroll(classRoom.getId(), currentClassmateId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failCount.get()).isEqualTo(5);
        assertThat(enrollmentCountOf(classRoom.getId())).isEqualTo(5);
        assertThat(enrollmentRowCountOf(classRoom.getId())).isEqualTo(5);
    }

    @Test
    @DisplayName("동일 클래스메이트가 같은 강의에 동시에 여러 번 신청해도 신청 row와 enrollment_count는 1개만 남는다")
    void enroll_concurrently_duplicateSameClassmate() throws Exception {
        ClassRoom classRoom = createClassRoom(1L, 10, OPEN);
        int requestCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(requestCount);
        CountDownLatch readyLatch = new CountDownLatch(requestCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failCount = new AtomicInteger();
        List<Throwable> failures = new CopyOnWriteArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();
                    enrollmentService.enroll(classRoom.getId(), 1L);
                    successCount.incrementAndGet();
                } catch (Throwable e) {
                    failures.add(e);
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(9);
        assertThat(enrollmentCountOf(classRoom.getId())).isEqualTo(1);
        assertThat(enrollmentRowCountOf(classRoom.getId())).isEqualTo(1);
        assertThat(failures).hasSize(9);
    }
}
