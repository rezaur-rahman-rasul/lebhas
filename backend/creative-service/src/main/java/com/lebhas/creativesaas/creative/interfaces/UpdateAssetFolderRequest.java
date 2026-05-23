package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateAssetFolderRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 120)
        String name,
        UUID parentFolderId,
        @Size(max = 500)
        String description
) {
}
