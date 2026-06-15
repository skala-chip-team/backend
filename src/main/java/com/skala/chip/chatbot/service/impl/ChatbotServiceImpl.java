package com.skala.chip.chatbot.service.impl;

import com.skala.chip.chatbot.client.ChatbotAgentClient;
import com.skala.chip.chatbot.client.ChatbotAgentClient.ChatbotAgentException;
import com.skala.chip.chatbot.client.InferResponse;
import com.skala.chip.chatbot.converter.ChatbotConverter;
import com.skala.chip.chatbot.domain.ChatbotMessage;
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
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * {@link ChatbotService} 구현.
 *
 * 세션 lifecycle·소유권·메시지 저장을 백엔드가 소유한다. AI 서버(/infer)는 무상태 추론만.
 *
 * sendMessage 흐름:
 *   JWT 주체(email) → user_id 조회
 *   → 세션 확인/생성 + 그룹 연결 규칙 강제
 *   → 이력 조회 → AI /infer 호출(트랜잭션 밖)
 *   → user + assistant 메시지 한 트랜잭션 저장({@link ChatbotTurnWriter})
 */
@Service
@RequiredArgsConstructor
public class ChatbotServiceImpl implements ChatbotService {

    private final ChatbotAgentClient chatbotAgentClient;
    private final ChatbotTurnWriter chatbotTurnWriter;
    private final ChatbotSessionRepository chatbotSessionRepository;
    private final ChatbotMessageRepository chatbotMessageRepository;
    private final UserRepository userRepository;

    @Override
    public ChatbotResponseDTO.MessageResult sendMessage(String email, ChatbotRequestDTO.SendMessage request) {
        String userId = resolveUserId(email);

        ChatbotSession newSession = null;  // 신규 세션일 때만 채워짐(트랜잭션에서 함께 저장)
        String sessionId;
        String groupId;
        List<ChatbotMessage> history;

        if (StringUtils.hasText(request.getSessionId())) {
            // 기존 세션 — 소유권 + 그룹 일치 검증
            ChatbotSession session = chatbotSessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.CHATBOT_SESSION_NOT_FOUND));
            if (!userId.equals(session.getUserId())) {
                throw new BusinessException(ErrorCode.CHATBOT_SESSION_FORBIDDEN);
            }
            if (StringUtils.hasText(request.getGroupId())
                    && !request.getGroupId().equals(session.getGroupId())) {
                throw new BusinessException(ErrorCode.CHATBOT_GROUP_MISMATCH);
            }
            sessionId = session.getSessionId();
            groupId = session.getGroupId();
            history = chatbotMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        } else {
            // 새 세션 — group_id 필수
            if (!StringUtils.hasText(request.getGroupId())) {
                throw new BusinessException(ErrorCode.CHATBOT_GROUP_REQUIRED);
            }
            sessionId = UUID.randomUUID().toString();
            groupId = request.getGroupId();
            newSession = ChatbotSession.builder()
                    .sessionId(sessionId)
                    .userId(userId)
                    .groupId(groupId)
                    .startedAt(LocalDateTime.now())
                    .build();
            history = List.of();
        }

        // AI 추론 (트랜잭션 밖 — LLM 호출이 길어 DB 트랜잭션을 잡지 않는다)
        InferResponse inferResponse;
        try {
            inferResponse = chatbotAgentClient.infer(
                    ChatbotConverter.toInferRequest(groupId, request.getMessage(), history));
        } catch (ChatbotAgentException e) {
            throw new BusinessException(mapAgentError(e.getStatus()));
        }

        // user + assistant 메시지(+ 신규 세션) 한 트랜잭션 저장
        chatbotTurnWriter.writeTurn(newSession, sessionId, request.getMessage(), inferResponse.getAnswer());

        return ChatbotConverter.toResponse(sessionId, inferResponse);
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
        if (!userId.equals(session.getUserId())) {
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
     * AI 추론(/infer) HTTP 상태코드 → ErrorCode 매핑.
     * /infer 는 무상태라 세션 관련 4xx(403/404/409)를 내지 않는다.
     * 입력 오류(400/422)만 그대로 전달하고, 그 외(연결 실패 status=0, 5xx)는 502 로 묶는다.
     */
    private ErrorCode mapAgentError(int status) {
        return switch (status) {
            case 400, 422 -> ErrorCode.INVALID_INPUT;
            default -> ErrorCode.CHATBOT_AGENT_ERROR;
        };
    }
}
