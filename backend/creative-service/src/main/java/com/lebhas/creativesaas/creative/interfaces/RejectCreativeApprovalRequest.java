package com.lebhas.creativesaas.creative.interfaces;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectCreativeApprovalRequest(
        @NotBlank
        @Size(min = 2, max = 2000)
        String rejectionReason
) {
}
