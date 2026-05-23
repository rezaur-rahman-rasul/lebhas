package com.lebhas.creativesaas.payment.infrastructure.persistence;

import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentProviderRepository extends JpaRepository<PaymentProvider, UUID> {

    Optional<PaymentProvider> findByCode(String code);

    List<PaymentProvider> findAllByEnabledTrueOrderByPriorityAscNameAsc();

    List<PaymentProvider> findAllByProviderType(PaymentProviderType providerType);
}
