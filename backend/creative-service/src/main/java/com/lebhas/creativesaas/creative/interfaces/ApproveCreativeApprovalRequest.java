package com.lebhas.creativesaas.creative.interfaces;

import jakarta.validation.constraints.Size;

public record ApproveCreativeApprovalRequest(
        @Size(max = 2000)
        String approvalNote
) {
}
