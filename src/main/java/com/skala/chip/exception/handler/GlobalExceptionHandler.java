package com.skala.chip.exception.handler;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.exception.code.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기.
 *
 * Controller에서 try-catch를 사용하지 않는 이유:
 * 예외 처리 로직이 비즈니스 로직과 섞이면 가독성이 떨어지고,
 * 여러 Controller에 중복 코드가 생긴다.
 * 이 클래스 한 곳에서 처리하면 일관된 ApiResponse 형식을 보장할 수 있다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * @Valid 검증 실패 시 발생하는 예외 처리.
     * 입력값 형식 오류(email 형식, notBlank 등)는 400으로 응답한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT));
    }

    /**
     * 처리되지 않은 모든 예외의 최종 fallback.
     * Stack Trace를 응답에 포함하지 않기 위해 INTERNAL_SERVER_ERROR 코드만 반환한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
