package com.skala.chip.exception.custom;

import com.skala.chip.exception.code.ErrorCode;

/**
 * 이메일 또는 비밀번호가 일치하지 않을 때 발생하는 예외.
 *
 * "이메일 없음"과 "비밀번호 불일치" 두 경우 모두 이 예외를 사용하는 이유:
 * 어느 쪽이 틀렸는지 구분된 메시지를 내려주면 공격자가
 * 해당 이메일의 계정 존재 여부를 추측할 수 있다.
 * 두 경우를 동일한 INVALID_CREDENTIALS로 처리해 정보를 노출하지 않는다.
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS);
    }
}
