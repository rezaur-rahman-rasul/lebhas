package com.lebhas.creativesaas.creativerequest.application.dto;

import java.util.UUID;

public record RetryCreativeRequestCommand(
        UUID workspaceId,
        UUID creativeRequestId
) {
}
