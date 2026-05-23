package com.lebhas.creativesaas.workspace.interfaces;

import com.lebhas.creativesaas.common.validation.ValidationMessages;
import com.lebhas.creativesaas.product.domain.ProductServiceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProductServiceRequest(
        @NotBlank(message = ValidationMessages.REQUIRED)
        @Size(max = 140)
        String name,
        @Size(max = 2000)
        String description,
        @Size(max = 120)
        String category,
        @Size(max = 240)
        String targetAudience,
        String sellingPoints,
        ProductServiceStatus status
) {
}
