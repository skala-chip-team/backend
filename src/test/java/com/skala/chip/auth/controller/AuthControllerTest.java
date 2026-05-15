package com.skala.chip.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.chip.auth.dto.AuthResponseDTO;
import com.skala.chip.auth.jwt.JwtAccessDeniedHandler;
import com.skala.chip.auth.jwt.JwtAuthenticationEntryPoint;
import com.skala.chip.auth.jwt.JwtProvider;
import com.skala.chip.auth.service.AuthService;
import com.skala.chip.exception.custom.InvalidCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
 * @AutoConfigureMockMvc(addFilters = false)를 사용하는 이유:
 * SecurityConfig(@EnableWebSecurity)는 @WebMvcTest에서 항상 로드된다.
 * addFilters = false로 MockMvc에 서블릿 필터를 적용하지 않아
 * JwtAuthenticationFilter가 요청을 가로채지 않고 컨트롤러까지 도달한다.
 * SecurityConfig 자체는 로드되므로 의존 Bean(@MockBean)은 여전히 필요하다.
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    // SecurityConfig → JwtAuthenticationFilter → JwtProvider 의존 체인 해소
    @MockBean
    private JwtProvider jwtProvider;

    // SecurityConfig에서 직접 주입받는 Bean들
    @MockBean
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @MockBean
    private JwtAccessDeniedHandler jwtAccessDeniedHandler;

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
