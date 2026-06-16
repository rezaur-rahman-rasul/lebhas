package com.lebhas.creativesaas.usage.application;

import com.lebhas.ai.credit.application.ProviderCreditExchangePolicyService;
import com.lebhas.ai.credit.application.ProviderCreditPoolService;
import com.lebhas.ai.credit.application.CreditValuePolicyService;
import com.lebhas.ai.credit.domain.CreditValuePolicy;
import com.lebhas.ai.credit.domain.FreeSignupCreditMode;
import com.lebhas.ai.credit.domain.ProviderCreditExchangePolicy;
import com.lebhas.ai.credit.domain.ProviderCreditPool;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.usage.domain.CreditLedger;
import com.lebhas.creativesaas.usage.domain.CreditLedgerTransactionType;
import com.lebhas.creativesaas.usage.infrastructure.persistence.CreditLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AdminFreeCreditAllocationService {

    public static final String FREE_SIGNUP_REFERENCE_TYPE = "FREE_SIGNUP_CREDIT";

    private static final Duration FREE_CREDIT_LOCK_TTL = Duration.ofSeconds(15);

    private final AiToolProviderRepository providerRepository;
    private final ProviderCreditPoolService providerCreditPoolService;
    private final ProviderCreditExchangePolicyService exchangePolicyService;
    private final CreditValuePolicyService creditValuePolicyService;
    private final CreditBalanceService creditBalanceService;
    private final CreditLedgerService creditLedgerService;
    private final CreditLedgerRepository creditLedgerRepository;
    private final RedisLockService redisLockService;
    private final DomainEventPublisher domainEventPublisher;

    public AdminFreeCreditAllocationService(
            AiToolProviderRepository providerRepository,
            ProviderCreditPoolService providerCreditPoolService,
            ProviderCreditExchangePolicyService exchangePolicyService,
            CreditValuePolicyService creditValuePolicyService,
            CreditBalanceService creditBalanceService,
            CreditLedgerService creditLedgerService,
            CreditLedgerRepository creditLedgerRepository,
            RedisLockService redisLockService,
            DomainEventPublisher domainEventPublisher
    ) {
        this.providerRepository = providerRepository;
        this.providerCreditPoolService = providerCreditPoolService;
        this.exchangePolicyService = exchangePolicyService;
        this.creditValuePolicyService = creditValuePolicyService;
        this.creditBalanceService = creditBalanceService;
        this.creditLedgerService = creditLedgerService;
        this.creditLedgerRepository = creditLedgerRepository;
        this.redisLockService = redisLockService;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public void grantForNewWorkspace(UUID workspaceId, UUID userId) {
        require(workspaceId, "workspaceId");
        RedisLockService.RedisLockToken token = redisLockService.acquire("lock:free-credit:" + workspaceId, FREE_CREDIT_LOCK_TTL)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Free signup credit lock could not be acquired"));
        try {
            if (alreadyGranted(workspaceId)) {
                return;
            }
            creditBalanceService.initializeWallet(workspaceId);
            CreditValuePolicy creditValuePolicy = creditValuePolicyService.requireActivePolicy();
            Optional<GrantContext> grantContext = resolveGrantContext();
            BigDecimal providerAvailableCredits = grantContext.map(context -> context.pool().availableInternalCredits()).orElse(zero());
            BigDecimal amount = creditValuePolicy.calculateFreeSignupCredits(providerAvailableCredits);
            if (amount.signum() > 0) {
                CreditBalanceService.BalanceMovement movement = creditBalanceService.purchase(workspaceId, amount);
                creditLedgerService.append(
                        workspaceId,
                        null,
                        null,
                        null,
                        CreditLedgerTransactionType.FREE_SIGNUP_CREDIT_GRANTED,
                        amount,
                        movement.balanceBefore(),
                        movement.balanceAfter(),
                        FREE_SIGNUP_REFERENCE_TYPE,
                        workspaceId,
                        "One-time admin signup free credit",
                        userId);
                if (creditValuePolicy.getFreeSignupMode() == FreeSignupCreditMode.PERCENTAGE_OF_PROVIDER_POOL && grantContext.isPresent()) {
                    GrantContext context = grantContext.orElseThrow();
                    providerCreditPoolService.allocateFreeSignupCredit(
                            context.provider().getId(),
                            amount,
                            FREE_SIGNUP_REFERENCE_TYPE,
                            workspaceId,
                            userId);
                }
            } else {
                creditLedgerService.append(
                        workspaceId,
                        null,
                        null,
                        null,
                        CreditLedgerTransactionType.FREE_SIGNUP_CREDIT_GRANTED,
                        zero(),
                        zero(),
                        zero(),
                        FREE_SIGNUP_REFERENCE_TYPE,
                        workspaceId,
                        "One-time admin signup free credit unavailable",
                        userId);
            }
            publishGranted(workspaceId, userId, amount);
        } finally {
            redisLockService.releaseQuietly(token);
        }
    }

    @Transactional(readOnly = true)
    public boolean alreadyGranted(UUID workspaceId) {
        return creditLedgerRepository.findAllByWorkspaceIdAndReferenceTypeAndReferenceId(
                        workspaceId,
                        FREE_SIGNUP_REFERENCE_TYPE,
                        workspaceId)
                .stream()
                .anyMatch(entry -> entry.getTransactionType() == CreditLedgerTransactionType.FREE_SIGNUP_CREDIT_GRANTED);
    }

    @Transactional(readOnly = true)
    public Optional<Instant> grantedAt(UUID workspaceId) {
        return creditLedgerRepository.findAllByWorkspaceIdAndReferenceTypeAndReferenceId(
                        workspaceId,
                        FREE_SIGNUP_REFERENCE_TYPE,
                        workspaceId)
                .stream()
                .filter(entry -> entry.getTransactionType() == CreditLedgerTransactionType.FREE_SIGNUP_CREDIT_GRANTED)
                .map(CreditLedger::getCreatedAt)
                .findFirst();
    }

    private Optional<GrantContext> resolveGrantContext() {
        List<AiToolProvider> providers = providerRepository.findAllByEnabledTrueAndStatusAndDeletedFalseOrderByProviderNameAsc(ProviderStatus.ACTIVE);
        for (AiToolProvider provider : providers) {
            Optional<ProviderCreditExchangePolicy> policy = exchangePolicyService.findActivePolicy(provider.getId());
            if (policy.isEmpty()) {
                continue;
            }
            try {
                ProviderCreditPool pool = providerCreditPoolService.requirePool(provider.getId());
                return Optional.of(new GrantContext(provider, pool, policy.get()));
            } catch (BusinessException exception) {
                // A provider without a configured pool is skipped; fallback is zero if no provider can allocate.
            }
        }
        return Optional.empty();
    }

    private void publishGranted(UUID workspaceId, UUID userId, BigDecimal amount) {
        domainEventPublisher.publish(KafkaTopicConstants.WORKSPACE_FREE_CREDIT_GRANTED, new BaseDomainEvent(
                KafkaTopicConstants.WORKSPACE_FREE_CREDIT_GRANTED,
                workspaceId,
                workspaceId,
                Instant.now(),
                Map.of(
                        "workspaceId", workspaceId.toString(),
                        "userId", userId == null ? "" : userId.toString(),
                        "creditAmount", amount)));
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(4);
    }

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private record GrantContext(
            AiToolProvider provider,
            ProviderCreditPool pool,
            ProviderCreditExchangePolicy policy
    ) {
    }
}
