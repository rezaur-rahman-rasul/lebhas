package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.application.dto.CreditPurchasePaymentSessionView;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchaseSettlementView;
import com.lebhas.creativesaas.payment.cache.PaymentSessionCacheEntry;
import com.lebhas.creativesaas.payment.domain.CreditPackage;
import com.lebhas.creativesaas.payment.domain.CreditPurchaseOrder;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageResult;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class CreditPaymentMapper {

    public CreditPurchasePaymentSessionView toSessionView(
            CreditPurchaseOrder order,
            PaymentTransaction transaction,
            CreditPackage creditPackage,
            PaymentSessionResponse sessionResponse
    ) {
        return new CreditPurchasePaymentSessionView(
                order.getId(),
                transaction.getId(),
                order.getWorkspaceId(),
                order.getCreditPackageId(),
                creditPackage.getCode(),
                creditPackage.getName(),
                order.getCredits(),
                order.getAmount(),
                order.getCurrency(),
                transaction.getPaymentPurpose(),
                transaction.getProviderId(),
                sessionResponse.providerCode(),
                transaction.getProviderSessionId(),
                transaction.getProviderTransactionId(),
                sessionResponse.redirectUrl(),
                transaction.getStatus(),
                sessionResponse.message()
        );
    }

    public PaymentSessionCacheEntry toCacheEntry(
            CreditPurchaseOrder order,
            PaymentTransaction transaction,
            PaymentSessionResponse sessionResponse
    ) {
        return new PaymentSessionCacheEntry(
                transaction.getId(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                null,
                order.getId(),
                null,
                order.getWorkspaceId(),
                null,
                order.getCreditPackageId(),
                transaction.getProviderId(),
                sessionResponse.providerCode(),
                transaction.getProviderSessionId(),
                transaction.getProviderTransactionId(),
                sessionResponse.redirectUrl(),
                transaction.getPaymentPurpose(),
                null,
                order.getAmount(),
                order.getCurrency(),
                Instant.now()
        );
    }

    public CreditPurchaseSettlementView toSettlementView(
            CreditPurchaseOrder order,
            PaymentTransaction transaction,
            CreditUsageResult creditUsageResult
    ) {
        return new CreditPurchaseSettlementView(
                order.getId(),
                transaction.getId(),
                order.getWorkspaceId(),
                order.getCredits(),
                order.getStatus(),
                transaction.getStatus(),
                creditUsageResult
        );
    }
}
