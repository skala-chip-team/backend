package com.skala.chip.user.service;

import com.skala.chip.user.dto.UserRequestDTO;
import com.skala.chip.user.dto.UserResponseDTO;

import java.util.List;

public interface UserService {

    void removeUser(String userId);

    UserResponseDTO.UserInfo changeRole(String userId, UserRequestDTO.RoleChangeRequest request);

    List<UserResponseDTO.UserSummary> getUsers();

    UserResponseDTO.UserDistricts assignDistricts(String userId, UserRequestDTO.DistrictAssignRequest request);
}
