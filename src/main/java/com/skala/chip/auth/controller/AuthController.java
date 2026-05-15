package com.skala.chip.auth.controller;

import com.skala.chip.auth.dto.AuthRequestDTO;
import com.skala.chip.auth.dto.AuthResponseDTO;
import com.skala.chip.auth.service.AuthService;
import com.skala.chip.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 관련 요청을 처리하는 컨트롤러.
 *
 * SecurityConfig에서 /api/auth/** 는 인증 없이 접근 가능하도록 허용되어 있다.
 * Controller에서는 입력값 검증(@Valid)만 수행하고,
 * 인증 비즈니스 로직은 AuthService에 위임한다.
 */
@Tag(name = "Auth", description = "인증 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 로그인 요청을 처리하고 JWT Access Token을 반환한다.
     *
     * @param request 이메일 + 비밀번호 (AuthRequestDTO.LoginRequest)
     * @return AccessToken, tokenType, username, role을 담은 LoginResponse
     *
     * 인증 흐름:
     * 1. @Valid로 입력값 형식 검증 (email 형식, notBlank)
     * 2. AuthService.login()에서 사용자 조회 → 비밀번호 검증 → JWT 발급 (Commit 4)
     * 3. 발급된 JWT를 ApiResponse로 감싸서 반환
     */
    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDTO.LoginResponse>> login(
            @Valid @RequestBody AuthRequestDTO.LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AuthResponseDTO.SignUpResponse>> signUp(
            @Valid @RequestBody AuthRequestDTO.SignUpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(authService.signUp(request)));
    }
}
