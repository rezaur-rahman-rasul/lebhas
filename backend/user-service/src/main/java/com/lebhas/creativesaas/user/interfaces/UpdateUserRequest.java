package com.lebhas.creativesaas.user.interfaces;

import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 80)
        String firstName,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 80)
        String lastName,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Email
        @Size(max = 160)
        String email,
        @Size(max = 30)
        String phone,
        Role role
) {
}
