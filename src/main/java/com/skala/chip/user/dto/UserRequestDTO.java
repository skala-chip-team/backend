package com.skala.chip.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

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
}