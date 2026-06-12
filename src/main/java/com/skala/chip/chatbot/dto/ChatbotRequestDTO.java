package com.skala.chip.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 챗봇 요청 DTO 모음.
 *
 * 프론트엔드 → 백엔드 요청 형태를 정의한다.
 * user_id 는 요청 본문에 받지 않는다 — JWT 인증 주체(email)로부터 서버에서 조회해 주입한다.
 * (요청 본문의 user_id 를 신뢰하면 다른 사용자의 세션을 가로챌 수 있어 보안상 위험하다.)
 */
public class ChatbotRequestDTO {

    /**
     * 챗봇 메시지 전송 요청 DTO.
     *
     * - groupId   : 재조정 그룹 ID. 챗봇 답변의 컨텍스트 범위를 결정한다. (에이전트가 group_id 로 컨텍스트 로드)
     * - sessionId : 대화 세션 ID. null/생략 시 에이전트가 새 세션을 생성해 반환한다.
     * - message   : 사용자 질문.
     * - refTime   : 기준 시각(ISO-8601). null 이면 에이전트가 현재 sim 시각을 사용한다.
     *               시뮬레이션 데이터는 과거 시점 기준이라 이 값으로 시간 계산 기준을 맞춘다.
     */
    @Getter
    @NoArgsConstructor
    public static class SendMessage {

        @NotBlank
        private String groupId;

        private String sessionId;

        @NotBlank
        private String message;

        private String refTime;
    }
}
