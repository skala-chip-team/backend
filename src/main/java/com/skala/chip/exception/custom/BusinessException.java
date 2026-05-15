package com.skala.chip.exception.custom;

import com.skala.chip.exception.code.ErrorCode;
import lombok.Getter;

/**
 * 서비스 비즈니스 예외의 기반 클래스.
 *
 * 모든 커스텀 예외가 이 클래스를 상속하도록 설계한 이유:
 * GlobalExceptionHandler에서 개별 예외마다 @ExceptionHandler를 추가하지 않고
 * BusinessException 하나로 통합 처리할 수 있어 핸들러 코드가 단순해진다.
 * 각 예외 클래스는 ErrorCode만 지정하면 되고, HTTP 상태코드와 메시지는
 * ErrorCode enum에서 중앙 관리된다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
