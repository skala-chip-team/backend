package com.skala.chip.reschedule.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * 반환 Map 은 RunResponse 전체(risk_analysis, reschedule_result, decision_summaries ...).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> run(String riskId) {
        Map<String, Object> body = Map.of("risk_id", riskId);
        try {
            return aiRestClient.post()
                    .uri("/run")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientException e) {
            throw new AiAgentException("에이전트 실행(/run) 호출 실패 (risk_id=" + riskId + "): "
                    + e.getMessage(), e);
        }
    }

    /** AI 서비스 호출 실패를 나타내는 런타임 예외. */
    public static class AiAgentException extends RuntimeException {
        public AiAgentException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
