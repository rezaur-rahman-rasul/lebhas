package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.common.validation.timezone.ValidTimezone;
import com.lebhas.creativesaas.common.validation.url.OptionalHttpUrl;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.domain.WorkspaceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 120)
        String name,
        @Size(max = 120)
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = ValidationMessages.INVALID_SLUG)
        String slug,
        @Size(max = 300)
        @OptionalHttpUrl(message = ValidationMessages.INVALID_URL)
        String logoUrl,
        @Size(max = 1000)
        String description,
        @Size(max = 80)
        String industry,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 80)
        @ValidTimezone(message = ValidationMessages.INVALID_TIMEZONE)
        String timezone,
        @NotNull(message = ValidationMessages.REQUIRED)
        WorkspaceLanguage language,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency,
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Pattern(regexp = "^[A-Z]{2}$")
        String country,
        WorkspaceStatus status
) {
}
