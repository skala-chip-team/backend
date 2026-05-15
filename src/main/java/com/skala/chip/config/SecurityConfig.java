package com.skala.chip.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 기본 설정.
 *
 * JWT 기반 인증은 서버가 세션을 유지하지 않으므로 Stateless 방식으로 구성한다.
 * JWT Filter는 Commit 5에서 SecurityFilterChain에 추가될 예정이다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * HTTP 보안 정책 설정.
     *
     * - CSRF: REST API는 쿠키 기반 인증을 사용하지 않으므로 비활성화
     * - Session: JWT를 사용하므로 서버 세션을 생성하지 않음 (STATELESS)
     * - 인증 없이 접근 가능한 경로: /, /health, /api/auth/**
     *   (로그인 전에 호출되는 엔드포인트는 permitAll 처리)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/health", "/api/auth/**").permitAll()
                .anyRequest().authenticated()
            );

        // TODO: Commit 5 - JwtAuthenticationFilter를 UsernamePasswordAuthenticationFilter 앞에 추가
        return http.build();
    }

    /**
     * 비밀번호 단방향 암호화에 BCrypt 사용.
     * DB에는 password_hash 컬럼에 BCrypt 해시값만 저장되므로
     * 로그인 시 평문 비밀번호와 해시를 비교할 때 이 Bean이 필요하다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
