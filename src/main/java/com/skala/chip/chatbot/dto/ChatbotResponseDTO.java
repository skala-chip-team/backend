package com.skala.chip.chatbot.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 챗봇 응답 DTO 모음. (백엔드 → 프론트엔드, ApiResponse.data 에 담긴다)
 */
public class ChatbotResponseDTO {

    /**
     * 챗봇 메시지 전송 결과 DTO.
     *
     * - sessionId : (신규/기존) 대화 세션 ID. 프론트는 다음 요청에 이 값을 넣어 대화를 이어간다.
     * - answer    : AI가 생성한 답변(마크다운).
     * - toolCalls : 답변 생성 과정에서 호출된 도구 이름 목록.
     * - sources   : 과거 사례 검색 출처(구조화). 화면에 근거로 노출.
     */
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MessageResult {

        private String sessionId;

        private String answer;

        private List<String> toolCalls;

        private List<Source> sources;
    }

    /** 과거 사례 검색 출처. */
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Source {

        private String groupId;

        private String optionId;

        private String strategy;

        private Double similarity;

        private String approvalStatus;

        private String selectionId;

        private String selectedAt;
    }

    /**
     * 세션 목록 항목 DTO. (GET /api/chatbot/sessions)
     *
     * - messageCount : 해당 세션의 메시지 수 (user + assistant 합)
     */
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class SessionSummary {

        private String sessionId;

        private LocalDateTime startedAt;

        private LocalDateTime endedAt;

        private long messageCount;
    }

    /**
     * 세션 내 단일 메시지 DTO. (GET /api/chatbot/sessions/{sessionId}/messages)
     *
     * - messageType : "user" | "assistant"
     */
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MessageDetail {

        private String messageId;

        private String messageType;

        private String content;

        private LocalDateTime createdAt;
    }
}
