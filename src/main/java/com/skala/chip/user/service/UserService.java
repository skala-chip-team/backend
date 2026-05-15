package com.skala.chip.user.service;

import com.skala.chip.user.dto.UserRequestDTO;
import com.skala.chip.user.dto.UserResponseDTO;

public interface UserService {

    void removeUser(String userId);

    UserResponseDTO.UserInfo changeRole(String userId, UserRequestDTO.RoleChangeRequest request);
}
