package com.skala.chip.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.chip.common.ApiResponse;
import com.skala.chip.exception.code.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증은 됐지만 권한이 부족한 요청(403)에 대한 응답 처리기.
 *
 * AuthenticationEntryPoint와의 차이:
 * - EntryPoint  : 인증 자체가 없는 경우 (토큰 없음, 만료, 위변조) → 401
 * - AccessDeniedHandler : 인증은 됐지만 해당 리소스에 대한 권한이 없는 경우 → 403
 *   예) ROLE_USER 토큰으로 ROLE_ADMIN 전용 API 호출
 */
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail(ErrorCode.FORBIDDEN))
        );
    }
}
