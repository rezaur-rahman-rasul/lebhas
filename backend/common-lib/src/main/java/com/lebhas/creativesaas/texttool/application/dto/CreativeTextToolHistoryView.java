package com.lebhas.creativesaas.texttool.application.dto;

import com.lebhas.creativesaas.texttool.domain.CreativeTextToolStatus;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record CreativeTextToolHistoryView(
        UUID id,
        UUID workspaceId,
        UUID projectId,
        UUID textToolOutputId,
        CreativeTextToolType toolType,
        String toolCode,
        CreativeTextToolStatus status,
        BigDecimal creditCost,
        String failureReason,
        Map<String, Object> request,
        Map<String, Object> response,
        Instant createdAt
) {
}
