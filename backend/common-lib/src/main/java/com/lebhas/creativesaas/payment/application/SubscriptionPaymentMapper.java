package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.application.dto.SubscriptionPaymentSessionView;
import com.lebhas.creativesaas.payment.cache.PaymentSessionCacheEntry;
import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.Invoice;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.SubscriptionOrder;
import com.lebhas.pricing.PricingPlan;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class SubscriptionPaymentMapper {

    public SubscriptionPaymentSessionView toSessionView(
            SubscriptionOrder order,
            PaymentTransaction transaction,
            Invoice invoice,
            PricingPlan pricingPlan,
            PaymentSessionResponse sessionResponse
    ) {
        return new SubscriptionPaymentSessionView(
                order.getId(),
                transaction.getId(),
                invoice.getId(),
                invoice.getInvoiceNumber(),
                order.getWorkspaceId(),
                order.getPricingPlanId(),
                pricingPlan.getCode(),
                pricingPlan.getName(),
                order.getBillingCycle(),
                transaction.getPaymentPurpose(),
                order.getAmount(),
                order.getCurrency(),
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
            SubscriptionOrder order,
            PaymentTransaction transaction,
            Invoice invoice,
            PaymentSessionResponse sessionResponse
    ) {
        return new PaymentSessionCacheEntry(
                transaction.getId(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                order.getId(),
                null,
                invoice.getId(),
                order.getWorkspaceId(),
                order.getPricingPlanId(),
                null,
                transaction.getProviderId(),
                sessionResponse.providerCode(),
                transaction.getProviderSessionId(),
                transaction.getProviderTransactionId(),
                sessionResponse.redirectUrl(),
                transaction.getPaymentPurpose(),
                order.getBillingCycle(),
                order.getAmount(),
                order.getCurrency(),
                Instant.now()
        );
    }

    public PaymentSessionRequest toPaymentSessionRequest(
            SubscriptionOrder order,
            PaymentTransaction transaction,
            PaymentPurpose paymentPurpose,
            String providerCode
    ) {
        return new PaymentSessionRequest(
                order.getWorkspaceId(),
                order.getRequestedBy(),
                paymentPurpose,
                order.getAmount(),
                order.getCurrency(),
                "subscription_order",
                order.getId(),
                null,
                providerCode,
                transaction.getId().toString(),
                java.util.Map.of(
                        "subscriptionOrderId", order.getId().toString(),
                        "billingCycle", order.getBillingCycle().name()
                )
        );
    }

    public BillingCycle billingCycleOrDefault(BillingCycle billingCycle) {
        return billingCycle == null ? BillingCycle.MONTHLY : billingCycle;
    }
}
