package com.skala.chip.user.service;

import com.skala.chip.exception.code.ErrorCode;
import com.skala.chip.exception.custom.BusinessException;
import com.skala.chip.user.domain.User;
import com.skala.chip.user.repository.UserDistrictMapRepository;
import com.skala.chip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 구역(district) 기반 접근 권한 검증 공통 컴포넌트.
 *
 * - ADMIN 역할은 전체 구역 접근 허용.
 * - 일반 사용자는 담당 구역(TT_USER_DISTRICT_MAP)에 한해 접근 허용.
 * - 인증되지 않은(anonymous/null) 호출은 접근 거부(403).
 *
 * 모니터링/스케줄/재조정/생산상태 등 구역 단위 데이터 API 에서 공통으로 사용한다.
 */
@Component
@RequiredArgsConstructor
public class DistrictAccessGuard {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ANONYMOUS = "anonymousUser";

    private final UserRepository userRepository;
    private final UserDistrictMapRepository userDistrictMapRepository;

    /** 인증 주체(email)로 사용자 로딩. 없거나 anonymous 면 403. */
    private User resolveUser(String email) {
        if (email == null || email.isBlank() || ANONYMOUS.equals(email)) {
            throw new BusinessException(ErrorCode.DISTRICT_FORBIDDEN);
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_FORBIDDEN));
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(String email) {
        if (email == null || email.isBlank() || ANONYMOUS.equals(email)) {
            return false;
        }
        return userRepository.findByEmail(email)
                .map(u -> u.getRole() != null && ROLE_ADMIN.equalsIgnoreCase(u.getRole().getRoleName()))
                .orElse(false);
    }

    /** 특정 구역 접근을 강제 검증한다. ADMIN 통과, 담당 구역이 아니면 403. */
    @Transactional(readOnly = true)
    public void assertDistrict(String email, String districtId) {
        User user = resolveUser(email);
        if (isAdminUser(user)) {
            return;
        }
        boolean allowed = userDistrictMapRepository.findByUserId(user.getUserId()).stream()
                .anyMatch(m -> districtId != null && districtId.equals(m.getDistrictId()));
        if (!allowed) {
            throw new BusinessException(ErrorCode.DISTRICT_FORBIDDEN);
        }
    }

    /**
     * 사용자가 접근 가능한 구역 ID 집합을 반환한다.
     * ADMIN 이면 {@code null} 을 반환하여 "전체 허용"을 의미한다(필터 생략용).
     */
    @Transactional(readOnly = true)
    public Set<String> allowedDistrictIds(String email) {
        User user = resolveUser(email);
        if (isAdminUser(user)) {
            return null; // 전체 허용
        }
        return userDistrictMapRepository.findByUserId(user.getUserId()).stream()
                .map(m -> m.getDistrictId())
                .collect(Collectors.toSet());
    }

    private boolean isAdminUser(User user) {
        return user.getRole() != null && ROLE_ADMIN.equalsIgnoreCase(user.getRole().getRoleName());
    }
}
