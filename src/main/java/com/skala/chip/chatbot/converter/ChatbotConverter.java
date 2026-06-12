package com.skala.chip.chatbot.converter;

import com.skala.chip.chatbot.client.AgentChatRequest;
import com.skala.chip.chatbot.client.AgentChatResponse;
import com.skala.chip.chatbot.dto.ChatbotRequestDTO;
import com.skala.chip.chatbot.dto.ChatbotResponseDTO;

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
}
