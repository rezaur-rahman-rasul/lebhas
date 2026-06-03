package com.lebhas.creativesaas.texttool.application.dto;

import com.lebhas.creativesaas.texttool.domain.CreativeTextToolType;

import java.util.UUID;

public record CreativeTextToolCommand(
        UUID workspaceId,
        UUID projectId,
        CreativeTextToolType toolType,
        CreativeTextToolRequest request
) {
}
