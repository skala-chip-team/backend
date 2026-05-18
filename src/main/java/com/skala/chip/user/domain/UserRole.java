package com.skala.chip.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * user_role 테이블 엔티티.
 *
 * 역할을 enum이 아닌 별도 테이블로 관리하는 이유:
 * 운영 중에 role_name, description을 코드 변경 없이 DB에서 수정할 수 있다.
 */
@Entity
@Table(name = "user_role")
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRole {

    @Id
    @Column(name = "role_id")
    private String roleId;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "description")
    private String description;
}
