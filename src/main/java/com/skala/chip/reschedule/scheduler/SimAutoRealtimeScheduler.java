package com.skala.chip.reschedule.scheduler;

import com.skala.chip.monitoring.repository.DelayRiskRepository;
import com.skala.chip.reschedule.client.AiAgentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 위험 감지 시 시뮬레이션을 자동으로 realtime 으로 전환하는 스케줄러.
 *
 * 동작:
 *  - 시뮬이 fast 로 돌다가 "현재 큐에 남아있는 High/Critical 위험"이 처음 잡히면
 *    그 시점부터 realtime(/sim/speed/toggle)으로 전환한다. (unit 이 큐에 머무는 동안 감지·재조정·예측이 살아남게)
 *  - 한 run 당 1회만 전환한다(switchedThisRun 플래그). 시뮬이 재시작(sim_now 가 되감김)되면 플래그를 리셋한다.
 *  - 시뮬이 실행 중이 아니면 아무것도 하지 않는다.
 *
 * 멱등/안전: toggle 은 fast↔realtime 플립이므로 ensureRealtime()이 preset 을 확인해 realtime 을 보장한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SimAutoRealtimeScheduler {

    // 시뮬 되감김(재시작) 판정 여유치 (sim-min)
    private static final double RESTART_BACKWARD_MARGIN_MIN = 60.0;

    private final AiAgentClient aiAgentClient;
    private final DelayRiskRepository delayRiskRepository;

    private volatile boolean switchedThisRun = false;
    private volatile Double lastSimNowMin = null;

    @Scheduled(fixedDelay = 5 * 1000)
    public void autoSwitchToRealtimeOnRisk() {
        try {
            Map<String, Object> status = aiAgentClient.simStatus();
            if (status == null) {
                return;
            }

            boolean running = Boolean.TRUE.equals(status.get("is_running"));
            if (!running) {
                // 정지/종료 상태 → 다음 run 을 위해 초기화
                switchedThisRun = false;
                lastSimNowMin = null;
                return;
            }

            Double simNow = status.get("sim_now_min") instanceof Number n ? n.doubleValue() : null;
            // 시뮬 재시작(sim_now 되감김) 감지 → 플래그 리셋
            if (simNow != null && lastSimNowMin != null
                    && simNow + RESTART_BACKWARD_MARGIN_MIN < lastSimNowMin) {
                switchedThisRun = false;
            }
            lastSimNowMin = simNow;

            if (switchedThisRun) {
                return; // 이번 run 은 이미 realtime 으로 전환함
            }

            long actionable = delayRiskRepository.countActionableHighCritical();
            if (actionable <= 0) {
                return; // 아직 감지된 High/Critical 위험 없음 → fast 유지
            }

            boolean ok = aiAgentClient.ensureRealtime();
            switchedThisRun = true; // 재시도 폭주 방지: 성공/실패 무관하게 이번 run 1회로 제한
            if (ok) {
                log.info("위험 감지({}건) → 시뮬레이션 realtime 자동 전환 (sim_now_min={})", actionable, simNow);
            } else {
                log.warn("위험 감지({}건)했으나 realtime 전환 실패(/sim/speed/toggle)", actionable);
            }
        } catch (Exception e) {
            // 한 주기 실패가 다음 주기를 막지 않도록 삼킨다.
            log.warn("시뮬 자동 realtime 전환 스케줄러 실행 실패: {}", e.getMessage());
        }
    }
}
