package com.skala.chip.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class UserResponseDTO {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class UserInfo {

        private String userId;
        private String email;
        private String name;
    }
}