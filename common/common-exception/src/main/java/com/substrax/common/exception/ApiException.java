package com.substrax.common.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends BaseException {
    public ApiException(String message) {
        super(ErrorCode.INVALID_REQUEST, HttpStatus.BAD_REQUEST, message);
    }
}
