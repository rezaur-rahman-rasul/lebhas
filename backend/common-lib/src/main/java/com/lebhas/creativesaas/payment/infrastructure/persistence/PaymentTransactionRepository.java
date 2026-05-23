package com.lebhas.creativesaas.payment.infrastructure.persistence;

import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    List<PaymentTransaction> findAllByWorkspaceIdAndStatusOrderByCreatedAtDesc(UUID workspaceId, PaymentTransactionStatus status);

    List<PaymentTransaction> findAllByPaymentPurposeAndStatus(PaymentPurpose paymentPurpose, PaymentTransactionStatus status);

    Optional<PaymentTransaction> findByProviderIdAndProviderTransactionId(UUID providerId, String providerTransactionId);

    Optional<PaymentTransaction> findByProviderIdAndProviderSessionId(UUID providerId, String providerSessionId);

    List<PaymentTransaction> findAllByReferenceTypeAndReferenceId(String referenceType, UUID referenceId);
}
