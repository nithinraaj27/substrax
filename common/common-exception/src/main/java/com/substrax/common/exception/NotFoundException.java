package com.substrax.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseException {
    public NotFoundException(String message) {


        super(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND ,message);
    }
}
