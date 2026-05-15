package com.skala.chip.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skala.chip.common.ApiResponse;
import com.skala.chip.exception.code.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증되지 않은 요청(401)에 대한 응답 처리기.
 *
 * Spring Security가 인증 실패를 감지하면 이 EntryPoint를 호출한다.
 * GlobalExceptionHandler를 거치지 않는 이유:
 * 필터 레벨에서 발생한 인증 실패는 Spring MVC DispatcherServlet에 도달하지 않으므로
 * @ExceptionHandler가 동작하지 않는다. 따라서 직접 HttpServletResponse에 JSON을 작성한다.
 *
 * 응답 케이스:
 * - TOKEN_EXPIRED  → 만료된 토큰 (재로그인 안내)
 * - INVALID_TOKEN  → 위변조/형식 오류 토큰
 * - UNAUTHORIZED   → Authorization 헤더 자체가 없는 경우
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    /**
     * JwtAuthenticationFilter가 request attribute에 저장한 실패 원인을 읽어
     * 만료/위변조/미인증을 구분한 ApiResponse를 응답한다.
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String jwtError = (String) request.getAttribute(JwtAuthenticationFilter.JWT_ERROR_ATTRIBUTE);

        ErrorCode errorCode;
        if ("TOKEN_EXPIRED".equals(jwtError)) {
            errorCode = ErrorCode.TOKEN_EXPIRED;
        } else if ("INVALID_TOKEN".equals(jwtError)) {
            errorCode = ErrorCode.INVALID_TOKEN;
        } else {
            // Authorization 헤더 자체가 없는 경우
            errorCode = ErrorCode.UNAUTHORIZED;
        }

        writeErrorResponse(response, errorCode);
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getCode());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.fail(errorCode))
        );
    }
}
