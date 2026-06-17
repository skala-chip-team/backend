package com.skala.chip.reschedule.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 재조정 에이전트(skala-chip-ai) 호출 클라이언트.
 *
 * 제공 엔드포인트:
 *  - POST /predict  {snap_time}         : 모델 실행(지연 예측 → delay_risk 갱신)
 *  - POST /run      {risk_id}           : 에이전트 실행(위험분석 + 재조정안 생성)
 *
 * 응답은 jsonb 로 그대로 저장/노출하므로 Map 으로 받는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAgentClient {

    // 초가 0이어도 항상 'ss' 까지 포함시켜 FastAPI(pydantic) 파싱을 안정화한다.
    private static final DateTimeFormatter SNAP_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final RestClient aiRestClient;

    /**
     * 모델 실행. 주어진 시각(snap_time) 기준으로 지연 예측을 수행한다.
     * 예측 결과는 AI 서비스가 공유 DB(delay_risk)에 기록한다.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> predict(LocalDateTime snapTime) {
        Map<String, Object> body = Map.of("snap_time", snapTime.format(SNAP_TIME_FORMAT));
        try {
            return aiRestClient.post()
                    .uri("/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new AiAgentException("모델 실행(/predict) 호출 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 에이전트 실행. 대표 risk_id 에 대한 재조정안(reschedule_options 포함)을 생성한다.
     * groupId 는 탐지 단계에서 이미 생성된 reschedule_group.group_id 로, AI 가 결과를
     * 어느 그룹에 대응시킬지 식별하도록 함께 전달한다. (null 이면 생략)
     * 반환 Map 은 RunResponse 전체(risk_analysis, reschedule_result, decision_summaries ...).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> run(String riskId, String groupId) {
        Map<String, Object> body = new HashMap<>();
        body.put("risk_id", riskId);
        if (groupId != null) {
            body.put("group_id", groupId);
        }
        try {
            return aiRestClient.post()
                    .uri("/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (HttpClientErrorException e) {
            // 404(risk_id not found) / 409(risk unit is not in the current queue) 은 서버 오류가 아니라
            // "현재 큐에서 처리할 수 없는 위험" 이라는 데이터 조건이다. 502 가 아닌 별도 예외로 구분한다.
            if (e.getStatusCode().value() == 404 || e.getStatusCode().value() == 409) {
                throw new NotActionableException("재조정 불가 위험 (risk_id=" + riskId
                        + ", group_id=" + groupId + "): " + e.getResponseBodyAsString(), e);
            }
            throw new AiAgentException("에이전트 실행(/run) 호출 실패 (risk_id=" + riskId
                    + ", group_id=" + groupId + "): " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new AiAgentException("에이전트 실행(/run) 호출 실패 (risk_id=" + riskId
                    + ", group_id=" + groupId + "): " + e.getMessage(), e);
        }
    }

    /**
     * 러닝 시뮬레이션 큐 재정렬(/replan). 승인된 재조정의 새 순서를 살아있는 sim 엔진에 반영한다.
     * @param district 구역 id, @param step 공정명(process_step, 예: STEP_A), @param newOrder 새 unit 순서(현재 대기 큐 집합과 일치해야 함)
     * @return 반영 성공 여부. sim 미실행(409)·검증 실패(400) 등은 false 로 처리하고 예외를 던지지 않는다(승인 자체는 성공으로 둔다).
     */
    public boolean replan(String district, String step, java.util.List<String> newOrder) {
        Map<String, Object> body = new HashMap<>();
        body.put("district", district);
        body.put("step", step);
        body.put("new_order", newOrder);
        try {
            aiRestClient.post()
                    .uri("/replan")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return true;
        } catch (HttpClientErrorException e) {
            // 409: sim 미실행 / 400: new_order 불일치 — 승인은 유지하고 sim 반영만 생략
            log.info("/replan 미반영 (district={}, step={}, status={}): {}",
                    district, step, e.getStatusCode().value(), e.getResponseBodyAsString());
            return false;
        } catch (RestClientException e) {
            log.warn("/replan 호출 실패 (district={}, step={}): {}", district, step, e.getMessage());
            return false;
        }
    }

    // ── 시뮬레이션 속도 제어 (/sim/*) ────────────────────────────────────────

    /** 시뮬레이션 상태 조회(/sim/status). 실패 시 null. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> simStatus() {
        try {
            return aiRestClient.get().uri("/sim/status").retrieve().body(Map.class);
        } catch (RestClientException e) {
            log.warn("/sim/status 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    /** 시뮬레이션 속도 토글(/sim/speed/toggle). 응답(sim_factor/preset) 반환, 실패 시 null. */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toggleSpeed() {
        try {
            return aiRestClient.post()
                    .uri("/sim/speed/toggle")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of())
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            log.warn("/sim/speed/toggle 호출 실패: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 시뮬레이션을 realtime 으로 보장한다.
     * toggle 은 fast↔realtime 플립이라, 응답 preset 을 확인해 realtime 이 아니면 한 번 더 토글한다(최대 2회).
     * @return realtime 전환 성공 여부
     */
    public boolean ensureRealtime() {
        for (int i = 0; i < 2; i++) {
            Map<String, Object> res = toggleSpeed();
            if (res == null) {
                return false;
            }
            if ("realtime".equals(res.get("preset"))) {
                return true;
            }
        }
        return false;
    }

    /** AI 서비스 호출 실패를 나타내는 런타임 예외. */
    public static class AiAgentException extends RuntimeException {
        public AiAgentException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 위험이 현재 큐에서 처리 불가능함을 나타내는 예외(/run 404·409).
     * 에이전트/서버 장애가 아니라 데이터 상태이므로 호출측에서 502 대신 409 로 매핑한다.
     */
    public static class NotActionableException extends RuntimeException {
        public NotActionableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
