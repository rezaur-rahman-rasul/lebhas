package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.payment.application.dto.PaymentProviderConfigurationView;
import com.lebhas.creativesaas.payment.application.dto.PaymentProviderView;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentProviderMapper {

    public PaymentProviderView toProviderView(
            PaymentProvider provider,
            List<PaymentProviderConfiguration> configurations
    ) {
        return new PaymentProviderView(
                provider.getId(),
                provider.getName(),
                provider.getCode(),
                provider.getProviderType(),
                provider.isEnabled(),
                provider.isSandboxEnabled(),
                provider.isLiveEnabled(),
                provider.getPriority(),
                configurations.stream().map(this::toConfigurationView).toList(),
                provider.getCreatedAt(),
                provider.getUpdatedAt()
        );
    }

    public List<PaymentProviderView> toProviderViews(
            List<PaymentProvider> providers,
            Map<UUID, List<PaymentProviderConfiguration>> configurationsByProvider
    ) {
        return providers.stream()
                .map(provider -> toProviderView(provider, configurationsByProvider.getOrDefault(provider.getId(), List.of())))
                .toList();
    }

    public PaymentProviderConfigurationView toConfigurationView(PaymentProviderConfiguration configuration) {
        return new PaymentProviderConfigurationView(
                configuration.getId(),
                configuration.getProviderId(),
                configuration.getEnvironmentType(),
                configuration.getApiBaseUrl(),
                configuration.getMerchantId(),
                configuration.getEncryptedApiKey() != null,
                configuration.getEncryptedSecret() != null,
                configuration.getEncryptedWebhookSecret() != null,
                configuration.getSuccessUrl(),
                configuration.getFailureUrl(),
                configuration.getCancelUrl(),
                configuration.isActive(),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt()
        );
    }
}
