package com.skala.chip.auth.service;

import com.skala.chip.auth.dto.AuthRequestDTO;
import com.skala.chip.auth.dto.AuthResponseDTO;

/**
 * 인증 비즈니스 로직 인터페이스.
 *
 * Controller는 이 인터페이스에만 의존하며,
 * 실제 구현체(AuthServiceImpl)는 Spring이 주입한다.
 */
public interface AuthService {

    /**
     * 이메일/비밀번호 기반 로그인을 처리하고 JWT를 발급한다.
     *
     * @param request 로그인 요청 DTO (email, password)
     * @return JWT Access Token과 사용자 정보가 담긴 LoginResponse
     */
    AuthResponseDTO.LoginResponse login(AuthRequestDTO.LoginRequest request);
}
