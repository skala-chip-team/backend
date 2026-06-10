package com.skala.chip.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class UserRequestDTO {

    @Getter
    public static class SignupRequest {

        @Email
        @NotBlank
        private String email;

        @NotBlank
        private String password;

        @NotBlank
        private String name;
    }

    @Getter
    @NoArgsConstructor
    public static class RoleChangeRequest {

        @NotBlank
        private String roleName;
    }

    /** 구역 권한 배정 요청. 빈 리스트면 전체 해제. */
    @Getter
    @NoArgsConstructor
    public static class DistrictAssignRequest {

        @NotNull
        private List<String> districtIds;
    }
}