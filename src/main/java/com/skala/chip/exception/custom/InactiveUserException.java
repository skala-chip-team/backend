package com.skala.chip.exception.custom;

import com.skala.chip.exception.code.ErrorCode;

/**
 * is_active = false 인 계정으로 로그인 시도 시 발생하는 예외.
 *
 * 비밀번호 검증 전에 먼저 확인하는 이유:
 * 비활성 계정의 비밀번호가 맞더라도 로그인을 허용하지 않아야 하며,
 * 불필요한 BCrypt 연산을 줄이기 위해 선행 검증한다.
 */
public class InactiveUserException extends BusinessException {

    public InactiveUserException() {
        super(ErrorCode.INACTIVE_USER);
    }
}
