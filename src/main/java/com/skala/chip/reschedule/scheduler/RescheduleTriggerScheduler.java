package com.skala.chip.reschedule.scheduler;

import com.skala.chip.reschedule.service.RescheduleOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 자동 재조정 트리거 스케줄러.
 *
 * 1분마다 최신 delay_risk 를 그룹핑해, risk_level 이 High/Critical 인 신규 그룹이 생기면
 * 자동으로 에이전트(/run)를 호출해 재조정안을 생성한다. (모델 실행 predict 는 제외 — A안)
 *
 * 멱등: 이미 reschedule_detail 이 있는 그룹은 스킵하므로 중복 호출하지 않는다.
 * 에이전트(LLM) 호출이 1분보다 길어질 수 있어 fixedDelay 로 직전 실행 종료 후 간격을 둔다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RescheduleTriggerScheduler {

    private final RescheduleOrchestrationService orchestrationService;

    @Scheduled(fixedDelay = 60 * 1000)
    public void detectAndTrigger() {
        try {
            int generated = orchestrationService.triggerAndGenerate();
            if (generated > 0) {
                log.info("자동 재조정 트리거: 신규 위험 그룹 {}건 에이전트 호출/생성 완료", generated);
            }
        } catch (Exception e) {
            // 한 주기 실패가 다음 주기를 막지 않도록 삼킨다.
            log.warn("자동 재조정 트리거 실행 실패: {}", e.getMessage());
        }
    }
}
