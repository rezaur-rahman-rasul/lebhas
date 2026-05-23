package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductServiceRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 140)
        String name,
        @Size(max = 2000)
        String description,
        @Size(max = 120)
        String category,
        @Size(max = 240)
        String targetAudience,
        String sellingPoints
) {
}
