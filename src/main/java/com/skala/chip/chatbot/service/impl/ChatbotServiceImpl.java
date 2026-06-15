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
import com.skala.chip.reschedule.domain.RescheduleGroup;
import com.skala.chip.reschedule.repository.RescheduleGroupRepository;
import com.skala.chip.user.domain.User;
import com.skala.chip.user.repository.UserDistrictMapRepository;
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
    private final UserDistrictMapRepository userDistrictMapRepository;
    private final RescheduleGroupRepository rescheduleGroupRepository;

    private static final String ROLE_ADMIN = "ADMIN";

    @Override
    public ChatbotResponseDTO.MessageResult sendMessage(String email, ChatbotRequestDTO.SendMessage request) {
        User user = resolveUser(email);
        String userId = user.getUserId();

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

        // 구역 기반 접근 권한: 운영자는 담당 구역의 재조정 그룹에만 챗봇으로 접근할 수 있다.
        // (세션 소유권과 별개 — 권한 없는 group_id 로 새 세션을 여는 것을 막는다. ADMIN 은 전체 허용)
        assertGroupAccess(user, groupId);

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
        return resolveUser(email).getUserId();
    }

    /** JWT 주체(email) → User (역할·구역 권한 판단에 필요). */
    private User resolveUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 구역 기반 그룹 접근 권한 검증. 그룹의 구역(district)이 사용자 담당 구역이 아니면 403.
     * - ADMIN 역할은 전체 구역 접근 허용
     * - 그룹이 존재하지 않으면(잘못된 group_id) 권한 검증을 생략한다(존재/유효성은 별도 책임).
     */
    private void assertGroupAccess(User user, String groupId) {
        if (user.getRole() != null && ROLE_ADMIN.equalsIgnoreCase(user.getRole().getRoleName())) {
            return; // 관리자: 전체 구역
        }
        String districtId = rescheduleGroupRepository.findById(groupId)
                .map(RescheduleGroup::getDistrictId)
                .orElse(null);
        if (districtId == null) {
            return; // 그룹 미존재 → 구역 판별 불가, 권한 검증 생략
        }
        boolean allowed = userDistrictMapRepository.findByUserId(user.getUserId()).stream()
                .anyMatch(m -> districtId.equals(m.getDistrictId()));
        if (!allowed) {
            throw new BusinessException(ErrorCode.CHATBOT_GROUP_FORBIDDEN);
        }
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
