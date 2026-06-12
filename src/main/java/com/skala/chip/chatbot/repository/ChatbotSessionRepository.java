package com.skala.chip.chatbot.repository;

import com.skala.chip.chatbot.domain.ChatbotSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatbotSessionRepository extends JpaRepository<ChatbotSession, String> {

    /**
     * 특정 사용자의 세션 목록(최신순) + 세션별 메시지 수.
     * 메시지 수는 상관 서브쿼리로 한 번에 계산해 N+1 을 피한다.
     */
    @Query("SELECT s.sessionId AS sessionId, s.startedAt AS startedAt, s.endedAt AS endedAt, "
            + "(SELECT COUNT(m) FROM ChatbotMessage m WHERE m.sessionId = s.sessionId) AS messageCount "
            + "FROM ChatbotSession s WHERE s.userId = :userId ORDER BY s.startedAt DESC")
    List<ChatbotSessionSummaryProjection> findSummariesByUserId(@Param("userId") String userId);
}
