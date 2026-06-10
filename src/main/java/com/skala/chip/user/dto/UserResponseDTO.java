package com.skala.chip.user.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class UserResponseDTO {

    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class UserInfo {

        private String userId;
        private String username;
        private String email;
        private String role;
    }

    /** 사용자 목록 항목. 담당 구역(권한)까지 포함한다. */
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class UserSummary {

        private String userId;
        private String username;
        private String email;
        private String role;
        private boolean active;
        private List<String> districtIds;
    }

    /** 구역 권한 배정 결과. */
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class UserDistricts {

        private String userId;
        private List<String> districtIds;
    }
}