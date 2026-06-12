package com.skala.chip.chatbot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 챗봇 대화 세션.
 *
 * 이 테이블은 챗봇 에이전트(ai_agent)가 /chat 처리 중에 생성·갱신한다.
 * 백엔드는 화면 이력 조회를 위해 <b>읽기만</b> 한다 (쓰기 없음 → setter/builder 미제공).
 */
@Entity
@Table(name = "TT_CHATBOT_SESSION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatbotSession {

    @Id
    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
