package com.substrax.common.exception;

import org.springframework.http.HttpStatus;

public class IdempotencyConflictException extends BaseException {
    public IdempotencyConflictException(String message) {
        super(ErrorCode.CONFLICT, HttpStatus.CONFLICT, message);
    }
}
