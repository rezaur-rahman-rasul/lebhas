package com.lebhas.creativesaas.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "invoices",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_invoices_invoice_number", columnNames = "invoice_number"),
        indexes = {
                @Index(name = "idx_invoices_workspace_created_at", columnList = "workspace_id,created_at"),
                @Index(name = "idx_invoices_payment_transaction_id", columnList = "payment_transaction_id"),
                @Index(name = "idx_invoices_invoice_type_status", columnList = "invoice_type,status"),
                @Index(name = "idx_invoices_issued_at", columnList = "issued_at")
        })
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "payment_transaction_id", nullable = false)
    private UUID paymentTransactionId;

    @Column(name = "invoice_number", nullable = false, length = 80)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false, length = 40)
    private InvoiceType invoiceType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceStatus status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Invoice() {
    }

    public static Invoice create(
            UUID workspaceId,
            UUID paymentTransactionId,
            String invoiceNumber,
            InvoiceType invoiceType,
            BigDecimal amount,
            String currency,
            InvoiceStatus status,
            Instant issuedAt,
            Instant paidAt
    ) {
        Invoice invoice = new Invoice();
        invoice.workspaceId = PaymentTransaction.require(workspaceId, "workspaceId");
        invoice.paymentTransactionId = PaymentTransaction.require(paymentTransactionId, "paymentTransactionId");
        invoice.invoiceNumber = PaymentTransaction.normalizeRequired(invoiceNumber, "invoiceNumber");
        invoice.invoiceType = PaymentTransaction.require(invoiceType, "invoiceType");
        invoice.amount = PaymentTransaction.normalizeMoney(amount, "amount");
        invoice.currency = PaymentTransaction.normalizeCurrency(currency);
        invoice.status = status == null ? InvoiceStatus.ISSUED : status;
        invoice.issuedAt = issuedAt == null ? Instant.now() : issuedAt;
        invoice.paidAt = paidAt;
        return invoice;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getPaymentTransactionId() {
        return paymentTransactionId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public InvoiceType getInvoiceType() {
        return invoiceType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
