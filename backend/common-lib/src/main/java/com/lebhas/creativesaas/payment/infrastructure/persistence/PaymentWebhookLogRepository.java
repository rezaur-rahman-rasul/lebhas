package com.lebhas.creativesaas.payment.infrastructure.persistence;

import com.lebhas.creativesaas.payment.domain.PaymentWebhookLog;
import com.lebhas.creativesaas.payment.domain.PaymentWebhookVerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, UUID> {

    Optional<PaymentWebhookLog> findByProviderIdAndSignatureHash(UUID providerId, String signatureHash);

    List<PaymentWebhookLog> findAllByProviderIdOrderByCreatedAtDesc(UUID providerId);

    List<PaymentWebhookLog> findAllByProviderIdAndProviderTransactionId(UUID providerId, String providerTransactionId);

    List<PaymentWebhookLog> findAllByProcessedFalseOrderByCreatedAtAsc();

    List<PaymentWebhookLog> findAllByVerificationStatus(PaymentWebhookVerificationStatus verificationStatus);
}
