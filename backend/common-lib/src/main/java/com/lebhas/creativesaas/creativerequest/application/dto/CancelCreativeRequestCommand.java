package com.lebhas.creativesaas.creativerequest.application.dto;

import java.util.UUID;

public record CancelCreativeRequestCommand(
        UUID workspaceId,
        UUID creativeRequestId
) {
}
