package com.substrax.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor // Essential for Jackson/Spring to handle the response
public class ApiError {

    private ErrorCode errorCode;
    private String message;
    private String traceId;
    private String path;

    @Builder.Default
    private Instant timeStamp = Instant.now();

    // Helper constructor for your Handler
    public ApiError(ErrorCode errorCode, String message, String traceId, String path) {
        this.errorCode = errorCode;
        this.message = message;
        this.traceId = traceId;
        this.path = path;
        this.timeStamp = Instant.now();
    }
}