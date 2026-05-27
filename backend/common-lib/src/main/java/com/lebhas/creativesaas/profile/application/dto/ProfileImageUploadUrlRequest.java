package com.lebhas.creativesaas.profile.application.dto;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileImageUploadUrlRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 255)
        String fileName,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 120)
        String mimeType,
        @Min(1)
        long fileSize
) {
}
