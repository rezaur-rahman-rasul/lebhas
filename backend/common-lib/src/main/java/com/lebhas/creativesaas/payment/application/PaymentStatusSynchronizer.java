package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.CreditPurchaseOrder;
import com.lebhas.creativesaas.payment.domain.PaymentOrderStatus;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.payment.domain.SubscriptionOrder;
import com.lebhas.creativesaas.payment.infrastructure.persistence.CreditPurchaseOrderRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.SubscriptionOrderRepository;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import com.lebhas.pricing.WorkspaceSubscriptionStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Service
public class PaymentStatusSynchronizer {

    private final PaymentTransactionService paymentTransactionService;
    private final SubscriptionOrderRepository subscriptionOrderRepository;
    private final CreditPurchaseOrderRepository creditPurchaseOrderRepository;
    private final WorkspaceSubscriptionRepository workspaceSubscriptionRepository;
    private final CreditPurchaseService creditPurchaseService;
    private final InvoiceService invoiceService;
    private final PaymentEventProducer paymentEventProducer;

    public PaymentStatusSynchronizer(
            PaymentTransactionService paymentTransactionService,
            SubscriptionOrderRepository subscriptionOrderRepository,
            CreditPurchaseOrderRepository creditPurchaseOrderRepository,
            WorkspaceSubscriptionRepository workspaceSubscriptionRepository,
            CreditPurchaseService creditPurchaseService,
            InvoiceService invoiceService,
            PaymentEventProducer paymentEventProducer
    ) {
        this.paymentTransactionService = paymentTransactionService;
        this.subscriptionOrderRepository = subscriptionOrderRepository;
        this.creditPurchaseOrderRepository = creditPurchaseOrderRepository;
        this.workspaceSubscriptionRepository = workspaceSubscriptionRepository;
        this.creditPurchaseService = creditPurchaseService;
        this.invoiceService = invoiceService;
        this.paymentEventProducer = paymentEventProducer;
    }

    public PaymentTransaction synchronize(PaymentTransaction transaction, PaymentWebhookVerificationResult verificationResult) {
        PaymentTransactionStatus targetStatus = verificationResult.paymentStatus();
        if (targetStatus == PaymentTransactionStatus.SUCCESS) {
            return synchronizeSuccess(transaction, verificationResult);
        }
        if (targetStatus == PaymentTransactionStatus.CANCELLED) {
            return synchronizeCancelled(transaction);
        }
        if (targetStatus == PaymentTransactionStatus.FAILED || targetStatus == PaymentTransactionStatus.EXPIRED) {
            return synchronizeFailed(transaction, verificationResult.failureReason());
        }
        return transaction;
    }

    private PaymentTransaction synchronizeSuccess(PaymentTransaction transaction, PaymentWebhookVerificationResult verificationResult) {
        if (transaction.getPaymentPurpose() == PaymentPurpose.CREDIT_PURCHASE) {
            creditPurchaseService.applySuccessfulCreditPurchase(
                    transaction.getId(),
                    verificationResult.providerTransactionId(),
                    transaction.getUserId());
            PaymentTransaction refreshed = paymentTransactionService.requireTransaction(transaction.getId());
            invoiceService.markPaid(refreshed.getId());
            return refreshed;
        }

        if (transaction.getStatus() == PaymentTransactionStatus.SUCCESS && subscriptionOrderAlreadyPaid(transaction)) {
            return transaction;
        }
        PaymentTransaction updated = paymentTransactionService.markSuccess(transaction, verificationResult.providerTransactionId());
        if (updated.getPaymentPurpose() == PaymentPurpose.SUBSCRIPTION_PURCHASE
                || updated.getPaymentPurpose() == PaymentPurpose.PLAN_UPGRADE
                || updated.getPaymentPurpose() == PaymentPurpose.PLAN_RENEWAL) {
            activateSubscription(updated);
        }
        invoiceService.markPaid(updated.getId());
        return updated;
    }

    private boolean subscriptionOrderAlreadyPaid(PaymentTransaction transaction) {
        return subscriptionOrderRepository.findByPaymentTransactionId(transaction.getId())
                .map(order -> order.getStatus() == PaymentOrderStatus.PAID)
                .orElse(false);
    }

    private PaymentTransaction synchronizeFailed(PaymentTransaction transaction, String failureReason) {
        if (transaction.getStatus() == PaymentTransactionStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Successful payment cannot be failed by webhook");
        }
        PaymentTransaction updated = paymentTransactionService.markFailed(transaction, failureReason);
        markOrderFailed(updated);
        invoiceService.markCancelled(updated.getId());
        return updated;
    }

    private PaymentTransaction synchronizeCancelled(PaymentTransaction transaction) {
        if (transaction.getStatus() == PaymentTransactionStatus.SUCCESS) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Successful payment cannot be cancelled by webhook");
        }
        PaymentTransaction updated = paymentTransactionService.markCancelled(transaction);
        markOrderCancelled(updated);
        invoiceService.markCancelled(updated.getId());
        return updated;
    }

    private void activateSubscription(PaymentTransaction transaction) {
        SubscriptionOrder order = subscriptionOrderRepository.findByPaymentTransactionId(transaction.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Subscription order not found"));
        if (order.getStatus() == PaymentOrderStatus.PAID) {
            return;
        }
        Instant startsAt = Instant.now();
        Instant expiresAt = expiresAt(startsAt, order.getBillingCycle());
        WorkspaceSubscription subscription = workspaceSubscriptionRepository.findFirstByWorkspaceIdAndDeletedFalse(order.getWorkspaceId())
                .orElseGet(() -> WorkspaceSubscription.create(
                        order.getWorkspaceId(),
                        order.getPricingPlanId(),
                        WorkspaceSubscriptionStatus.ACTIVE,
                        startsAt,
                        expiresAt,
                        null,
                        true));
        subscription.update(
                order.getPricingPlanId(),
                WorkspaceSubscriptionStatus.ACTIVE,
                startsAt,
                expiresAt,
                null,
                subscription.isAutoRenew());
        workspaceSubscriptionRepository.save(subscription);
        order.markPaid(startsAt, expiresAt);
        subscriptionOrderRepository.save(order);
        invoiceService.markPaid(transaction.getId());
        paymentEventProducer.subscriptionActivated(order, transaction);
    }

    private Instant expiresAt(Instant startsAt, BillingCycle billingCycle) {
        ZonedDateTime start = ZonedDateTime.ofInstant(startsAt, ZoneOffset.UTC);
        return switch (billingCycle) {
            case MONTHLY -> start.plusMonths(1).toInstant();
            case YEARLY -> start.plusYears(1).toInstant();
        };
    }

    private void markOrderFailed(PaymentTransaction transaction) {
        if (transaction.getPaymentPurpose() == PaymentPurpose.CREDIT_PURCHASE) {
            creditPurchaseOrderRepository.findByPaymentTransactionId(transaction.getId()).ifPresent(order -> {
                order.markFailed();
                creditPurchaseOrderRepository.save(order);
            });
            return;
        }
        subscriptionOrderRepository.findByPaymentTransactionId(transaction.getId()).ifPresent(order -> {
            order.markFailed();
            subscriptionOrderRepository.save(order);
        });
    }

    private void markOrderCancelled(PaymentTransaction transaction) {
        if (transaction.getPaymentPurpose() == PaymentPurpose.CREDIT_PURCHASE) {
            creditPurchaseOrderRepository.findByPaymentTransactionId(transaction.getId()).ifPresent(order -> {
                order.markCancelled();
                creditPurchaseOrderRepository.save(order);
            });
            return;
        }
        subscriptionOrderRepository.findByPaymentTransactionId(transaction.getId()).ifPresent(order -> {
            order.markCancelled();
            subscriptionOrderRepository.save(order);
        });
    }

}
