package com.skala.chip.chatbot.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 챗봇 에이전트(ai_agent) {@code POST /chat} 응답 바디.
 *
 * 에이전트의 Pydantic 모델(ChatResponse)이 snake_case 로 내려주므로
 * {@code @JsonProperty} 로 역직렬화 키를 매핑한다.
 */
@Getter
@NoArgsConstructor
public class AgentChatResponse {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("answer")
    private String answer;

    @JsonProperty("tool_calls")
    private List<String> toolCalls;
}
