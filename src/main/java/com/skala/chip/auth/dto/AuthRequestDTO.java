package com.skala.chip.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 요청 DTO 모음.
 * 중첩 클래스로 요청 유형별 DTO를 관리한다.
 */
public class AuthRequestDTO {

    /**
     * 로그인 요청 DTO.
     *
     * email 형식 검증은 서버에서도 수행한다.
     * 프론트 검증만으로는 악의적인 요청을 막을 수 없기 때문이다.
     */
    @Getter
    @NoArgsConstructor
    public static class LoginRequest {

        @NotBlank
        @Email
        private String email;

        // 평문 비밀번호는 서비스 레이어에서 BCrypt 해시값과 비교 후 즉시 폐기된다
        @NotBlank
        private String password;
    }

    @Getter
    @NoArgsConstructor
    public static class SignUpRequest {

        @NotBlank
        private String username;

        @NotBlank
        @Email
        private String email;

        @NotBlank
        @Size(min = 8)
        private String password;
    }
}
