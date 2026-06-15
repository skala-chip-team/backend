package com.skala.chip.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 챗봇 요청 DTO 모음.
 *
 * 프론트엔드 → 백엔드 요청 형태. user_id 는 받지 않는다 — JWT 인증 주체(email)에서 서버가 조회·주입.
 */
public class ChatbotRequestDTO {

    /**
     * 챗봇 메시지 전송 요청 DTO.
     *
     * - groupId   : 재조정 그룹 ID. 새 세션이면 필수, 기존 세션이면 선택(보내면 일치 검증).
     *               group/session 조합 규칙은 서비스에서 강제(없음+없음=400, 불일치=409).
     * - sessionId : 대화 세션 ID. 없으면 새 세션 생성, 있으면 해당 세션의 그룹을 컨텍스트로 사용.
     * - message   : 사용자 질문.
     *
     * ref_time 은 받지 않는다 — 시각은 AI 서버가 자체 시뮬 시계로 채운다.
     */
    @Getter
    @NoArgsConstructor
    public static class SendMessage {

        private String groupId;

        private String sessionId;

        @NotBlank
        private String message;
    }
}
