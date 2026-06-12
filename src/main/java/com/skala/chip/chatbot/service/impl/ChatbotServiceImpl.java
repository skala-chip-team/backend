package com.skala.chip.chatbot.service.impl;

import com.skala.chip.chatbot.client.AgentChatRequest;
import com.skala.chip.chatbot.client.AgentChatResponse;
import com.skala.chip.chatbot.client.ChatbotAgentClient;
import com.skala.chip.chatbot.client.ChatbotAgentClient.ChatbotAgentException;
import com.skala.chip.chatbot.converter.ChatbotConverter;
import com.skala.chip.chatbot.domain.ChatbotSession;
import com.skala.chip.chatbot.dto.ChatbotRequestDTO;
import com.skala.chip.chatbot.dto.ChatbotResponseDTO;
import com.skala.chip.chatbot.repository.ChatbotMessageRepository;
import com.skala.chip.chatbot.repository.ChatbotSessionRepository;
import com.skala.chip.chatbot.service.ChatbotService;
import com.skala.chip.exception.code.ErrorCode;
import com.skala.chip.exception.custom.BusinessException;
import com.skala.chip.user.domain.User;
import com.skala.chip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link ChatbotService} 구현.
 *
 * - 메시지 전송: JWT 주체(email) → user_id 조회 → 에이전트 /chat 호출 → 응답 변환.
 *   에이전트는 session_id 의 소유자를 user_id 로 검증(403)하므로, user_id 는 본문이 아닌
 *   인증 토큰에서 확정한 값을 주입한다.
 * - 이력 조회: 공유 DB(tt_chatbot_session / td_chatbot_message)를 직접 읽는다.
 */
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatbotAgentClient chatbotAgentClient;
    private final ChatbotSessionRepository chatbotSessionRepository;
    private final ChatbotMessageRepository chatbotMessageRepository;
    private final UserRepository userRepository;

    @Override
    public ChatbotResponseDTO.MessageResult sendMessage(String email, ChatbotRequestDTO.SendMessage request) {
        String userId = resolveUserId(email);

        AgentChatRequest agentRequest = ChatbotConverter.toAgentRequest(request, userId);

        AgentChatResponse agentResponse;
        try {
            agentResponse = chatbotAgentClient.chat(agentRequest);
        } catch (ChatbotAgentException e) {
            throw new BusinessException(mapAgentError(e.getStatus()));
        }

        return ChatbotConverter.toResponse(agentResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatbotResponseDTO.SessionSummary> getSessions(String email) {
        String userId = resolveUserId(email);
        return chatbotSessionRepository.findSummariesByUserId(userId).stream()
                .map(ChatbotConverter::toSessionSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatbotResponseDTO.MessageDetail> getMessages(String email, String sessionId) {
        String userId = resolveUserId(email);

        ChatbotSession session = chatbotSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHATBOT_SESSION_NOT_FOUND));
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.CHATBOT_SESSION_FORBIDDEN);
        }

        return chatbotMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(ChatbotConverter::toMessageDetail)
                .toList();
    }

    /** JWT 주체(email) → user_id. JWT 에는 email(sub)만 있으므로 DB 에서 조회한다. */
    private String resolveUserId(String email) {
        return userRepository.findByEmail(email)
                .map(User::getUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
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
