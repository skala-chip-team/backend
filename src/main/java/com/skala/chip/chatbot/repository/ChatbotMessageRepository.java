package com.skala.chip.chatbot.repository;

import com.skala.chip.chatbot.domain.ChatbotMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatbotMessageRepository extends JpaRepository<ChatbotMessage, String> {

    /** 세션의 메시지를 시간순으로 조회한다. */
    List<ChatbotMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
