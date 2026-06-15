package com.skala.chip.chatbot.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * AI 서버(skala-chip-ai) {@code POST /infer} 요청 — 무상태 추론.
 *
 * 세션/사용자/시각은 보내지 않는다 (인증=백엔드, 시각=AI 서버 자체 시뮬 시계).
 * AI 서버는 group_id 로 컨텍스트를 로드하고 history + message 로 답한다. (DB 미접근)
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InferRequest {

    @JsonProperty("group_id")
    private String groupId;

    @JsonProperty("message")
    private String message;

    /** 최근 대화 이력(시간순). user/assistant 턴. */
    @JsonProperty("history")
    private List<Turn> history;

    /** 대화 한 턴. role = "user" | "assistant". */
    @Getter
    @Builder
    @AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
    public static class Turn {

        @JsonProperty("role")
        private String role;

        @JsonProperty("content")
        private String content;
    }
}
