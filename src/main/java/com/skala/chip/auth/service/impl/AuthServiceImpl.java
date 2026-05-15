package com.skala.chip.auth.service.impl;

import com.skala.chip.auth.dto.AuthRequestDTO;
import com.skala.chip.auth.dto.AuthResponseDTO;
import com.skala.chip.auth.jwt.JwtProvider;
import com.skala.chip.auth.service.AuthService;
import com.skala.chip.exception.code.ErrorCode;
import com.skala.chip.exception.custom.BusinessException;
import com.skala.chip.exception.custom.DuplicateEmailException;
import com.skala.chip.exception.custom.DuplicateUsernameException;
import com.skala.chip.exception.custom.InactiveUserException;
import com.skala.chip.exception.custom.InvalidCredentialsException;
import com.skala.chip.user.domain.User;
import com.skala.chip.user.domain.UserRole;
import com.skala.chip.user.repository.UserRepository;
import com.skala.chip.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 로그인 인증 비즈니스 로직 구현체.
 *
 * 인증 흐름:
 * 1. 이메일로 사용자 조회
 * 2. 계정 활성화 여부 확인 (is_active)
 * 3. BCrypt 비밀번호 검증
 * 4. JWT Access Token 발급
 * 5. LoginResponse 반환
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 이메일/비밀번호 기반 로그인 처리.
     *
     * @param request 이메일 + 평문 비밀번호
     * @return AccessToken, tokenType, username, role 포함 LoginResponse
     * @throws InvalidCredentialsException 이메일 없음 또는 비밀번호 불일치
     * @throws InactiveUserException       비활성 계정 로그인 시도
     */
    @Override
    @Transactional
    public AuthResponseDTO.LoginResponse login(AuthRequestDTO.LoginRequest request) {

        // 1. 이메일로 사용자 조회
        // USER_NOT_FOUND가 아닌 INVALID_CREDENTIALS를 던지는 이유:
        // 이메일 존재 여부를 노출하면 공격자가 계정 존재를 추측할 수 있다.
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        // 2. 계정 활성화 여부 확인
        // 비밀번호 검증 전에 확인하는 이유: 비활성 계정은 인증 자체를 차단해야 하며,
        // 불필요한 BCrypt 연산(고비용)을 줄이기 위해 선행 검증한다.
        if (!user.isActive()) {
            throw new InactiveUserException();
        }

        // 3. 비밀번호 검증 (평문 vs DB의 BCrypt 해시값 비교)
        // matches()는 단방향 해시라 역산이 불가능하므로 평문과 해시를 직접 비교한다.
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        // 4. 마지막 로그인 시간 갱신
        user.updateLastLogin();

        // 5. JWT Access Token 발급
        // Claim에 email(sub)과 role을 담아 이후 요청에서 DB 조회 없이 사용자를 식별한다.
        String accessToken = jwtProvider.generateToken(
                user.getEmail(),
                user.getRole().getRoleName()
        );

        // 6. 클라이언트가 Authorization 헤더를 구성할 수 있도록 tokenType도 함께 반환한다.
        return AuthResponseDTO.LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .username(user.getUsername())
                .role(user.getRole().getRoleName())
                .build();
    }

    @Override
    @Transactional
    public AuthResponseDTO.SignUpResponse signUp(AuthRequestDTO.SignUpRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException();
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUsernameException();
        }

        UserRole userRole = userRoleRepository.findByRoleName("WORKER")
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR));

        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .role(userRole)
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return AuthResponseDTO.SignUpResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
