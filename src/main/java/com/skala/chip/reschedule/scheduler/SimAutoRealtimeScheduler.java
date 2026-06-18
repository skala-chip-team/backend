package com.skala.chip.reschedule.scheduler;

import com.skala.chip.monitoring.repository.DelayRiskRepository;
import com.skala.chip.reschedule.client.AiAgentClient;
import com.skala.chip.reschedule.service.RescheduleGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 위험 상황에 따라 시뮬레이션 속도를 realtime↔fast 로 자동 조절하는 스케줄러.
 *
 * 동작(매 폴링마다 현재 상태로 판정 — 단방향 래치 없음):
 *  - fast→realtime: High/Critical 위험이 새로 감지되거나(maxHighCriticalDetectionTime 전진),
 *    현재 큐에 처리 중인 High/Critical(actionable)이 남아있으면 realtime 으로 전환.
 *    NOTE: 고속에서는 위험 unit 이 5초 폴링 간격 안에 큐를 통과해버려 "큐 잔존(actionable)" 조건만으론
 *          최초 전환을 못 잡으므로, '새 감지' 신호로도 전환을 트리거한다.
 *  - realtime→fast: 큐 잔존 High/Critical(actionable)=0 이고 새 감지도 없는 상태가 일정 시간
 *    (QUIET_POLLS_BEFORE_FAST 폴링) 연속 지속되면 fast 로 복귀. 유예가 없으면 고속에서 위험이
 *    'unit이 큐를 통과한 뒤' 감지(actionable=0,신규=true)되어 realtime↔fast 가 매 폴링 깜빡인다.
 *  - 시뮬 재시작(idle→running, sim_now 되감김) 시 fast 기준으로 상태를 리셋한다.
 *  - 시뮬이 실행 중이 아니면 아무것도 하지 않는다.
 *
 * 멱등/안전: toggle 은 fast↔realtime 플립이므로 ensureRealtime()/ensureFast()가 preset 을 확인해 목표 상태를 보장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimAutoRealtimeScheduler {

    // 시뮬 되감김(재시작) 판정 여유치 (sim-min)
    private static final double RESTART_BACKWARD_MARGIN_MIN = 60.0;
    // realtime→fast 복귀 전 요구되는 연속 '잠잠' 폴링 수(히스테리시스).
    // 폴링 간격 5초 × 12 = 약 60초 동안 위험 활동(큐 잔존/신규 감지)이 전혀 없어야 fast 로 복귀한다.
    // 고속에서는 위험 unit 이 이미 큐를 통과한 뒤 감지(actionable=0, 신규=true)되므로, 유예 없이는
    // realtime↔fast 가 매 폴링 단위로 깜빡인다(플립플롭). 유예로 위험 구간 동안 realtime 을 유지한다.
    private static final int QUIET_POLLS_BEFORE_FAST = 12;

    private final AiAgentClient aiAgentClient;
    private final DelayRiskRepository delayRiskRepository;
    private final RescheduleGroupService rescheduleGroupService;

    private volatile boolean realtimeMode = false;
    private volatile boolean prevRunning = false;
    private volatile Double lastSimNowMin = null;
    // 직전 폴링까지 관측한 가장 최신 High/Critical 감지 시각(새 감지 판정용)
    private volatile LocalDateTime lastSeenDetection = null;
    // 위험 활동 없이 연속으로 잠잠했던 폴링 수(realtime 일 때만 누적)
    private volatile int quietPolls = 0;

    @Scheduled(fixedDelay = 5 * 1000)
    public void autoAdjustSpeedOnRisk() {
        try {
            Map<String, Object> status = aiAgentClient.simStatus();
            if (status == null) {
                return;
            }

            boolean running = Boolean.TRUE.equals(status.get("is_running"));
            if (!running) {
                // 정지/종료 상태 → 다음 run 을 위해 초기화(새 run 은 fast 로 시작)
                realtimeMode = false;
                prevRunning = false;
                lastSimNowMin = null;
                lastSeenDetection = null;
                return;
            }

            Double simNow = status.get("sim_now_min") instanceof Number n ? n.doubleValue() : null;
            boolean backward = simNow != null && lastSimNowMin != null
                    && simNow + RESTART_BACKWARD_MARGIN_MIN < lastSimNowMin;
            // 새 run 시작: 직전에 정지였다가 running 이 되었거나(idle→running), sim_now 가 되감김(재시작).
            boolean newRun = !prevRunning || backward;
            prevRunning = true;
            lastSimNowMin = simNow;

            if (newRun) {
                // 새 run 은 fast 로 시작한다고 보고 상태 리셋
                realtimeMode = false;
                lastSeenDetection = null;
                quietPolls = 0;
                // 새 run 시작 → 이전 재조정 그룹/선택 정리
                try {
                    int cleared = rescheduleGroupService.clearAllForNewSimRun();
                    if (cleared > 0) {
                        log.info("시뮬 새 run 감지 → 이전 재조정 그룹 {}건 정리", cleared);
                    }
                } catch (Exception e) {
                    log.warn("새 run 재조정 그룹 정리 실패: {}", e.getMessage());
                }
            }

            // 새 High/Critical 감지 여부(고속에서도 최초 전환을 잡기 위한 신호)
            LocalDateTime newest = delayRiskRepository.maxHighCriticalDetectionTime();
            boolean newRisk = newest != null
                    && (lastSeenDetection == null || newest.isAfter(lastSeenDetection));
            if (newRisk) {
                lastSeenDetection = newest;
            }

            long actionable = delayRiskRepository.countActionableHighCritical();
            // 위험 '활동': 지금 큐에 처리 중(actionable>0)이거나 직전 폴링 이후 새로 감지됨.
            boolean riskActivity = actionable > 0 || newRisk;

            if (riskActivity) {
                quietPolls = 0; // 위험 활동 있음 → 잠잠 카운터 리셋(유예 재시작)
                if (!realtimeMode) {
                    boolean ok = aiAgentClient.ensureRealtime();
                    realtimeMode = true; // 재시도 폭주 방지: 성공/실패 무관하게 전환 시도는 1회로
                    if (ok) {
                        log.info("위험 감지(actionable={}건, 신규={}) → 시뮬레이션 realtime 자동 전환 (sim_now_min={})",
                                actionable, newRisk, simNow);
                    } else {
                        log.warn("위험 감지(actionable={}건)했으나 realtime 전환 실패(/sim/speed/toggle)", actionable);
                    }
                }
            } else if (realtimeMode) {
                // 위험 활동 없음 → 유예(QUIET_POLLS_BEFORE_FAST) 동안 연속 잠잠해야만 fast 복귀(플립플롭 방지)
                quietPolls++;
                if (quietPolls >= QUIET_POLLS_BEFORE_FAST) {
                    boolean ok = aiAgentClient.ensureFast();
                    realtimeMode = false;
                    quietPolls = 0;
                    if (ok) {
                        log.info("위험 해소(약 {}초간 큐 잔존 0·신규 감지 없음) → 시뮬레이션 fast 복귀 (sim_now_min={})",
                                QUIET_POLLS_BEFORE_FAST * 5, simNow);
                    } else {
                        log.warn("위험 해소됐으나 fast 복귀 실패(/sim/speed/toggle)");
                    }
                }
            }
        } catch (Exception e) {
            // 한 주기 실패가 다음 주기를 막지 않도록 삼킨다.
            log.warn("시뮬 자동 속도 전환 스케줄러 실행 실패: {}", e.getMessage());
        }
    }
}
