package com.lebhas.creativesaas.payment.application.dto;

import com.lebhas.creativesaas.payment.domain.PaymentOrderStatus;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageResult;

import java.util.UUID;

public record CreditPurchaseSettlementView(
        UUID creditPurchaseOrderId,
        UUID paymentTransactionId,
        UUID workspaceId,
        long credits,
        PaymentOrderStatus orderStatus,
        PaymentTransactionStatus paymentStatus,
        CreditUsageResult creditUsageResult
) {
}
