package com.lebhas.creativesaas.common.api;

import java.time.Instant;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        List<ApiError> errors,
        Instant timestamp
) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, List.of(), Instant.now());
    }

    public static <T> ApiResponse<T> success(T data) {
        return success("Request completed successfully", data);
    }

    public static <T> ApiResponse<T> failure(String message, List<ApiError> errors) {
        return new ApiResponse<>(false, message, null, List.copyOf(errors), Instant.now());
    }

    public static <T> ApiResponse<T> failure(String message, ApiError error) {
        return failure(message, List.of(error));
    }
}
