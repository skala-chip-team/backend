package com.skala.chip.auth.service.impl;

import com.skala.chip.auth.dto.AuthRequestDTO;
import com.skala.chip.auth.dto.AuthResponseDTO;
import com.skala.chip.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 인증 비즈니스 로직 구현체.
 *
 * 로그인 처리 흐름 (Commit 4에서 완성):
 * 1. 이메일로 User 조회 → 없으면 USER_NOT_FOUND 예외
 * 2. is_active 확인 → 비활성 계정이면 INACTIVE_USER 예외
 * 3. BCrypt로 비밀번호 검증 → 불일치 시 INVALID_CREDENTIALS 예외
 * 4. JwtProvider로 Access Token 발급
 * 5. LoginResponse 반환
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    // TODO: Commit 4 - UserRepository, PasswordEncoder, JwtProvider 주입 예정

    /**
     * @param request 이메일 + 평문 비밀번호
     * @return AccessToken, tokenType("Bearer"), username, role 포함 LoginResponse
     */
    @Override
    public AuthResponseDTO.LoginResponse login(AuthRequestDTO.LoginRequest request) {
        // TODO: Commit 4 - 실제 인증 로직 구현
        throw new UnsupportedOperationException("구현 예정");
    }
}
