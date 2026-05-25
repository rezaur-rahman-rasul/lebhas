package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.application.dto.CreditPurchaseOrderView;
import com.lebhas.creativesaas.payment.application.dto.InvoiceView;
import com.lebhas.creativesaas.payment.application.dto.PaymentTransactionView;
import com.lebhas.creativesaas.payment.application.dto.SubscriptionOrderView;
import com.lebhas.creativesaas.payment.domain.CreditPurchaseOrder;
import com.lebhas.creativesaas.payment.domain.Invoice;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.SubscriptionOrder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentApiMapper {

    public SubscriptionOrderView toSubscriptionOrderView(SubscriptionOrder order) {
        return new SubscriptionOrderView(
                order.getId(),
                order.getWorkspaceId(),
                order.getPricingPlanId(),
                order.getRequestedBy(),
                order.getBillingCycle(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getPaymentTransactionId(),
                order.getStartsAt(),
                order.getExpiresAt(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public List<SubscriptionOrderView> toSubscriptionOrderViews(List<SubscriptionOrder> orders) {
        return orders.stream().map(this::toSubscriptionOrderView).toList();
    }

    public CreditPurchaseOrderView toCreditPurchaseOrderView(CreditPurchaseOrder order) {
        return new CreditPurchaseOrderView(
                order.getId(),
                order.getWorkspaceId(),
                order.getCreditPackageId(),
                order.getRequestedBy(),
                order.getCredits(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus(),
                order.getPaymentTransactionId(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    public List<CreditPurchaseOrderView> toCreditPurchaseOrderViews(List<CreditPurchaseOrder> orders) {
        return orders.stream().map(this::toCreditPurchaseOrderView).toList();
    }

    public PaymentTransactionView toPaymentTransactionView(PaymentTransaction transaction) {
        return new PaymentTransactionView(
                transaction.getId(),
                transaction.getWorkspaceId(),
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
                transaction.getFailureReason(),
                transaction.getInitiatedAt(),
                transaction.getCompletedAt(),
                transaction.getCancelledAt(),
                transaction.getCreatedAt(),
                transaction.getUpdatedAt()
        );
    }

    public List<PaymentTransactionView> toPaymentTransactionViews(List<PaymentTransaction> transactions) {
        return transactions.stream().map(this::toPaymentTransactionView).toList();
    }

    public InvoiceView toInvoiceView(Invoice invoice) {
        return new InvoiceView(
                invoice.getId(),
                invoice.getWorkspaceId(),
                invoice.getPaymentTransactionId(),
                invoice.getInvoiceNumber(),
                invoice.getInvoiceType(),
                invoice.getAmount(),
                invoice.getCurrency(),
                invoice.getStatus(),
                invoice.getIssuedAt(),
                invoice.getPaidAt(),
                invoice.getCreatedAt()
        );
    }

    public List<InvoiceView> toInvoiceViews(List<Invoice> invoices) {
        return invoices.stream().map(this::toInvoiceView).toList();
    }
}
