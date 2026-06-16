package com.skala.chip.user.service.impl;

import com.skala.chip.exception.code.ErrorCode;
import com.skala.chip.exception.custom.BusinessException;
import com.skala.chip.exception.custom.UserNotFoundException;
import com.skala.chip.monitoring.repository.DistrictRepository;
import com.skala.chip.user.domain.User;
import com.skala.chip.user.domain.UserDistrictMap;
import com.skala.chip.user.domain.UserRole;
import com.skala.chip.user.dto.UserRequestDTO;
import com.skala.chip.user.dto.UserResponseDTO;
import com.skala.chip.user.repository.UserDistrictMapRepository;
import com.skala.chip.user.repository.UserRepository;
import com.skala.chip.user.repository.UserRoleRepository;
import com.skala.chip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserDistrictMapRepository userDistrictMapRepository;
    private final DistrictRepository districtRepository;

    @Override
    @Transactional
    public void removeUser(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserResponseDTO.UserInfo changeRole(String userId, UserRequestDTO.RoleChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 관리자(ADMIN) 계정의 역할은 변경 불가 (권한 강등/탈취 방지)
        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getRoleName())) {
            throw new BusinessException(ErrorCode.ADMIN_ROLE_CHANGE_FORBIDDEN);
        }

        UserRole newRole = userRoleRepository.findByRoleName(request.getRoleName())
                .orElseThrow(() -> new BusinessException(ErrorCode.ROLE_NOT_FOUND));

        user.updateRole(newRole);

        return UserResponseDTO.UserInfo.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getRoleName())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDTO.UserSummary> getUsers() {
        List<User> users = userRepository.findAll();

        // 사용자별 담당 구역을 한 번에 조회 (N+1 방지)
        List<String> userIds = users.stream().map(User::getUserId).toList();
        Map<String, List<String>> districtsByUser = userIds.isEmpty()
                ? Map.of()
                : userDistrictMapRepository.findByUserIdIn(userIds).stream()
                        .collect(Collectors.groupingBy(
                                UserDistrictMap::getUserId,
                                Collectors.mapping(UserDistrictMap::getDistrictId, Collectors.toList())));

        return users.stream()
                .map(u -> UserResponseDTO.UserSummary.builder()
                        .userId(u.getUserId())
                        .username(u.getUsername())
                        .email(u.getEmail())
                        .role(u.getRole() != null ? u.getRole().getRoleName() : null)
                        .active(u.isActive())
                        .districtIds(districtsByUser.getOrDefault(u.getUserId(), List.of()))
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public UserResponseDTO.UserDistricts assignDistricts(
            String userId, UserRequestDTO.DistrictAssignRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 중복/널 제거
        List<String> districtIds = request.getDistrictIds() == null
                ? List.of()
                : request.getDistrictIds().stream()
                        .filter(Objects::nonNull)
                        .filter(s -> !s.isBlank())
                        .distinct()
                        .toList();

        // 존재하지 않는 구역이면 거부 (FK 오류 대신 404)
        for (String districtId : districtIds) {
            if (!districtRepository.existsById(districtId)) {
                throw new BusinessException(ErrorCode.DISTRICT_NOT_FOUND);
            }
        }

        // 기존 매핑을 모두 지우고 새 집합으로 교체 (PUT = 전체 치환)
        userDistrictMapRepository.deleteByUserId(userId);
        for (String districtId : districtIds) {
            userDistrictMapRepository.save(UserDistrictMap.builder()
                    .mapId("UDM-" + UUID.randomUUID())
                    .userId(userId)
                    .districtId(districtId)
                    .build());
        }

        return UserResponseDTO.UserDistricts.builder()
                .userId(userId)
                .districtIds(districtIds)
                .build();
    }
}
