package com.skala.chip.auth.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 인증 응답 DTO 모음.
 * 중첩 클래스로 응답 유형별 DTO를 관리한다.
 */
public class AuthResponseDTO {

    /**
     * 로그인 성공 응답 DTO.
     *
     * 클라이언트는 이후 요청 시 Authorization 헤더에
     * "Bearer {accessToken}" 형식으로 토큰을 포함해야 한다.
     * tokenType 필드는 클라이언트가 헤더를 자동 구성할 수 있도록 함께 내려준다.
     */
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class LoginResponse {

        private String accessToken;

        // 항상 "Bearer" 고정값. Authorization 헤더 prefix로 사용된다.
        private String tokenType;

        private String username;

        // user_role.role_name 값. 클라이언트가 권한별 UI를 분기하는 데 사용된다.
        private String role;
    }
}
