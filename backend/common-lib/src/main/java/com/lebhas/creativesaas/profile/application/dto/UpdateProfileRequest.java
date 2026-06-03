package com.lebhas.creativesaas.profile.application.dto;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 80)
        String firstName,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 80)
        String lastName,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 160)
        String displayName,
        @Size(max = 30)
        String phoneNumber,
        @Size(max = 120)
        String jobTitle,
        @Size(max = 1000)
        String bio,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 80)
        String timezone,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 20)
        String locale
) {
    public UpdateProfileRequest(
            String firstName,
            String lastName,
            String displayName,
            String phoneNumber,
            String jobTitle,
            String timezone,
            String locale
    ) {
        this(firstName, lastName, displayName, phoneNumber, jobTitle, null, timezone, locale);
    }
}
