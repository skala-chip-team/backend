package com.skala.chip.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 기반 인증 필터.
 *
 * OncePerRequestFilter를 상속하는 이유:
 * 서블릿 필터는 한 요청에서 여러 번 호출될 수 있는데(forward/include 등),
 * OncePerRequestFilter는 요청당 정확히 한 번만 실행을 보장한다.
 *
 * 필터 실행 순서 (SecurityConfig에서 설정):
 * 이 필터 → UsernamePasswordAuthenticationFilter → ...
 * UsernamePasswordAuthenticationFilter보다 먼저 실행되어야
 * JWT 인증 정보가 이미 SecurityContext에 등록된 상태로 이후 필터가 동작한다.
 *
 * 인증 흐름:
 * 요청 → Authorization 헤더 파싱 → 토큰 검증 → SecurityContext 등록 → 다음 필터
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    // JwtAuthenticationEntryPoint에서 원인을 읽을 수 있도록 public 상수로 선언
    public static final String JWT_ERROR_ATTRIBUTE = "jwt_error";

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 요청마다 JWT를 검증하고 유효하면 SecurityContext에 인증 정보를 등록한다.
     *
     * 토큰이 없거나 유효하지 않은 경우 SecurityContext를 비워두고 다음 필터로 넘긴다.
     * 이후 SecurityConfig의 authorizeHttpRequests에서 인증 필요 경로면 403을 응답한다.
     * 만료/위변조 토큰에 대한 세부 응답 처리는 Commit 6에서 추가된다.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Authorization 헤더에서 Bearer 토큰 추출
        String token = extractToken(request);

        // 2. 토큰 검증 및 SecurityContext 등록
        //    유효한 토큰이면 인증 정보를 등록하고, 실패 시 원인을 attribute에 저장한다.
        //    JwtAuthenticationEntryPoint가 attribute를 읽어 만료/위변조를 구분한 응답을 반환한다.
        if (token != null && jwtProvider.validateToken(token)) {

            // 3. 토큰 Claim에서 사용자 식별 정보 추출
            String email = jwtProvider.getEmail(token);
            String role = jwtProvider.getRole(token);

            // 4. ROLE_ prefix 추가
            // Spring Security의 hasRole("ADMIN")은 내부적으로 "ROLE_ADMIN"을 기대한다.
            // JWT claim의 role_name(예: "ADMIN")에 prefix를 붙여 권한 형식을 맞춘다.
            List<SimpleGrantedAuthority> authorities =
                    List.of(new SimpleGrantedAuthority("ROLE_" + role));

            // 5. SecurityContext에 인증 토큰 등록
            // credentials(두 번째 인자)를 null로 설정하는 이유:
            // 이미 JWT로 인증이 완료된 상태이므로 비밀번호를 다시 보관할 필요가 없다.
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);

            // 요청의 IP, 세션 정보를 인증 객체에 추가 (감사 로그 등에 활용 가능)
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } else if (token != null) {
            // 토큰이 있지만 유효하지 않은 경우: 만료/위변조 원인을 구분해 저장
            // EntryPoint가 이 attribute를 읽어 클라이언트에 맞는 메시지를 반환한다.
            if (jwtProvider.isTokenExpired(token)) {
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "TOKEN_EXPIRED");
            } else {
                request.setAttribute(JWT_ERROR_ATTRIBUTE, "INVALID_TOKEN");
            }
        }

        // 6. 인증 성공 여부와 무관하게 다음 필터로 전달
        //    인증이 필요한 경로에서 Context가 비어있으면 Spring Security가 401/403 처리
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 토큰 문자열만 추출.
     *
     * "Bearer {token}" 형식에서 "Bearer " prefix를 제거한다.
     * 헤더가 없거나 형식이 다른 경우 null을 반환해 인증을 건너뛴다.
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
