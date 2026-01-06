package com.substrax.common.exception;

import java.time.Instant;

public class ApiError {

    private final ErrorCode errorCode;
    private final String message;
    private final String traceId;
    private final String path;
    private final Instant timeStamp;

    public ApiError(ErrorCode errorCode, String message, String traceId, String path) {
        this.errorCode = errorCode;
        this.message = message;
        this.traceId = traceId;
        this.path = path;
        this.timeStamp = getTimeStamp();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getMessage() {
        return message;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getPath() {
        return path;
    }

    public Instant getTimeStamp() {
        return timeStamp;
    }
}
