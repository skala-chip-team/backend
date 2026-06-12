package com.skala.chip.chatbot.converter;

import com.skala.chip.chatbot.client.AgentChatRequest;
import com.skala.chip.chatbot.client.AgentChatResponse;
import com.skala.chip.chatbot.domain.ChatbotMessage;
import com.skala.chip.chatbot.dto.ChatbotRequestDTO;
import com.skala.chip.chatbot.dto.ChatbotResponseDTO;
import com.skala.chip.chatbot.repository.ChatbotSessionSummaryProjection;

/**
 * 챗봇 DTO ↔ 에이전트 통신 모델 변환기.
 *
 * 변환 로직은 서비스가 아닌 Converter 에 둔다(코드 컨벤션). 정적 메서드만 제공한다.
 */
public class ChatbotConverter {

    private ChatbotConverter() {
    }

    /**
     * 프론트 요청 + (JWT 에서 조회한) user_id 를 에이전트 요청으로 변환한다.
     * user_id 는 본문이 아닌 인증 주체에서 주입한다.
     */
    public static AgentChatRequest toAgentRequest(ChatbotRequestDTO.SendMessage request, String userId) {
        return AgentChatRequest.builder()
                .groupId(request.getGroupId())
                .userId(userId)
                .sessionId(request.getSessionId())
                .message(request.getMessage())
                .refTime(request.getRefTime())
                .build();
    }

    /** 에이전트 응답(snake_case)을 프론트 응답(camelCase)으로 변환한다. */
    public static ChatbotResponseDTO.MessageResult toResponse(AgentChatResponse response) {
        return ChatbotResponseDTO.MessageResult.builder()
                .sessionId(response.getSessionId())
                .answer(response.getAnswer())
                .toolCalls(response.getToolCalls())
                .build();
    }

    /** 세션 목록 프로젝션 → 세션 요약 DTO. */
    public static ChatbotResponseDTO.SessionSummary toSessionSummary(ChatbotSessionSummaryProjection p) {
        return ChatbotResponseDTO.SessionSummary.builder()
                .sessionId(p.getSessionId())
                .startedAt(p.getStartedAt())
                .endedAt(p.getEndedAt())
                .messageCount(p.getMessageCount())
                .build();
    }

    /** 메시지 엔티티 → 메시지 DTO. */
    public static ChatbotResponseDTO.MessageDetail toMessageDetail(ChatbotMessage m) {
        return ChatbotResponseDTO.MessageDetail.builder()
                .messageId(m.getMessageId())
                .messageType(m.getMessageType())
                .content(m.getContent())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
