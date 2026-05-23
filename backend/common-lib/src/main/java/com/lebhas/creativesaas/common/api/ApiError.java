package com.lebhas.creativesaas.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        String code,
        String field,
        String message
) {
    public static ApiError of(String code, String field, String message) {
        return new ApiError(code, field, message);
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, null, message);
    }
}
