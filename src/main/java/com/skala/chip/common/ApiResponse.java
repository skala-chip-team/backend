package com.skala.chip.common;

import com.skala.chip.exception.code.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API 응답을 감싸는 공통 응답 클래스.
 *
 * 프론트엔드와 일관된 응답 구조를 계약하기 위해 사용한다.
 * 성공/실패 여부와 무관하게 동일한 JSON 구조를 보장한다.
 *
 * 성공: { "success": true,  "code": 200, "message": "요청 성공",   "data": {...} }
 * 실패: { "success": false, "code": 401, "message": "...",         "data": null  }
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private boolean success;
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, 200, "요청 성공", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, 201, "생성 성공", data);
    }

    /**
     * ErrorCode enum에서 HTTP 상태코드와 메시지를 가져온다.
     * 메시지를 직접 넘기지 않고 ErrorCode를 사용하는 이유는
     * 에러 메시지를 한 곳(ErrorCode)에서 관리하기 위함이다.
     */
    public static ApiResponse<?> fail(ErrorCode errorCode) {
        return new ApiResponse<>(false, errorCode.getCode(), errorCode.getMessage(), null);
    }
}
