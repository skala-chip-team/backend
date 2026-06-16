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
    DISTRICT_FORBIDDEN(403, "담당 구역이 아니어서 접근할 수 없습니다."),

    // --- Machine (장비 설정) ---
    MACHINE_NOT_FOUND(404, "존재하지 않는 장비입니다."),
    PROCESS_STEP_NOT_FOUND(404, "존재하지 않는 공정 STEP입니다."),
    MACHINE_HAS_ACTIVE_SCHEDULE(409, "진행 중인 스케줄이 있어 장비를 삭제할 수 없습니다."),

    // --- Reschedule ---
    RESCHEDULE_GROUP_NOT_FOUND(404, "존재하지 않는 재조정 그룹입니다."),
    RESCHEDULE_DETAIL_NOT_READY(400, "재조정안이 아직 생성되지 않았습니다."),
    RESCHEDULE_STRATEGY_NOT_FOUND(404, "해당 전략의 재조정안을 찾을 수 없습니다."),
    RESCHEDULE_EXPIRED(409, "만료된 재조정안은 선택할 수 없습니다."),
    RESCHEDULE_NOT_ACTIONABLE(409, "현재 대기열에 재조정 가능한 위험이 없습니다. (해당 unit이 이미 처리되었습니다)"),
    RESCHEDULE_GENERATE_FAILED(502, "재조정안 생성에 실패했습니다. (에이전트 호출 오류)"),

    // --- Order ---
    ORDER_NOT_FOUND(404, "존재하지 않는 주문입니다."),

    // --- Chatbot ---
    // 세션 lifecycle·소유권·메시지 저장은 백엔드가 소유한다. AI 서버(/infer)는 무상태 추론만.
    // 세션은 하나의 재조정 그룹에 고정되며, 그룹 검증 규칙을 백엔드에서 강제한다.
    CHATBOT_GROUP_REQUIRED(400, "새 대화에는 group_id가 필요합니다."),
    CHATBOT_SESSION_NOT_FOUND(404, "챗봇 세션을 찾을 수 없습니다."),
    CHATBOT_SESSION_FORBIDDEN(403, "다른 사용자의 챗봇 세션입니다."),
    CHATBOT_GROUP_FORBIDDEN(403, "담당 구역이 아닌 재조정 그룹에는 접근할 수 없습니다."),
    CHATBOT_GROUP_MISMATCH(409, "세션에 연결된 그룹과 다른 group_id 입니다."),
    CHATBOT_AGENT_ERROR(502, "챗봇 에이전트(추론) 호출에 실패했습니다."),

    // --- Common ---
    INVALID_INPUT(400, "잘못된 요청입니다."),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다.");

    private final int code;
    private final String message;
}
