package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.payment.application.dto.PaymentProviderConfigurationCommand;
import com.lebhas.creativesaas.payment.application.dto.PaymentProviderConfigurationView;
import com.lebhas.creativesaas.payment.cache.PaymentConfigurationCacheService;
import com.lebhas.creativesaas.payment.cache.PaymentProviderCacheService;
import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentProviderConfigurationRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentProviderConfigurationService {

    private final CurrentUserContext currentUserContext;
    private final PaymentProviderRepository providerRepository;
    private final PaymentProviderConfigurationRepository configurationRepository;
    private final PaymentCredentialEncryptionService encryptionService;
    private final PaymentProviderMapper mapper;
    private final PaymentConfigurationCacheService configurationCacheService;
    private final PaymentProviderCacheService providerCacheService;

    public PaymentProviderConfigurationService(
            CurrentUserContext currentUserContext,
            PaymentProviderRepository providerRepository,
            PaymentProviderConfigurationRepository configurationRepository,
            PaymentCredentialEncryptionService encryptionService,
            PaymentProviderMapper mapper,
            PaymentConfigurationCacheService configurationCacheService,
            PaymentProviderCacheService providerCacheService
    ) {
        this.currentUserContext = currentUserContext;
        this.providerRepository = providerRepository;
        this.configurationRepository = configurationRepository;
        this.encryptionService = encryptionService;
        this.mapper = mapper;
        this.configurationCacheService = configurationCacheService;
        this.providerCacheService = providerCacheService;
    }

    @Transactional
    public PaymentProviderConfigurationView createConfiguration(PaymentProviderConfigurationCommand command) {
        requireMaster();
        PaymentProvider provider = requireProvider(command.providerId());
        PaymentEnvironmentType environmentType = requireEnvironment(command.environmentType());
        configurationRepository.findByProviderIdAndEnvironmentType(provider.getId(), environmentType)
                .ifPresent(existing -> {
                    throw new BusinessException(
                            ErrorCode.BUSINESS_RULE_VIOLATION,
                            "Payment provider configuration already exists for this environment"
                    );
                });
        PaymentProviderConfiguration configuration = PaymentProviderConfiguration.create(
                provider.getId(),
                environmentType,
                command.apiBaseUrl(),
                command.merchantId(),
                encryptionService.encryptNullable(command.apiKey(), null),
                encryptionService.encryptNullable(command.secret(), null),
                encryptionService.encryptNullable(command.webhookSecret(), null),
                command.successUrl(),
                command.failureUrl(),
                command.cancelUrl(),
                command.active()
        );
        PaymentProviderConfigurationView view = mapper.toConfigurationView(configurationRepository.save(configuration));
        cacheAndInvalidateProvider(view);
        return view;
    }

    @Transactional
    public PaymentProviderConfigurationView updateConfiguration(PaymentProviderConfigurationCommand command) {
        requireMaster();
        PaymentProviderConfiguration configuration = requireConfiguration(command.configurationId());
        PaymentEnvironmentType environmentType = requireEnvironment(command.environmentType());
        if (command.providerId() != null && !configuration.getProviderId().equals(command.providerId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Payment provider configuration cannot be moved");
        }
        configurationRepository.findByProviderIdAndEnvironmentType(configuration.getProviderId(), environmentType)
                .filter(existing -> !existing.getId().equals(configuration.getId()))
                .ifPresent(existing -> {
                    throw new BusinessException(
                            ErrorCode.BUSINESS_RULE_VIOLATION,
                            "Payment provider configuration already exists for this environment"
                    );
                });
        PaymentEnvironmentType previousEnvironmentType = configuration.getEnvironmentType();
        configuration.update(
                environmentType,
                command.apiBaseUrl(),
                command.merchantId(),
                encryptionService.encryptNullable(command.apiKey(), configuration.getEncryptedApiKey()),
                encryptionService.encryptNullable(command.secret(), configuration.getEncryptedSecret()),
                encryptionService.encryptNullable(command.webhookSecret(), configuration.getEncryptedWebhookSecret()),
                command.successUrl(),
                command.failureUrl(),
                command.cancelUrl(),
                command.active()
        );
        PaymentProviderConfigurationView view = mapper.toConfigurationView(configurationRepository.save(configuration));
        configurationCacheService.invalidateConfiguration(configuration.getProviderId(), previousEnvironmentType);
        cacheAndInvalidateProvider(view);
        return view;
    }

    @Transactional
    public PaymentProviderConfigurationView activateConfiguration(UUID configurationId) {
        requireMaster();
        PaymentProviderConfiguration configuration = requireConfiguration(configurationId);
        configuration.activate();
        PaymentProviderConfigurationView view = mapper.toConfigurationView(configurationRepository.save(configuration));
        cacheAndInvalidateProvider(view);
        return view;
    }

    @Transactional
    public PaymentProviderConfigurationView deactivateConfiguration(UUID configurationId) {
        requireMaster();
        PaymentProviderConfiguration configuration = requireConfiguration(configurationId);
        configuration.deactivate();
        PaymentProviderConfigurationView view = mapper.toConfigurationView(configurationRepository.save(configuration));
        cacheAndInvalidateProvider(view);
        return view;
    }

    private PaymentProvider requireProvider(UUID providerId) {
        if (providerId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Payment provider id is required");
        }
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Payment provider not found"));
    }

    private PaymentProviderConfiguration requireConfiguration(UUID configurationId) {
        if (configurationId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Payment provider configuration id is required");
        }
        return configurationRepository.findById(configurationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Payment provider configuration not found"));
    }

    private PaymentEnvironmentType requireEnvironment(PaymentEnvironmentType environmentType) {
        if (environmentType == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Payment environment type is required");
        }
        return environmentType;
    }

    private CurrentUser requireMaster() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (!currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return currentUser;
    }

    private void cacheAndInvalidateProvider(PaymentProviderConfigurationView view) {
        configurationCacheService.cacheConfiguration(view);
        providerCacheService.invalidateProvider(view.providerId());
    }
}
