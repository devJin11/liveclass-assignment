package com.liveclass.assignment.global.scheduler;


import com.liveclass.assignment.domain.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnrollmentPaymentExpireScheduler {

  private final EnrollmentService enrollmentService;

  /**
   * 1분마다 결제 대기 시간이 만료된 PENDING 신청을 CANCELLED 처리한다.
   */
  @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
  public void cancelExpiredPendingEnrollments() {
    int cancelledCount = enrollmentService.cancelExpiredPendingEnrollments();

    if (cancelledCount > 0) {
      log.info("결제 대기 만료 수강 신청 자동 취소 완료. cancelledCount={}", cancelledCount);
    }

  }

}