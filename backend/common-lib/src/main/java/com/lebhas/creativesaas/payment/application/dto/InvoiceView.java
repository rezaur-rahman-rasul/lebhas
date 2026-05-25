package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.InvoiceStatus;
import com.lebhas.creativesaas.payment.domain.InvoiceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InvoiceView(
        UUID id,
        UUID workspaceId,
        UUID paymentTransactionId,
        String invoiceNumber,
        InvoiceType invoiceType,
        BigDecimal amount,
        String currency,
        InvoiceStatus status,
        Instant issuedAt,
        Instant paidAt,
        Instant createdAt
) {
}
