package com.lebhas.creativesaas.imagecreative.application.dto;

import java.util.UUID;

public record ProductImageCreativeCommand(
        UUID workspaceId,
        UUID projectId,
        ProductImageCreativeRequest request
) {
}
