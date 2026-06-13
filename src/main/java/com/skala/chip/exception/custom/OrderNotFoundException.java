package com.skala.chip.exception.custom;

import com.skala.chip.exception.code.ErrorCode;

public class OrderNotFoundException extends BusinessException {
    public OrderNotFoundException() {
        super(ErrorCode.ORDER_NOT_FOUND);
    }
}
