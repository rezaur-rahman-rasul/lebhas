package com.lebhas.creativesaas.generatedversion.application.dto;

import java.util.UUID;

public record GeneratedVersionApprovalActionCommand(
        UUID workspaceId,
        UUID generatedVersionId,
        String comment
) {
}
