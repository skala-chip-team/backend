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
 * 챗봇 대화 세션.
 *
 * 세션 lifecycle 은 백엔드가 소유한다 — 백엔드가 session_id(UUID)를 발급하고
 * 하나의 재조정 그룹(group_id)에 고정해 생성한다. (AI 서버는 더 이상 세션을 만들지 않음)
 */
@Entity
@Table(name = "TT_CHATBOT_SESSION")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatbotSession {

    @Id
    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    // 세션의 컨텍스트 그룹 — 세션 내에서 고정. 백엔드가 세션 생성 시 채운다.
    // (기존 행이 있어 DB 는 nullable; 신규 세션은 항상 채움 → 추후 not null 강화 가능)
    @Column(name = "group_id")
    private String groupId;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
