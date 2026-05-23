package com.lebhas.creativesaas.payment.infrastructure.persistence;

import com.lebhas.creativesaas.payment.domain.CreditPurchaseOrder;
import com.lebhas.creativesaas.payment.domain.PaymentOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditPurchaseOrderRepository extends JpaRepository<CreditPurchaseOrder, UUID> {

    List<CreditPurchaseOrder> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<CreditPurchaseOrder> findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(UUID workspaceId, PaymentOrderStatus status);

    Optional<CreditPurchaseOrder> findByPaymentTransactionId(UUID paymentTransactionId);
}
