package com.lebhas.creativesaas.profile.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConfirmProfileImageUploadRequest(
        @NotNull
        UUID uploadReferenceId
) {
}
