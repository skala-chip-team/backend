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
 * 챗봇 대화 메시지(한 turn).
 *
 * 챗봇 에이전트(ai_agent)가 /chat 처리 중에 user/assistant 메시지를 각각 저장한다.
 * 백엔드는 화면 이력 조회를 위해 <b>읽기만</b> 한다.
 */
@Entity
@Table(name = "TD_CHATBOT_MESSAGE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatbotMessage {

    @Id
    @Column(name = "message_id")
    private String messageId;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    // "user" | "assistant"
    @Column(name = "message_type")
    private String messageType;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
