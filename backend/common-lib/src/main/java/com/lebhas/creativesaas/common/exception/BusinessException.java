package com.lebhas.creativesaas.common.exception;

import com.lebhas.creativesaas.common.api.ApiError;

import java.util.List;

public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ApiError> errors;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.defaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(errorCode, message, List.of(ApiError.of(errorCode.code(), message)));
    }

    public BusinessException(ErrorCode errorCode, String message, List<ApiError> errors) {
        super(message);
        this.errorCode = errorCode;
        this.errors = List.copyOf(errors);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ApiError> getErrors() {
        return errors;
    }
}
