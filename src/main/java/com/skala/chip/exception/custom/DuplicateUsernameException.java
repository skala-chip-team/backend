package com.skala.chip.exception.custom;

import com.skala.chip.exception.code.ErrorCode;

public class DuplicateUsernameException extends BusinessException {
    public DuplicateUsernameException() {
        super(ErrorCode.DUPLICATE_USERNAME);
    }
}
