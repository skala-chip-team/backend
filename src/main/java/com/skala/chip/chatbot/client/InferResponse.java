package com.skala.chip.chatbot.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI 서버(skala-chip-ai) {@code POST /infer} 응답 — 무상태 추론 결과.
 *
 * AI 서버는 답변·도구호출·과거사례 출처만 돌려준다 (DB 저장은 백엔드가).
 */
@Getter
@NoArgsConstructor
public class InferResponse {

    @JsonProperty("answer")
    private String answer;

    @JsonProperty("tool_calls")
    private List<String> toolCalls;

    /** 과거 사례 검색 출처(구조화). */
    @JsonProperty("sources")
    private List<Source> sources;

    @Getter
    @NoArgsConstructor
    public static class Source {

        @JsonProperty("group_id")
        private String groupId;

        @JsonProperty("option_id")
        private String optionId;

        @JsonProperty("strategy")
        private String strategy;

        @JsonProperty("similarity")
        private Double similarity;

        @JsonProperty("approval_status")
        private String approvalStatus;

        @JsonProperty("selection_id")
        private String selectionId;

        @JsonProperty("selected_at")
        private String selectedAt;
    }
}
