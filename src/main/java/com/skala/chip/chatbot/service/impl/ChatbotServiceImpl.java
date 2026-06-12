package com.skala.chip.chatbot.service.impl;

import com.skala.chip.chatbot.client.AgentChatRequest;
import com.skala.chip.chatbot.client.AgentChatResponse;
import com.skala.chip.chatbot.client.ChatbotAgentClient;
import com.skala.chip.chatbot.client.ChatbotAgentClient.ChatbotAgentException;
import com.skala.chip.chatbot.converter.ChatbotConverter;
import com.skala.chip.chatbot.dto.ChatbotRequestDTO;
import com.skala.chip.chatbot.dto.ChatbotResponseDTO;
import com.skala.chip.chatbot.service.ChatbotService;
import com.skala.chip.exception.code.ErrorCode;
import com.skala.chip.exception.custom.BusinessException;
import com.skala.chip.user.domain.User;
import com.skala.chip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * {@link ChatbotService} 구현.
 *
 * 흐름: JWT 인증 주체(email) → user_id 조회 → 에이전트 요청 변환 → /chat 호출 → 응답 변환.
 * 에이전트는 session_id 의 소유자를 user_id 로 검증(403)하므로, user_id 는 본문이 아닌
 * 인증 토큰에서 확정한 값을 주입한다.
 */
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatbotAgentClient chatbotAgentClient;
    private final UserRepository userRepository;

    @Override
    public ChatbotResponseDTO.MessageResult sendMessage(String email, ChatbotRequestDTO.SendMessage request) {
        // JWT 에는 email(sub)만 있고 user_id 는 없으므로 DB 에서 조회해 주입한다.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        AgentChatRequest agentRequest = ChatbotConverter.toAgentRequest(request, user.getUserId());

        AgentChatResponse agentResponse;
        try {
            agentResponse = chatbotAgentClient.chat(agentRequest);
        } catch (ChatbotAgentException e) {
            throw new BusinessException(mapAgentError(e.getStatus()));
        }

        return ChatbotConverter.toResponse(agentResponse);
    }

    /**
     * 에이전트 HTTP 상태코드 → 백엔드 ErrorCode 매핑.
     * 에이전트가 의도적으로 내려준 4xx(403/404/409/400/422)는 의미를 보존하고,
     * 그 외(연결 실패 status=0, 5xx)는 502(CHATBOT_AGENT_ERROR)로 묶는다.
     */
    private ErrorCode mapAgentError(int status) {
        return switch (status) {
            case 403 -> ErrorCode.CHATBOT_SESSION_FORBIDDEN;
            case 404 -> ErrorCode.CHATBOT_SESSION_NOT_FOUND;
            case 409 -> ErrorCode.CHATBOT_REF_TIME_REQUIRED;
            case 400, 422 -> ErrorCode.INVALID_INPUT;
            default -> ErrorCode.CHATBOT_AGENT_ERROR;
        };
    }
}
