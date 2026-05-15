package com.skala.chip.exception.custom;

import com.skala.chip.exception.code.ErrorCode;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException() {
        super(ErrorCode.USER_NOT_FOUND);
    }
}
