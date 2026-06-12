package com.skala.chip.chatbot.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 챗봇 응답 DTO 모음.
 *
 * 백엔드 → 프론트엔드 응답 형태를 정의한다. (ApiResponse.data 에 담긴다)
 * 에이전트(ai_agent /chat)의 snake_case 응답을 백엔드 표준 camelCase 로 변환한 값이다.
 */
public class ChatbotResponseDTO {

    /**
     * 챗봇 메시지 전송 결과 DTO.
     *
     * - sessionId : (신규/기존) 대화 세션 ID. 프론트는 다음 요청에 이 값을 넣어 대화를 이어간다.
     * - answer    : 에이전트가 생성한 답변(마크다운).
     * - toolCalls : 답변 생성 과정에서 호출된 도구 이름 목록. (예: get_unit_status, get_step_queue)
     */
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class MessageResult {

        private String sessionId;

        private String answer;

        private List<String> toolCalls;
    }
}
