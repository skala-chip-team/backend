package com.skala.chip.user.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.user.dto.UserRequestDTO;
import com.skala.chip.user.dto.UserResponseDTO;
import com.skala.chip.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "회원 관리 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원 삭제")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeUser(@PathVariable String userId) {
        userService.removeUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "역할 변경")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<UserResponseDTO.UserInfo>> changeRole(
            @PathVariable String userId,
            @Valid @RequestBody UserRequestDTO.RoleChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.changeRole(userId, request)));
    }
}
