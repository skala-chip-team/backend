package com.skala.chip.user.service.impl;

import com.skala.chip.exception.code.ErrorCode;
import com.skala.chip.exception.custom.BusinessException;
import com.skala.chip.exception.custom.UserNotFoundException;
import com.skala.chip.user.domain.User;
import com.skala.chip.user.domain.UserRole;
import com.skala.chip.user.dto.UserRequestDTO;
import com.skala.chip.user.dto.UserResponseDTO;
import com.skala.chip.user.repository.UserRepository;
import com.skala.chip.user.repository.UserRoleRepository;
import com.skala.chip.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

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
}
