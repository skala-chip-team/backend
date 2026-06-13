package com.skala.chip.chatbot.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 챗봇 에이전트(ai_agent) {@code POST /chat} 요청 바디.
 *
 * 에이전트의 Pydantic 모델(ChatRequest)이 snake_case 필드를 기대하므로
 * {@code @JsonProperty} 로 직렬화 키를 고정한다. (백엔드 전역 네이밍 전략과 무관하게 안정적)
 * null 인 session_id/ref_time 은 전송하지 않는다 — 에이전트는 미지정 시
 * session_id → 새 세션 생성, ref_time → 현재 sim 시각 사용으로 동작한다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentChatRequest {

    @JsonProperty("group_id")
    private String groupId;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("message")
    private String message;

    @JsonProperty("ref_time")
    private String refTime;
}
