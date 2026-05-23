package com.lebhas.creativesaas.payment.infrastructure.persistence;

import com.lebhas.creativesaas.payment.domain.PaymentOrderStatus;
import com.lebhas.creativesaas.payment.domain.SubscriptionOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionOrderRepository extends JpaRepository<SubscriptionOrder, UUID> {

    List<SubscriptionOrder> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<SubscriptionOrder> findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(UUID workspaceId, PaymentOrderStatus status);

    Optional<SubscriptionOrder> findByPaymentTransactionId(UUID paymentTransactionId);
}
