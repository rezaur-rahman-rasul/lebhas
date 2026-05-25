package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.payment.application.event.CreditPurchaseEventDto;
import com.lebhas.creativesaas.payment.application.event.InvoiceEventDto;
import com.lebhas.creativesaas.payment.application.event.PaymentTransactionEventDto;
import com.lebhas.creativesaas.payment.application.event.PaymentWebhookEventDto;
import com.lebhas.creativesaas.payment.application.event.SubscriptionOrderEventDto;
import com.lebhas.creativesaas.payment.domain.CreditPurchaseOrder;
import com.lebhas.creativesaas.payment.domain.Invoice;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookLog;
import com.lebhas.creativesaas.payment.domain.SubscriptionOrder;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageResult;
import com.lebhas.pricing.PricingPlan;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentEventProducer {

    private final DomainEventPublisher domainEventPublisher;

    public PaymentEventProducer(DomainEventPublisher domainEventPublisher) {
        this.domainEventPublisher = domainEventPublisher;
    }

    public void paymentTransactionInitiated(PaymentTransaction transaction) {
        publish(KafkaTopicConstants.PAYMENT_TRANSACTION_INITIATED, transactionEvent(transaction));
    }

    public void paymentTransactionSucceeded(PaymentTransaction transaction) {
        publish(KafkaTopicConstants.PAYMENT_TRANSACTION_SUCCEEDED, transactionEvent(transaction));
    }

    public void paymentTransactionFailed(PaymentTransaction transaction) {
        publish(KafkaTopicConstants.PAYMENT_TRANSACTION_FAILED, transactionEvent(transaction));
    }

    public void paymentTransactionCancelled(PaymentTransaction transaction) {
        publish(KafkaTopicConstants.PAYMENT_TRANSACTION_CANCELLED, transactionEvent(transaction));
    }

    public void subscriptionOrderCreated(SubscriptionOrder order, PaymentTransaction transaction, PricingPlan pricingPlan) {
        publish(KafkaTopicConstants.SUBSCRIPTION_ORDER_CREATED, subscriptionOrderEvent(order, transaction, pricingPlan));
    }

    public void subscriptionActivated(SubscriptionOrder order, PaymentTransaction transaction) {
        publish(KafkaTopicConstants.SUBSCRIPTION_ACTIVATED, subscriptionOrderEvent(order, transaction, null));
    }

    public void subscriptionChanged(SubscriptionOrder order, PaymentTransaction transaction, PricingPlan pricingPlan) {
        publish(KafkaTopicConstants.SUBSCRIPTION_CHANGED, subscriptionOrderEvent(order, transaction, pricingPlan));
    }

    public void creditPurchaseCreated(CreditPurchaseOrder order, PaymentTransaction transaction, String creditPackageCode) {
        publish(KafkaTopicConstants.CREDIT_PURCHASE_CREATED, creditPurchaseEvent(order, transaction, creditPackageCode, null));
    }

    public void creditPurchaseCompleted(
            CreditPurchaseOrder order,
            PaymentTransaction transaction,
            CreditUsageResult usageResult
    ) {
        publish(KafkaTopicConstants.CREDIT_PURCHASE_COMPLETED, creditPurchaseEvent(
                order,
                transaction,
                null,
                usageResult == null ? null : usageResult.ledgerId()));
    }

    public void invoiceCreated(Invoice invoice) {
        publish(KafkaTopicConstants.INVOICE_CREATED, invoiceEvent(invoice));
    }

    public void invoicePaid(Invoice invoice) {
        publish(KafkaTopicConstants.INVOICE_PAID, invoiceEvent(invoice));
    }

    public void paymentWebhookReceived(PaymentProvider provider, PaymentWebhookLog log, boolean duplicate) {
        publish(KafkaTopicConstants.PAYMENT_WEBHOOK_RECEIVED, new PaymentWebhookEventDto(
                null,
                log.getId(),
                null,
                provider.getId(),
                provider.getCode(),
                log.getProviderTransactionId(),
                log.getWebhookEventType(),
                log.getVerificationStatus(),
                null,
                log.isProcessed(),
                duplicate,
                log.getFailureReason()
        ));
    }

    public void paymentWebhookProcessed(
            PaymentProvider provider,
            PaymentWebhookLog log,
            PaymentTransaction transaction,
            PaymentWebhookVerificationResult verificationResult,
            boolean duplicate
    ) {
        publish(KafkaTopicConstants.PAYMENT_WEBHOOK_PROCESSED, new PaymentWebhookEventDto(
                transaction == null ? null : transaction.getWorkspaceId(),
                log.getId(),
                transaction == null ? null : transaction.getId(),
                provider.getId(),
                provider.getCode(),
                verificationResult.providerTransactionId(),
                verificationResult.eventType(),
                verificationResult.verificationStatus(),
                transaction == null ? verificationResult.paymentStatus() : transaction.getStatus(),
                log.isProcessed(),
                duplicate,
                verificationResult.failureReason()
        ));
    }

    private PaymentTransactionEventDto transactionEvent(PaymentTransaction transaction) {
        return new PaymentTransactionEventDto(
                transaction.getWorkspaceId(),
                transaction.getId(),
                transaction.getUserId(),
                transaction.getProviderId(),
                transaction.getPaymentPurpose(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getAmount(),
                transaction.getCurrency(),
                transaction.getProviderTransactionId(),
                transaction.getProviderSessionId(),
                transaction.getStatus(),
                transaction.getFailureReason()
        );
    }

    private SubscriptionOrderEventDto subscriptionOrderEvent(
            SubscriptionOrder order,
            PaymentTransaction transaction,
            PricingPlan pricingPlan
    ) {
        return new SubscriptionOrderEventDto(
                order.getWorkspaceId(),
                order.getId(),
                transaction == null ? order.getPaymentTransactionId() : transaction.getId(),
                order.getPricingPlanId(),
                pricingPlan == null ? null : pricingPlan.getCode(),
                order.getRequestedBy(),
                order.getBillingCycle(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getStartsAt(),
                order.getExpiresAt()
        );
    }

    private CreditPurchaseEventDto creditPurchaseEvent(
            CreditPurchaseOrder order,
            PaymentTransaction transaction,
            String creditPackageCode,
            UUID ledgerId
    ) {
        return new CreditPurchaseEventDto(
                order.getWorkspaceId(),
                order.getId(),
                transaction == null ? order.getPaymentTransactionId() : transaction.getId(),
                order.getCreditPackageId(),
                creditPackageCode,
                order.getRequestedBy(),
                order.getCredits(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                ledgerId
        );
    }

    private InvoiceEventDto invoiceEvent(Invoice invoice) {
        return new InvoiceEventDto(
                invoice.getWorkspaceId(),
                invoice.getId(),
                invoice.getPaymentTransactionId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceType(),
                invoice.getAmount(),
                invoice.getCurrency(),
                invoice.getStatus(),
                invoice.getIssuedAt(),
                invoice.getPaidAt()
        );
    }

    private void publish(String topic, PaymentTransactionEventDto event) {
        publish(topic, event.workspaceId(), event.transactionId(), attributes(event));
    }

    private void publish(String topic, SubscriptionOrderEventDto event) {
        publish(topic, event.workspaceId(), event.subscriptionOrderId(), attributes(event));
    }

    private void publish(String topic, CreditPurchaseEventDto event) {
        publish(topic, event.workspaceId(), event.creditPurchaseOrderId(), attributes(event));
    }

    private void publish(String topic, InvoiceEventDto event) {
        publish(topic, event.workspaceId(), event.invoiceId(), attributes(event));
    }

    private void publish(String topic, PaymentWebhookEventDto event) {
        publish(topic, event.workspaceId(), event.webhookLogId(), attributes(event));
    }

    private void publish(String topic, UUID workspaceId, UUID aggregateId, Map<String, Object> attributes) {
        domainEventPublisher.publish(topic, new BaseDomainEvent(
                topic,
                workspaceId,
                aggregateId,
                Instant.now(),
                attributes
        ));
    }

    private Map<String, Object> attributes(Record event) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        for (java.lang.reflect.RecordComponent component : event.getClass().getRecordComponents()) {
            try {
                Object value = component.getAccessor().invoke(event);
                if (value != null) {
                    attributes.put(component.getName(), value);
                }
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Failed to build payment event attributes", exception);
            }
        }
        return attributes;
    }
}
