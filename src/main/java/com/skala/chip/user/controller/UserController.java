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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "User", description = "회원 관리 API")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 목록 조회", description = "전체 사용자와 각 사용자의 담당 구역(권한)을 반환한다.")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponseDTO.UserSummary>>> getUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getUsers()));
    }

    @Operation(summary = "구역 권한 배정", description = "사용자의 담당 구역을 요청한 집합으로 전체 치환한다. (빈 리스트면 전체 해제)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{userId}/districts")
    public ResponseEntity<ApiResponse<UserResponseDTO.UserDistricts>> assignDistricts(
            @PathVariable String userId,
            @Valid @RequestBody UserRequestDTO.DistrictAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.assignDistricts(userId, request)));
    }

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
