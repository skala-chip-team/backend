package com.skala.chip.chatbot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 챗봇 대화 메시지(한 turn).
 *
 * 백엔드가 user/assistant 메시지를 한 트랜잭션으로 저장한다. (AI 서버는 저장하지 않음)
 */
@Entity
@Table(name = "TD_CHATBOT_MESSAGE")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
