package com.lebhas.creativesaas.payment.application.event;

import com.lebhas.creativesaas.payment.domain.InvoiceStatus;
import com.lebhas.creativesaas.payment.domain.InvoiceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceEventDto(
        UUID workspaceId,
        UUID invoiceId,
        UUID transactionId,
        String invoiceNumber,
        InvoiceType invoiceType,
        BigDecimal amount,
        String currency,
        InvoiceStatus status,
        Instant issuedAt,
        Instant paidAt
) {
}
