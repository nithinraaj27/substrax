package com.substrax.common.exception;

import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException {
    public BadRequestException(String message) {
        super(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST, message);
    }
}
