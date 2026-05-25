package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.domain.Invoice;
import com.lebhas.creativesaas.payment.domain.InvoiceStatus;
import com.lebhas.creativesaas.payment.domain.InvoiceType;
import com.lebhas.creativesaas.payment.infrastructure.persistence.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class InvoiceService {

    private static final DateTimeFormatter INVOICE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC);

    private final InvoiceRepository invoiceRepository;
    private final PaymentEventProducer paymentEventProducer;

    public InvoiceService(InvoiceRepository invoiceRepository, PaymentEventProducer paymentEventProducer) {
        this.invoiceRepository = invoiceRepository;
        this.paymentEventProducer = paymentEventProducer;
    }

    public Invoice issueSubscriptionInvoice(
            UUID workspaceId,
            UUID paymentTransactionId,
            BigDecimal amount,
            String currency
    ) {
        return issueInvoice(workspaceId, paymentTransactionId, InvoiceType.SUBSCRIPTION, amount, currency);
    }

    public Invoice issueCreditPurchaseInvoice(
            UUID workspaceId,
            UUID paymentTransactionId,
            BigDecimal amount,
            String currency
    ) {
        return issueInvoice(workspaceId, paymentTransactionId, InvoiceType.CREDIT_PURCHASE, amount, currency);
    }

    private Invoice issueInvoice(
            UUID workspaceId,
            UUID paymentTransactionId,
            InvoiceType invoiceType,
            BigDecimal amount,
            String currency
    ) {
        Invoice invoice = Invoice.create(
                workspaceId,
                paymentTransactionId,
                nextInvoiceNumber(invoiceType, paymentTransactionId),
                invoiceType,
                amount,
                currency,
                InvoiceStatus.ISSUED,
                Instant.now(),
                null
        );
        Invoice saved = invoiceRepository.save(invoice);
        paymentEventProducer.invoiceCreated(saved);
        return saved;
    }

    public java.util.Optional<Invoice> findByPaymentTransactionId(UUID paymentTransactionId) {
        if (paymentTransactionId == null) {
            return java.util.Optional.empty();
        }
        return invoiceRepository.findByPaymentTransactionId(paymentTransactionId);
    }

    public java.util.Optional<Invoice> markPaid(UUID paymentTransactionId) {
        return findByPaymentTransactionId(paymentTransactionId)
                .map(invoice -> {
                    boolean alreadyPaid = invoice.getStatus() == InvoiceStatus.PAID;
                    invoice.markPaid();
                    Invoice saved = invoiceRepository.save(invoice);
                    if (!alreadyPaid) {
                        paymentEventProducer.invoicePaid(saved);
                    }
                    return saved;
                });
    }

    public java.util.Optional<Invoice> markCancelled(UUID paymentTransactionId) {
        return findByPaymentTransactionId(paymentTransactionId)
                .map(invoice -> {
                    invoice.markCancelled();
                    return invoiceRepository.save(invoice);
                });
    }

    private String nextInvoiceNumber(InvoiceType invoiceType, UUID paymentTransactionId) {
        String prefix = invoiceType == InvoiceType.CREDIT_PURCHASE ? "INV-CRD-" : "INV-SUB-";
        return prefix + INVOICE_DATE.format(Instant.now()) + "-" + paymentTransactionId.toString().substring(0, 8).toUpperCase();
    }
}
