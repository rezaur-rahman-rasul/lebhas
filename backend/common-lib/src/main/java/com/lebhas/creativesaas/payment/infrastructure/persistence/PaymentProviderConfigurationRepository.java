package com.lebhas.creativesaas.payment.infrastructure.persistence;

import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentProviderConfigurationRepository extends JpaRepository<PaymentProviderConfiguration, UUID> {

    Optional<PaymentProviderConfiguration> findByProviderIdAndEnvironmentType(UUID providerId, PaymentEnvironmentType environmentType);

    Optional<PaymentProviderConfiguration> findByProviderIdAndEnvironmentTypeAndActiveTrue(UUID providerId, PaymentEnvironmentType environmentType);

    List<PaymentProviderConfiguration> findAllByProviderId(UUID providerId);
}
