package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUser;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.payment.application.dto.PaymentProviderCommand;
import com.lebhas.creativesaas.payment.application.dto.PaymentProviderView;
import com.lebhas.creativesaas.payment.cache.PaymentProviderCacheService;
import com.lebhas.creativesaas.payment.domain.PaymentProvider;
import com.lebhas.creativesaas.payment.domain.PaymentProviderConfiguration;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentProviderConfigurationRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentProviderRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PaymentProviderManagementService {

    private final CurrentUserContext currentUserContext;
    private final PaymentProviderRepository providerRepository;
    private final PaymentProviderConfigurationRepository configurationRepository;
    private final PaymentProviderMapper mapper;
    private final PaymentProviderCacheService paymentProviderCacheService;

    public PaymentProviderManagementService(
            CurrentUserContext currentUserContext,
            PaymentProviderRepository providerRepository,
            PaymentProviderConfigurationRepository configurationRepository,
            PaymentProviderMapper mapper,
            PaymentProviderCacheService paymentProviderCacheService
    ) {
        this.currentUserContext = currentUserContext;
        this.providerRepository = providerRepository;
        this.configurationRepository = configurationRepository;
        this.mapper = mapper;
        this.paymentProviderCacheService = paymentProviderCacheService;
    }

    @Transactional
    public PaymentProviderView createProvider(PaymentProviderCommand command) {
        requireMaster();
        validateUniqueCode(command.code(), null);
        PaymentProvider provider = PaymentProvider.create(
                command.name(),
                command.code(),
                command.providerType(),
                command.enabled(),
                command.sandboxEnabled(),
                command.liveEnabled(),
                command.priority()
        );
        provider = providerRepository.save(provider);
        PaymentProviderView view = mapper.toProviderView(provider, List.of());
        paymentProviderCacheService.cacheProvider(view);
        paymentProviderCacheService.invalidateActiveProvider();
        return view;
    }

    @Transactional
    public PaymentProviderView updateProvider(PaymentProviderCommand command) {
        requireMaster();
        PaymentProvider provider = requireProvider(command.providerId());
        validateUniqueCode(command.code(), provider.getId());
        provider.update(
                command.name(),
                command.code(),
                command.providerType(),
                command.sandboxEnabled(),
                command.liveEnabled(),
                command.priority()
        );
        if (command.enabled()) {
            provider.enable();
        } else {
            provider.disable();
        }
        return saveAndView(provider);
    }

    @Transactional
    public PaymentProviderView enableProvider(UUID providerId) {
        requireMaster();
        PaymentProvider provider = requireProvider(providerId);
        provider.enable();
        return saveAndView(provider);
    }

    @Transactional
    public PaymentProviderView disableProvider(UUID providerId) {
        requireMaster();
        PaymentProvider provider = requireProvider(providerId);
        provider.disable();
        return saveAndView(provider);
    }

    @Transactional
    public PaymentProviderView prioritizeProvider(UUID providerId, int priority) {
        requireMaster();
        PaymentProvider provider = requireProvider(providerId);
        provider.updatePriority(priority);
        return saveAndView(provider);
    }

    @Transactional(readOnly = true)
    public List<PaymentProviderView> listProviders() {
        requireMaster();
        List<PaymentProvider> providers = providerRepository.findAll(Sort.by(
                Sort.Order.asc("priority"),
                Sort.Order.asc("name")
        ));
        Map<UUID, List<PaymentProviderConfiguration>> configurationsByProvider = providers.stream()
                .collect(Collectors.toMap(
                        PaymentProvider::getId,
                        provider -> configurationRepository.findAllByProviderId(provider.getId()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        return mapper.toProviderViews(providers, configurationsByProvider);
    }

    @Transactional(readOnly = true)
    public PaymentProviderView getProvider(UUID providerId) {
        requireMaster();
        PaymentProvider provider = requireProvider(providerId);
        return mapper.toProviderView(provider, configurationRepository.findAllByProviderId(provider.getId()));
    }

    @Transactional(readOnly = true)
    PaymentProvider requireProvider(UUID providerId) {
        if (providerId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Payment provider id is required");
        }
        return providerRepository.findById(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Payment provider not found"));
    }

    private PaymentProviderView saveAndView(PaymentProvider provider) {
        provider = providerRepository.save(provider);
        PaymentProviderView view = mapper.toProviderView(provider, configurationRepository.findAllByProviderId(provider.getId()));
        paymentProviderCacheService.cacheProvider(view);
        paymentProviderCacheService.invalidateActiveProvider();
        return view;
    }

    private void validateUniqueCode(String code, UUID currentProviderId) {
        String normalizedCode = normalizeCode(code);
        providerRepository.findByCode(normalizedCode)
                .filter(existing -> !existing.getId().equals(currentProviderId))
                .ifPresent(existing -> {
                    throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Payment provider code is already in use");
                });
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Payment provider code is required");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private CurrentUser requireMaster() {
        CurrentUser currentUser = currentUserContext.requireCurrentUser();
        if (!currentUser.isMaster()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return currentUser;
    }
}
