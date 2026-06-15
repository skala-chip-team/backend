package com.skala.chip.chatbot.client;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * AI 서버(skala-chip-ai) {@code POST /infer} 호출 클라이언트 — 무상태 추론.
 *
 * reschedule 도메인의 {@code AiAgentClient} 와 동일한 {@code aiRestClient} 빈을 재사용한다.
 * (base-url=skala-chip-ai:8000, read-timeout=120s — LLM 호출이라 길게 잡혀 있음)
 *
 * 세션/저장/인증은 백엔드가 담당하고, 이 클라이언트는 추론 요청·응답만 중계한다.
 * HTTP 상태코드를 {@link ChatbotAgentException} 에 담아 던지고, 매핑은 서비스 계층이 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatbotAgentClient {

    private final RestClient aiRestClient;

    /**
     * 추론 호출. group_id 컨텍스트 + history + message 로 답변을 받는다.
     *
     * @throws ChatbotAgentException AI 서버가 4xx/5xx 를 반환하거나(상태코드 포함),
     *                               연결/타임아웃 등으로 호출에 실패한 경우(상태코드 0).
     */
    public InferResponse infer(InferRequest request) {
        try {
            return aiRestClient.post()
                    .uri("/infer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(InferResponse.class);
        } catch (RestClientResponseException e) {
            log.warn("AI 추론(/infer) 응답 오류: status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new ChatbotAgentException(e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            log.error("AI 추론(/infer) 호출 실패: {}", e.getMessage());
            throw new ChatbotAgentException(0, e);
        }
    }

    /** AI 추론 호출 실패를 나타내는 런타임 예외. HTTP 상태코드(없으면 0)를 보존한다. */
    @Getter
    public static class ChatbotAgentException extends RuntimeException {
        private final int status;

        public ChatbotAgentException(int status, Throwable cause) {
            super("AI 추론(/infer) 호출 실패 (status=" + status + "): "
                    + (cause == null ? "" : cause.getMessage()), cause);
            this.status = status;
        }
    }
}
