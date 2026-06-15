package com.skala.chip.chatbot.service.impl;

import com.skala.chip.chatbot.domain.ChatbotMessage;
import com.skala.chip.chatbot.domain.ChatbotSession;
import com.skala.chip.chatbot.repository.ChatbotMessageRepository;
import com.skala.chip.chatbot.repository.ChatbotSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 한 대화 턴의 영속화를 담당. (AI 추론 호출은 트랜잭션 밖에서 끝낸 뒤 여기서 저장)
 *
 * 신규 세션(있으면) + user/assistant 메시지를 <b>한 트랜잭션</b>으로 커밋해
 * 부분 저장(질문만 저장되고 답변 저장 실패)을 방지한다.
 */
@Component
@RequiredArgsConstructor
public class ChatbotTurnWriter {

    private final ChatbotSessionRepository chatbotSessionRepository;
    private final ChatbotMessageRepository chatbotMessageRepository;

    /**
     * @param newSession 신규 세션이면 저장할 엔티티, 기존 세션이면 null
     * @param sessionId  메시지를 매달 세션 ID
     * @param question   사용자 질문
     * @param answer     AI 답변
     */
    @Transactional
    public void writeTurn(ChatbotSession newSession, String sessionId, String question, String answer) {
        if (newSession != null) {
            chatbotSessionRepository.save(newSession);
        }
        LocalDateTime now = LocalDateTime.now();
        ChatbotMessage userMsg = ChatbotMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .messageType("user")
                .content(question)
                .createdAt(now)
                .build();
        ChatbotMessage assistantMsg = ChatbotMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sessionId(sessionId)
                .messageType("assistant")
                .content(answer)
                // user 보다 뒤로 정렬되도록 1ms 뒤 (created_at ASC 정렬 안정화)
                .createdAt(now.plusNanos(1_000_000))
                .build();
        chatbotMessageRepository.saveAll(List.of(userMsg, assistantMsg));
    }
}
