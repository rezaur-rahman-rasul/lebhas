package com.lebhas.creativesaas.payment.infrastructure.persistence;

import com.lebhas.creativesaas.payment.domain.Invoice;
import com.lebhas.creativesaas.payment.domain.InvoiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Optional<Invoice> findByPaymentTransactionId(UUID paymentTransactionId);

    List<Invoice> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<Invoice> findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(UUID workspaceId, InvoiceStatus status);
}
