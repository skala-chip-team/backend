package com.skala.chip.exception.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서비스 전체에서 사용하는 에러 코드 정의.
 *
 * 에러 메시지를 코드에 하드코딩하지 않고 이 enum에서 중앙 관리한다.
 * GlobalExceptionHandler에서 이 코드를 꺼내 ApiResponse.fail()에 전달한다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // --- Auth ---
    // 이메일이 존재하지 않거나 비밀번호가 틀린 경우 동일 메시지를 내려준다.
    // 어느 쪽이 틀렸는지 노출하면 계정 존재 여부를 추측할 수 있어 보안상 위험하다.
    INVALID_CREDENTIALS(401, "이메일 또는 비밀번호가 올바르지 않습니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "접근 권한이 없습니다."),

    // --- User ---
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    // is_active = false인 계정은 로그인 자체를 차단한다.
    INACTIVE_USER(403, "비활성화된 계정입니다."),

    // --- Common ---
    INVALID_INPUT(400, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int code;
    private final String message;
}
