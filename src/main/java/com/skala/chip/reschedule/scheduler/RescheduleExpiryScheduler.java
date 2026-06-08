package com.skala.chip.reschedule.scheduler;

import com.skala.chip.reschedule.repository.RescheduleGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 재조정 그룹 자동 만료 스케줄러.
 * 생성(acted_at) 후 일정 시간(1시간) 동안 승인되지 않은 pending 그룹을 expired 로 전환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RescheduleExpiryScheduler {

    // 만료 기준 시간 (생성 후 1시간)
    private static final long EXPIRY_HOURS = 1;

    private final RescheduleGroupRepository rescheduleGroupRepository;

    // 5분마다 점검
    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Transactional
    public void expireOldPendingGroups() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(EXPIRY_HOURS);
        int expired = rescheduleGroupRepository.expirePendingOlderThan(threshold);
        if (expired > 0) {
            log.info("재조정 그룹 자동 만료: {}건 (기준 {} 이전)", expired, threshold);
        }
    }
}
