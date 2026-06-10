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

    // --- JWT ---
    // 만료와 위변조를 구분하는 이유: 클라이언트가 만료 시 재발급을 시도할 수 있도록 안내하기 위함
    TOKEN_EXPIRED(401, "만료된 토큰입니다. 다시 로그인해주세요."),
    INVALID_TOKEN(401, "유효하지 않은 토큰입니다."),

    // --- User ---
    USER_NOT_FOUND(404, "사용자를 찾을 수 없습니다."),
    INACTIVE_USER(403, "비활성화된 계정입니다."),
    DUPLICATE_EMAIL(409, "이미 사용 중인 이메일입니다."),
    ROLE_NOT_FOUND(404, "존재하지 않는 역할입니다."),
    DISTRICT_NOT_FOUND(404, "존재하지 않는 구역입니다."),

    // --- Reschedule ---
    RESCHEDULE_GROUP_NOT_FOUND(404, "존재하지 않는 재조정 그룹입니다."),
    RESCHEDULE_DETAIL_NOT_READY(400, "재조정안이 아직 생성되지 않았습니다."),
    RESCHEDULE_STRATEGY_NOT_FOUND(404, "해당 전략의 재조정안을 찾을 수 없습니다."),
    RESCHEDULE_EXPIRED(409, "만료된 재조정안은 선택할 수 없습니다."),
    RESCHEDULE_GENERATE_FAILED(502, "재조정안 생성에 실패했습니다. (에이전트 호출 오류)"),

    // --- Common ---
    INVALID_INPUT(400, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int code;
    private final String message;
}
