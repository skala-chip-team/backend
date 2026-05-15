package com.skala.chip.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    // HS256은 최소 32바이트 키가 필요하다.
    private static final String SECRET = "skala-chip-test-secret-key-for-unit-test!!";
    private static final long EXPIRATION = 86400000L; // 24h
    private static final String TEST_EMAIL = "test@test.com";
    private static final String TEST_ROLE = "USER";

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(SECRET, EXPIRATION);
    }

    @Test
    @DisplayName("토큰 생성 - generateToken()이 null이 아닌 문자열을 반환한다")
    void 토큰_생성_성공() {
        // when
        String token = jwtProvider.generateToken(TEST_EMAIL, TEST_ROLE);

        // then
        assertThat(token).isNotBlank();
        // JWT 형식: header.payload.signature 세 파트로 구성
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("email 추출 - 발급한 토큰의 sub claim에서 email을 정확히 추출한다")
    void email_추출() {
        // given
        String token = jwtProvider.generateToken(TEST_EMAIL, TEST_ROLE);

        // when
        String email = jwtProvider.getEmail(token);

        // then
        assertThat(email).isEqualTo(TEST_EMAIL);
    }

    @Test
    @DisplayName("role 추출 - 발급한 토큰의 role claim에서 role을 정확히 추출한다")
    void role_추출() {
        // given
        String token = jwtProvider.generateToken(TEST_EMAIL, TEST_ROLE);

        // when
        String role = jwtProvider.getRole(token);

        // then
        assertThat(role).isEqualTo(TEST_ROLE);
    }

    @Test
    @DisplayName("유효한 토큰 검증 - validateToken()이 true를 반환한다")
    void 유효한_토큰_검증() {
        // given
        String token = jwtProvider.generateToken(TEST_EMAIL, TEST_ROLE);

        // when & then
        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("만료 토큰 - expiration=-1000ms로 즉시 만료 토큰 생성 시 validateToken()이 false를 반환한다")
    void 만료_토큰_검증() {
        // given: 만료 시간을 음수로 설정해 이미 만료된 토큰 생성
        // 같은 secret을 써야 정상적으로 파싱(서명 검증)되어 ExpiredJwtException이 발생한다.
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1000L);
        String expiredToken = expiredProvider.generateToken(TEST_EMAIL, TEST_ROLE);

        // when & then
        assertThat(jwtProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("만료 토큰 isTokenExpired - 만료 토큰이면 true를 반환한다")
    void 만료_토큰_isTokenExpired_true() {
        // given
        JwtProvider expiredProvider = new JwtProvider(SECRET, -1000L);
        String expiredToken = expiredProvider.generateToken(TEST_EMAIL, TEST_ROLE);

        // when & then
        assertThat(jwtProvider.isTokenExpired(expiredToken)).isTrue();
    }

    @Test
    @DisplayName("잘못된 토큰 - 위변조/형식 오류 토큰이면 validateToken()이 false를 반환한다")
    void 잘못된_토큰_검증() {
        // when & then
        assertThat(jwtProvider.validateToken("invalid.token.string")).isFalse();
    }

    @Test
    @DisplayName("잘못된 토큰 isTokenExpired - 위변조 토큰이면 false를 반환한다 (만료가 아닌 다른 오류)")
    void 잘못된_토큰_isTokenExpired_false() {
        // 만료 토큰(ExpiredJwtException)이 아닌 경우 isTokenExpired()는 false여야 한다.
        assertThat(jwtProvider.isTokenExpired("invalid.token.string")).isFalse();
    }
}
