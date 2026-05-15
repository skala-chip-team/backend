package com.skala.chip.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.chip.auth.dto.AuthResponseDTO;
import com.skala.chip.auth.service.AuthService;
import com.skala.chip.exception.custom.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 웹 레이어 테스트.
 *
 * Security를 제외하는 이유:
 * @WebMvcTest에 SecurityConfig가 로드되면 JwtAuthenticationFilter가 개입해
 * 컨트롤러 로직 검증 전에 응답이 바뀌는 부작용이 생긴다.
 * 컨트롤러 단위 테스트에서는 Security 동작을 배제하고,
 * 입력값 검증(@Valid) / 응답 구조(ApiResponse) / 예외 응답만 검증한다.
 */
@WebMvcTest(
        value = AuthController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class}
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("로그인 성공 - 유효한 요청이면 200과 LoginResponse를 반환한다")
    void 로그인_성공_200() throws Exception {
        // given
        AuthResponseDTO.LoginResponse loginResponse = AuthResponseDTO.LoginResponse.builder()
                .accessToken("mocked.jwt.token")
                .tokenType("Bearer")
                .username("testUser")
                .role("USER")
                .build();

        when(authService.login(any())).thenReturn(loginResponse);

        Map<String, String> request = Map.of(
                "email", "test@test.com",
                "password", "password123"
        );

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.username").value("testUser"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("이메일 형식 오류 - @Email 검증 실패 시 400을 반환한다")
    void 이메일_형식_오류_400() throws Exception {
        // given: @Email 검증 실패 → GlobalExceptionHandler → INVALID_INPUT(400)
        Map<String, String> request = Map.of(
                "email", "not-an-email",
                "password", "password123"
        );

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("비밀번호 누락 - @NotBlank 검증 실패 시 400을 반환한다")
    void 비밀번호_누락_400() throws Exception {
        // given
        Map<String, String> request = Map.of("email", "test@test.com");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("인증 실패 - 이메일/비밀번호 불일치 시 401을 반환한다")
    void 인증_실패_401() throws Exception {
        // given: Service에서 InvalidCredentialsException → GlobalExceptionHandler → 401
        when(authService.login(any())).thenThrow(new InvalidCredentialsException());

        Map<String, String> request = Map.of(
                "email", "wrong@test.com",
                "password", "wrongPassword"
        );

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401));
    }
}
