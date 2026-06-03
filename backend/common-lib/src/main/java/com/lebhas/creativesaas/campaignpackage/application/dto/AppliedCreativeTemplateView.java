package com.lebhas.creativesaas.campaignpackage.application.dto;

import java.util.Map;
import java.util.UUID;

public record AppliedCreativeTemplateView(
        UUID templateId,
        UUID workspaceId,
        UUID projectId,
        UUID brandId,
        Map<String, Object> appliedPayload
) {
}
