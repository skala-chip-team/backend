package com.skala.chip.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * user 테이블 엔티티.
 *
 * 테이블명을 "user"로 쿼트 처리한 이유:
 * PostgreSQL에서 user는 예약어이므로 식별자로 그대로 사용하면 SQL 오류가 발생한다.
 */
@Entity
@Table(name = "\"user\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @Column(name = "user_id")
    private String userId;

    /**
     * LAZY 로딩으로 설정한 이유:
     * 로그인 시 role_name만 필요하다면 UserRole 전체를 즉시 조회할 필요가 없다.
     * 필요한 시점에만 role을 로딩해 불필요한 쿼리를 줄인다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private UserRole role;

    @Column(name = "username")
    private String username;

    @Column(name = "email", unique = true)
    private String email;

    // 평문 비밀번호는 저장하지 않고, BCrypt 해시값만 DB에 보관한다.
    @Column(name = "password_hash")
    private String passwordHash;

    // false인 경우 로그인 자체를 차단한다. (ErrorCode.INACTIVE_USER)
    @Column(name = "is_active")
    private boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;
}
