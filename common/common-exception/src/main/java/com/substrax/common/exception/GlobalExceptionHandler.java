package com.substrax.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.MDC;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 1. Handle all custom business exceptions (NotFound, Conflict, ServiceUnavailable, etc.)
    @ExceptionHandler(value = BaseException.class)
    public ResponseEntity<ApiError> handleBaseException(BaseException ex, HttpServletRequest request){
        ApiError error = new ApiError(
                ex.getErrorCode(),
                ex.getMessage(),
                getTraceId(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, ex.getStatus());
    }

    // 2. Handle Validation Error  (Missing Field in the JSON)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ApiError error = new ApiError(
                ErrorCode.VALIDATION_ERROR,
                message,
                getTraceId(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        // THIS IS THE MISSING LINK: It prints the error to your 'docker logs'
        log.error("CRITICAL: Unexpected error caught in GlobalExceptionHandler [TraceID: {}]", getTraceId(), ex);

        ApiError error = new ApiError(
                ErrorCode.INTERNAL_ERROR,
                "Internal Error: " + ex.getMessage(), // Let's see the message in Postman for now
                getTraceId(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request){

        log.error("UnHandled Exception", ex);
        ApiError error = new ApiError(
                ErrorCode.INVALID_REQUEST,
                ex.getMessage(),
                getTraceId(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiError> handleConflictException(IdempotencyConflictException ex, HttpServletRequest request)
    {
        ApiError apiError = new ApiError(
                ex.getErrorCode(),
                ex.getMessage(),
                getTraceId(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(apiError, ex.getStatus());
    }

    private String getTraceId() {
        return MDC.get("traceId");
    }
}
