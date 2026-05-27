package com.lebhas.creativesaas.profile.application;

import com.lebhas.creativesaas.common.api.ApiError;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PasswordStrengthPolicyService {

    private final int minLength;
    private final int maxLength;
    private final boolean requireUppercase;
    private final boolean requireLowercase;
    private final boolean requireDigit;
    private final boolean requireSymbol;

    public PasswordStrengthPolicyService(
            @Value("${platform.profile.password.min-length:8}") int minLength,
            @Value("${platform.profile.password.max-length:100}") int maxLength,
            @Value("${platform.profile.password.require-uppercase:true}") boolean requireUppercase,
            @Value("${platform.profile.password.require-lowercase:true}") boolean requireLowercase,
            @Value("${platform.profile.password.require-digit:true}") boolean requireDigit,
            @Value("${platform.profile.password.require-symbol:true}") boolean requireSymbol
    ) {
        this.minLength = Math.max(1, minLength);
        this.maxLength = Math.max(this.minLength, maxLength);
        this.requireUppercase = requireUppercase;
        this.requireLowercase = requireLowercase;
        this.requireDigit = requireDigit;
        this.requireSymbol = requireSymbol;
    }

    public void validate(String password) {
        List<ApiError> errors = new ArrayList<>();
        if (password == null || password.isBlank()) {
            errors.add(ApiError.of(ErrorCode.VALIDATION_FAILED.code(), "newPassword", "New password is required"));
        } else {
            if (password.length() < minLength || password.length() > maxLength) {
                errors.add(ApiError.of(
                        ErrorCode.VALIDATION_FAILED.code(),
                        "newPassword",
                        "New password length is invalid"));
            }
            if (requireUppercase && password.chars().noneMatch(Character::isUpperCase)) {
                errors.add(ApiError.of(ErrorCode.VALIDATION_FAILED.code(), "newPassword", "New password must include an uppercase letter"));
            }
            if (requireLowercase && password.chars().noneMatch(Character::isLowerCase)) {
                errors.add(ApiError.of(ErrorCode.VALIDATION_FAILED.code(), "newPassword", "New password must include a lowercase letter"));
            }
            if (requireDigit && password.chars().noneMatch(Character::isDigit)) {
                errors.add(ApiError.of(ErrorCode.VALIDATION_FAILED.code(), "newPassword", "New password must include a number"));
            }
            if (requireSymbol && password.chars().allMatch(Character::isLetterOrDigit)) {
                errors.add(ApiError.of(ErrorCode.VALIDATION_FAILED.code(), "newPassword", "New password must include a symbol"));
            }
        }
        if (!errors.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Password strength validation failed", errors);
        }
    }
}
