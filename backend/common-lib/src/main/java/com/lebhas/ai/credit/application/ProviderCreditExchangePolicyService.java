package com.lebhas.ai.credit.application;

import com.lebhas.ai.credit.application.dto.ProviderCreditExchangePolicyCommand;
import com.lebhas.ai.credit.application.dto.ProviderCreditExchangePolicyView;
import com.lebhas.ai.credit.domain.ProviderCreditExchangePolicy;
import com.lebhas.ai.credit.infrastructure.persistence.ProviderCreditExchangePolicyRepository;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProviderCreditExchangePolicyService {

    private final ProviderCreditExchangePolicyRepository policyRepository;
    private final AiToolProviderRepository providerRepository;
    private final ProviderCreditMapper mapper;
    private final DomainEventPublisher domainEventPublisher;

    public ProviderCreditExchangePolicyService(
            ProviderCreditExchangePolicyRepository policyRepository,
            AiToolProviderRepository providerRepository,
            ProviderCreditMapper mapper,
            DomainEventPublisher domainEventPublisher
    ) {
        this.policyRepository = policyRepository;
        this.providerRepository = providerRepository;
        this.mapper = mapper;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public ProviderCreditExchangePolicyView createOrReplacePolicy(UUID providerId, ProviderCreditExchangePolicyCommand command) {
        requireProvider(providerId);
        ProviderCreditExchangePolicy policy = policyRepository.findFirstByProviderIdAndDeletedFalseOrderByUpdatedAtDesc(providerId)
                .orElseGet(() -> ProviderCreditExchangePolicy.create(
                        providerId,
                        command.internalCreditPerProviderUnit(),
                        command.freeSignupCreditPercentage(),
                        command.freeSignupCreditEnabled(),
                        command.maxFreeSignupCredits(),
                        command.minProviderBalanceRequired(),
                        command.fallbackFreeCredits(),
                        command.active()));
        if (policy.getId() != null) {
            policy.update(
                    command.internalCreditPerProviderUnit(),
                    command.freeSignupCreditPercentage(),
                    command.freeSignupCreditEnabled(),
                    command.maxFreeSignupCredits(),
                    command.minProviderBalanceRequired(),
                    command.fallbackFreeCredits(),
                    command.active());
        }
        policy = policyRepository.save(policy);
        publishPolicyUpdated(policy);
        return mapper.toPolicyView(policy);
    }

    @Transactional(readOnly = true)
    public ProviderCreditExchangePolicyView getPolicy(UUID providerId) {
        return mapper.toPolicyView(requireLatestPolicy(providerId));
    }

    @Transactional(readOnly = true)
    public Optional<ProviderCreditExchangePolicy> findActivePolicy(UUID providerId) {
        return policyRepository.findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(providerId);
    }

    public ProviderCreditExchangePolicy requireLatestPolicy(UUID providerId) {
        return policyRepository.findFirstByProviderIdAndDeletedFalseOrderByUpdatedAtDesc(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Provider exchange policy not found"));
    }

    private AiToolProvider requireProvider(UUID providerId) {
        return providerRepository.findByIdAndDeletedFalse(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
    }

    private void publishPolicyUpdated(ProviderCreditExchangePolicy policy) {
        domainEventPublisher.publish(KafkaTopicConstants.PROVIDER_EXCHANGE_POLICY_UPDATED, new BaseDomainEvent(
                KafkaTopicConstants.PROVIDER_EXCHANGE_POLICY_UPDATED,
                null,
                policy.getProviderId(),
                Instant.now(),
                Map.of(
                        "providerId", policy.getProviderId().toString(),
                        "freeSignupCreditPercentage", policy.getFreeSignupCreditPercentage(),
                        "freeSignupCreditEnabled", policy.isFreeSignupCreditEnabled(),
                        "active", policy.isActive())));
    }
}
