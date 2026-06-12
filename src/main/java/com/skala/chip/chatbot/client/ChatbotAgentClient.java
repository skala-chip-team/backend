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
 * 챗봇 에이전트(skala-chip-ai) {@code POST /chat} 호출 클라이언트.
 *
 * reschedule 도메인의 {@code AiAgentClient} 와 동일한 {@code aiRestClient} 빈을 재사용한다.
 * (base-url=skala-chip-ai:8000, read-timeout=120s — LLM 호출이라 길게 잡혀 있어 /chat 에도 적합)
 *
 * 이 클라이언트는 전송 계층만 책임진다 — HTTP 상태코드를 {@link ChatbotAgentException} 에 담아
 * 던지고, 상태코드 → ErrorCode 매핑은 서비스 계층이 수행한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatbotAgentClient {

    private final RestClient aiRestClient;

    /**
     * 에이전트에 질문을 전달하고 답변을 받는다.
     *
     * @throws ChatbotAgentException 에이전트가 4xx/5xx 를 반환하거나(상태코드 포함),
     *                               연결/타임아웃 등으로 호출에 실패한 경우(상태코드 0).
     */
    public AgentChatResponse chat(AgentChatRequest request) {
        try {
            return aiRestClient.post()
                    .uri("/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AgentChatResponse.class);
        } catch (RestClientResponseException e) {
            // 4xx/5xx — 에이전트가 내려준 상태코드를 보존한다.
            log.warn("챗봇 에이전트(/chat) 응답 오류: status={}, body={}",
                    e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new ChatbotAgentException(e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            // 연결 거부 / 타임아웃 등 — 상태코드 없음.
            log.error("챗봇 에이전트(/chat) 호출 실패: {}", e.getMessage());
            throw new ChatbotAgentException(0, e);
        }
    }

    /** 챗봇 에이전트 호출 실패를 나타내는 런타임 예외. HTTP 상태코드(없으면 0)를 보존한다. */
    @Getter
    public static class ChatbotAgentException extends RuntimeException {
        private final int status;

        public ChatbotAgentException(int status, Throwable cause) {
            super("챗봇 에이전트(/chat) 호출 실패 (status=" + status + "): "
                    + (cause == null ? "" : cause.getMessage()), cause);
            this.status = status;
        }
    }
}
