package com.skala.chip.auth.service.impl;

import com.skala.chip.auth.dto.AuthRequestDTO;
import com.skala.chip.auth.dto.AuthResponseDTO;
import com.skala.chip.auth.jwt.JwtProvider;
import com.skala.chip.exception.custom.InactiveUserException;
import com.skala.chip.exception.custom.InvalidCredentialsException;
import com.skala.chip.user.domain.User;
import com.skala.chip.user.domain.UserRole;
import com.skala.chip.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @InjectMocks
    private AuthServiceImpl authServiceImpl;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    private User mockUser;
    private UserRole mockUserRole;
    private AuthRequestDTO.LoginRequest mockRequest;

    private static final String TEST_EMAIL = "test@test.com";
    private static final String TEST_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$hashed";
    private static final String TEST_USERNAME = "testUser";
    private static final String TEST_ROLE = "USER";
    private static final String MOCK_TOKEN = "mocked.jwt.token";

    @BeforeEach
    void setUp() {
        // mock 객체 생성만 수행.
        // getEmail()은 모든 테스트에서 AuthServiceImpl 첫 줄(findByEmail 호출)에 필요하므로 공통 stub.
        // getPassword()는 이메일 없음/비활성 계정 테스트에서는 호출되지 않으므로 각 테스트에서만 stub.
        mockUserRole = mock(UserRole.class);
        mockUser = mock(User.class);

        mockRequest = mock(AuthRequestDTO.LoginRequest.class);
        when(mockRequest.getEmail()).thenReturn(TEST_EMAIL);
    }

    @Test
    @DisplayName("로그인 성공 - 유효한 이메일/비밀번호이면 LoginResponse를 반환한다")
    void 로그인_성공() {
        // given
        when(mockRequest.getPassword()).thenReturn(TEST_PASSWORD);
        when(mockUserRole.getRoleName()).thenReturn(TEST_ROLE);
        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getEmail()).thenReturn(TEST_EMAIL);
        when(mockUser.getPasswordHash()).thenReturn(HASHED_PASSWORD);
        when(mockUser.getUsername()).thenReturn(TEST_USERNAME);
        when(mockUser.getRole()).thenReturn(mockUserRole);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(true);
        when(jwtProvider.generateToken(TEST_EMAIL, TEST_ROLE)).thenReturn(MOCK_TOKEN);

        // when
        AuthResponseDTO.LoginResponse response = authServiceImpl.login(mockRequest);

        // then
        assertThat(response.getAccessToken()).isEqualTo(MOCK_TOKEN);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(response.getRole()).isEqualTo(TEST_ROLE);
    }

    @Test
    @DisplayName("이메일 없음 - 존재하지 않는 이메일이면 InvalidCredentialsException 발생")
    void 이메일_없음_예외() {
        // given: findByEmail 실패 후 즉시 예외 발생.
        // getPassword() / passwordEncoder / jwtProvider 는 호출되지 않으므로 stub 불필요.
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authServiceImpl.login(mockRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("비밀번호 불일치 - BCrypt 검증 실패 시 InvalidCredentialsException 발생")
    void 비밀번호_불일치_예외() {
        // given: isActive 확인 후 비밀번호 검증 단계에서 실패.
        // jwtProvider.generateToken()은 호출되지 않으므로 stub 불필요.
        when(mockRequest.getPassword()).thenReturn(TEST_PASSWORD);
        when(mockUser.isActive()).thenReturn(true);
        when(mockUser.getPasswordHash()).thenReturn(HASHED_PASSWORD);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.matches(TEST_PASSWORD, HASHED_PASSWORD)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authServiceImpl.login(mockRequest))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("비활성 계정 - is_active=false이면 비밀번호 검증 전에 InactiveUserException 발생")
    void 비활성_계정_예외() {
        // given: isActive 체크에서 즉시 예외 발생.
        // getPassword() / passwordEncoder / jwtProvider 는 호출되지 않으므로 stub 불필요.
        when(mockUser.isActive()).thenReturn(false);
        when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(mockUser));

        // when & then
        assertThatThrownBy(() -> authServiceImpl.login(mockRequest))
                .isInstanceOf(InactiveUserException.class);
    }
}
