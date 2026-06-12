package com.skala.chip.chatbot.repository;

import java.time.LocalDateTime;

/**
 * 세션 목록 조회용 프로젝션. (세션 메타 + 메시지 수)
 */
public interface ChatbotSessionSummaryProjection {

    String getSessionId();

    LocalDateTime getStartedAt();

    LocalDateTime getEndedAt();

    long getMessageCount();
}
