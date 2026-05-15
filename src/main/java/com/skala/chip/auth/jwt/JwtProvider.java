package com.skala.chip.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT Access Token 생성 / 검증 / 파싱 유틸리티.
 *
 * 전체 인증 흐름에서 이 클래스의 위치:
 * 1. 로그인 성공 → generateToken()으로 토큰 발급 → 클라이언트에 반환  (Commit 4)
 * 2. 이후 모든 요청 → 클라이언트가 "Authorization: Bearer {token}" 헤더 전송
 * 3. JwtAuthenticationFilter(Commit 5)에서 validateToken() 검증
 *    → 통과 시 getEmail(), getRole()로 사용자 정보 추출 → SecurityContext 등록
 */
@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expiration;

    /**
     * 생성자 주입으로 secret을 SecretKey 객체로 변환.
     *
     * 매 요청마다 키를 생성하면 성능 낭비이므로 Bean 초기화 시 한 번만 생성한다.
     * HS256은 최소 32바이트 키가 필요하다. (JWT_SECRET 환경변수로 주입)
     */
    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    /**
     * Access Token 생성.
     *
     * Claim 구조:
     * - sub  : 토큰 주체 (email). 사용자를 DB 없이 식별하는 고유값
     * - role : 권한 정보. JWT Filter에서 꺼내 SecurityContext에 등록 (Commit 5)
     * - iat  : 발급 시각 (자동 설정)
     * - exp  : 만료 시각. application.yml의 jwt.expiration(ms) 기준
     *
     * @param email 사용자 이메일
     * @param role  user_role.role_name 값
     * @return 서명된 JWT 문자열
     */
    public String generateToken(String email, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 유효성 검증.
     *
     * 검증 항목:
     * 1. 서명 위변조 여부 — secretKey로 서명 재계산 후 비교
     * 2. 만료 여부 — exp claim이 현재 시각보다 과거인지 확인
     * 3. 토큰 구조 형식 — header.payload.signature 형식인지 확인
     *
     * ExpiredJwtException을 별도로 catch하는 이유:
     * 만료는 재발급이 가능한 정상 케이스이므로 Commit 6에서
     * 위변조(SignatureException)와 다르게 처리할 수 있도록 분리해 둔다.
     *
     * @param token Authorization 헤더에서 추출한 JWT 문자열
     * @return 유효하면 true
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            // 만료 토큰: JwtAuthenticationFilter에서 request attribute로 구분 처리
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            // 위변조 / 잘못된 형식 / 빈 토큰
            return false;
        }
    }

    /**
     * 토큰이 만료되었는지 여부만 확인.
     *
     * validateToken()이 false를 반환한 경우, 이 메서드로 원인을 구분한다.
     * JwtAuthenticationFilter에서 만료/위변조를 분리해 request attribute에 저장하고,
     * JwtAuthenticationEntryPoint에서 읽어 클라이언트에 맞는 메시지를 내려준다.
     *
     * @param token JWT 문자열 (만료되었을 것으로 의심되는 토큰)
     * @return 만료된 토큰이면 true, 위변조/형식 오류면 false
     */
    public boolean isTokenExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰에서 이메일(sub claim) 추출.
     * JWT Filter에서 사용자를 특정하는 데 사용된다.
     *
     * @param token 유효성 검증이 완료된 JWT 문자열
     * @return 사용자 이메일
     */
    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * 토큰에서 role claim 추출.
     * JWT Filter에서 SecurityContext에 권한(GrantedAuthority)을 등록할 때 사용된다.
     *
     * @param token 유효성 검증이 완료된 JWT 문자열
     * @return role 문자열 (user_role.role_name)
     */
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 서명 검증 후 Claims(payload 전체) 파싱.
     *
     * verifyWith(secretKey)가 서명을 검증하므로
     * 이 메서드 호출 성공 = 위변조되지 않은 토큰임이 보장된다.
     * 만료 및 형식 오류는 JwtException 계열로 throw된다.
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
