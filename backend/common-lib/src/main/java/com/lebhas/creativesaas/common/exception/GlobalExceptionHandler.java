package com.lebhas.creativesaas.common.exception;

import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ApiError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toApiError)
                .toList();
        return failure(ErrorCode.VALIDATION_FAILED, errors);
    }

    @ExceptionHandler(BindException.class)
    ResponseEntity<ApiResponse<Void>> handleBindException(BindException exception) {
        List<ApiError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toApiError)
                .toList();
        return failure(ErrorCode.VALIDATION_FAILED, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException exception) {
        List<ApiError> errors = exception.getConstraintViolations().stream()
                .map(violation -> ApiError.of(
                        ErrorCode.VALIDATION_FAILED.code(),
                        violation.getPropertyPath().toString(),
                        violation.getMessage()))
                .toList();
        return failure(ErrorCode.VALIDATION_FAILED, errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException exception) {
        ApiError error = ApiError.of(
                ErrorCode.VALIDATION_FAILED.code(),
                exception.getParameterName(),
                "Required request parameter is missing");
        return failure(ErrorCode.VALIDATION_FAILED, List.of(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        ApiError error = ApiError.of(ErrorCode.VALIDATION_FAILED.code(), "Malformed request body");
        return failure(ErrorCode.VALIDATION_FAILED, List.of(error));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException exception) {
        ApiError error = ApiError.of(
                ErrorCode.VALIDATION_FAILED.code(),
                exception.getName(),
                "Request value could not be converted to the required type");
        return failure(ErrorCode.VALIDATION_FAILED, List.of(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        ApiError error = ApiError.of(ErrorCode.VALIDATION_FAILED.code(), exception.getMessage());
        return failure(ErrorCode.VALIDATION_FAILED, List.of(error));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception, HttpServletRequest request) {
        log.warn("Business exception at {} {}: {}", request.getMethod(), request.getRequestURI(), exception.getMessage());
        return ResponseEntity
                .status(exception.getErrorCode().httpStatus())
                .body(ApiResponse.failure(exception.getMessage(), exception.getErrors()));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException exception) {
        return failure(ErrorCode.UNAUTHORIZED, List.of(ApiError.of(ErrorCode.UNAUTHORIZED.code(), ErrorCode.UNAUTHORIZED.defaultMessage())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return failure(ErrorCode.FORBIDDEN, List.of(ApiError.of(ErrorCode.FORBIDDEN.code(), ErrorCode.FORBIDDEN.defaultMessage())));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected exception at {} {}", request.getMethod(), request.getRequestURI(), exception);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure(
                        ErrorCode.INTERNAL_ERROR.defaultMessage(),
                        ApiError.of(ErrorCode.INTERNAL_ERROR.code(), ErrorCode.INTERNAL_ERROR.defaultMessage())));
    }

    private ApiError toApiError(FieldError fieldError) {
        return ApiError.of(
                ErrorCode.VALIDATION_FAILED.code(),
                fieldError.getField(),
                fieldError.getDefaultMessage() == null ? ErrorCode.VALIDATION_FAILED.defaultMessage() : fieldError.getDefaultMessage());
    }

    private ResponseEntity<ApiResponse<Void>> failure(ErrorCode errorCode, List<ApiError> errors) {
        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(ApiResponse.failure(errorCode.defaultMessage(), errors));
    }
}
