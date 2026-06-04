package com.lebhas.ai.credit.application;

import com.lebhas.ai.credit.application.dto.ProviderCreditLedgerView;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolAdjustmentCommand;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolCommand;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolView;
import com.lebhas.ai.credit.domain.ProviderCreditLedger;
import com.lebhas.ai.credit.domain.ProviderCreditPool;
import com.lebhas.ai.credit.domain.ProviderCreditTransactionType;
import com.lebhas.ai.credit.infrastructure.persistence.ProviderCreditLedgerRepository;
import com.lebhas.ai.credit.infrastructure.persistence.ProviderCreditPoolRepository;
import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.infrastructure.persistence.AiToolProviderRepository;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.data.domain.Pageable;
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
public class ProviderCreditPoolService {

    private static final Duration PROVIDER_POOL_LOCK_TTL = Duration.ofSeconds(10);

    private final ProviderCreditPoolRepository poolRepository;
    private final ProviderCreditLedgerRepository ledgerRepository;
    private final AiToolProviderRepository providerRepository;
    private final ProviderCreditMapper mapper;
    private final RedisLockService redisLockService;
    private final CurrentUserContext currentUserContext;
    private final DomainEventPublisher domainEventPublisher;

    public ProviderCreditPoolService(
            ProviderCreditPoolRepository poolRepository,
            ProviderCreditLedgerRepository ledgerRepository,
            AiToolProviderRepository providerRepository,
            ProviderCreditMapper mapper,
            RedisLockService redisLockService,
            CurrentUserContext currentUserContext,
            DomainEventPublisher domainEventPublisher
    ) {
        this.poolRepository = poolRepository;
        this.ledgerRepository = ledgerRepository;
        this.providerRepository = providerRepository;
        this.mapper = mapper;
        this.redisLockService = redisLockService;
        this.currentUserContext = currentUserContext;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public ProviderCreditPoolView createOrReplacePool(UUID providerId, ProviderCreditPoolCommand command) {
        requireProvider(providerId);
        return withProviderPoolLock(providerId, () -> {
            ProviderCreditPool pool = poolRepository.findFirstByProviderIdAndDeletedFalseOrderByUpdatedAtDesc(providerId)
                    .orElseGet(() -> ProviderCreditPool.create(
                            providerId,
                            command.currency(),
                            command.providerBalanceAmount(),
                            command.internalCreditEquivalent(),
                            command.lowBalanceThreshold()));
            BigDecimal before = pool.availableInternalCredits();
            if (pool.getId() != null) {
                pool.update(command.currency(), command.providerBalanceAmount(), command.internalCreditEquivalent(), command.lowBalanceThreshold());
            }
            pool = poolRepository.save(pool);
            appendLedger(pool, ProviderCreditTransactionType.PROVIDER_BALANCE_SET, command.internalCreditEquivalent(),
                    before, pool.availableInternalCredits(), "PROVIDER_CREDIT_POOL", pool.getId(), "Provider credit pool set", currentUserId());
            publishPoolUpdated(pool);
            return mapper.toPoolView(pool);
        });
    }

    @Transactional(readOnly = true)
    public ProviderCreditPoolView getPool(UUID providerId) {
        return mapper.toPoolView(requirePool(providerId));
    }

    @Transactional
    public ProviderCreditPoolView adjustPool(UUID providerId, ProviderCreditPoolAdjustmentCommand command) {
        requireProvider(providerId);
        return withProviderPoolLock(providerId, () -> {
            ProviderCreditPool pool = requirePool(providerId);
            BigDecimal before = pool.availableInternalCredits();
            pool.adjustInternalEquivalent(command.amount());
            pool = poolRepository.save(pool);
            appendLedger(pool, ProviderCreditTransactionType.PROVIDER_BALANCE_ADJUSTED, command.amount(),
                    before, pool.availableInternalCredits(), command.referenceType(), command.referenceId(), command.description(), currentUserId());
            publishPoolUpdated(pool);
            return mapper.toPoolView(pool);
        });
    }

    @Transactional(readOnly = true)
    public List<ProviderCreditPoolView> listPools() {
        return poolRepository.findAllByDeletedFalseOrderByUpdatedAtDesc()
                .stream()
                .map(mapper::toPoolView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResult<ProviderCreditLedgerView> listLedger(UUID providerId, Pageable pageable) {
        requireProvider(providerId);
        return PagedResult.from(ledgerRepository.findAllByProviderIdOrderByCreatedAtDesc(providerId, pageable)
                .map(mapper::toLedgerView));
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findReservedProviderId(String referenceType, UUID referenceId) {
        if (referenceType == null || referenceId == null) {
            return Optional.empty();
        }
        return ledgerRepository.findAllByReferenceTypeAndReferenceId(referenceType, referenceId)
                .stream()
                .filter(entry -> entry.getTransactionType() == ProviderCreditTransactionType.PROVIDER_CREDIT_RESERVED)
                .map(ProviderCreditLedger::getProviderId)
                .findFirst();
    }

    @Transactional
    public void allocateFreeSignupCredit(UUID providerId, BigDecimal amount, String referenceType, UUID referenceId, UUID createdBy) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        withProviderPoolLock(providerId, () -> {
            ProviderCreditPool pool = requirePool(providerId);
            BigDecimal before = pool.availableInternalCredits();
            pool.allocateFreeCredit(amount);
            pool = poolRepository.save(pool);
            appendLedger(pool, ProviderCreditTransactionType.FREE_SIGNUP_CREDIT_GRANTED, amount,
                    before, pool.availableInternalCredits(), referenceType, referenceId, "Free signup provider credit allocation", createdBy);
            publishPoolUpdated(pool);
            return null;
        });
    }

    @Transactional
    public void reserveProviderCredits(UUID providerId, BigDecimal amount, String referenceType, UUID referenceId, UUID createdBy) {
        withProviderPoolLock(providerId, () -> {
            ProviderCreditPool pool = requirePool(providerId);
            BigDecimal before = pool.availableInternalCredits();
            pool.reserve(amount);
            pool = poolRepository.save(pool);
            appendLedger(pool, ProviderCreditTransactionType.PROVIDER_CREDIT_RESERVED, amount,
                    before, pool.availableInternalCredits(), referenceType, referenceId, "Provider credits reserved", createdBy);
            publishProviderCreditEvent(KafkaTopicConstants.PROVIDER_CREDIT_RESERVED, pool, amount, referenceType, referenceId);
            return null;
        });
    }

    @Transactional
    public void useReservedProviderCredits(UUID providerId, BigDecimal amount, String referenceType, UUID referenceId, UUID createdBy) {
        withProviderPoolLock(providerId, () -> {
            ProviderCreditPool pool = requirePool(providerId);
            BigDecimal before = pool.availableInternalCredits();
            pool.useReserved(amount);
            pool = poolRepository.save(pool);
            appendLedger(pool, ProviderCreditTransactionType.PROVIDER_CREDIT_USED, amount,
                    before, pool.availableInternalCredits(), referenceType, referenceId, "Provider credits used", createdBy);
            publishProviderCreditEvent(KafkaTopicConstants.PROVIDER_CREDIT_USED, pool, amount, referenceType, referenceId);
            return null;
        });
    }

    @Transactional
    public void releaseReservedProviderCredits(UUID providerId, BigDecimal amount, String referenceType, UUID referenceId, UUID createdBy) {
        withProviderPoolLock(providerId, () -> {
            ProviderCreditPool pool = requirePool(providerId);
            BigDecimal before = pool.availableInternalCredits();
            pool.releaseReserved(amount);
            pool = poolRepository.save(pool);
            appendLedger(pool, ProviderCreditTransactionType.PROVIDER_CREDIT_RELEASED, amount,
                    before, pool.availableInternalCredits(), referenceType, referenceId, "Provider credits released", createdBy);
            publishProviderCreditEvent(KafkaTopicConstants.PROVIDER_CREDIT_RELEASED, pool, amount, referenceType, referenceId);
            return null;
        });
    }

    public ProviderCreditPool requirePool(UUID providerId) {
        return poolRepository.findFirstByProviderIdAndDeletedFalseOrderByUpdatedAtDesc(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Provider credit pool not found"));
    }

    private ProviderCreditLedger appendLedger(
            ProviderCreditPool pool,
            ProviderCreditTransactionType transactionType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String referenceType,
            UUID referenceId,
            String description,
            UUID createdBy
    ) {
        return ledgerRepository.save(ProviderCreditLedger.create(
                pool.getProviderId(),
                transactionType,
                amount,
                balanceBefore,
                balanceAfter,
                referenceType,
                referenceId,
                description,
                createdBy));
    }

    private <T> T withProviderPoolLock(UUID providerId, java.util.function.Supplier<T> action) {
        RedisLockService.RedisLockToken token = redisLockService.acquire("lock:provider-credit-pool:" + providerId, PROVIDER_POOL_LOCK_TTL)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Provider credit pool lock could not be acquired"));
        try {
            return action.get();
        } finally {
            redisLockService.releaseQuietly(token);
        }
    }

    private AiToolProvider requireProvider(UUID providerId) {
        return providerRepository.findByIdAndDeletedFalse(providerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "AI provider not found"));
    }

    private UUID currentUserId() {
        return currentUserContext.getCurrentUser().map(user -> user.userId()).orElse(null);
    }

    private void publishPoolUpdated(ProviderCreditPool pool) {
        publishProviderCreditEvent(KafkaTopicConstants.PROVIDER_CREDIT_POOL_UPDATED, pool, pool.availableInternalCredits(), "PROVIDER_CREDIT_POOL", pool.getId());
    }

    private void publishProviderCreditEvent(String topic, ProviderCreditPool pool, BigDecimal amount, String referenceType, UUID referenceId) {
        domainEventPublisher.publish(topic, new BaseDomainEvent(
                topic,
                null,
                pool.getProviderId(),
                Instant.now(),
                Map.of(
                        "providerId", pool.getProviderId().toString(),
                        "amount", amount,
                        "availableInternalCredits", pool.availableInternalCredits(),
                        "referenceType", referenceType == null ? "" : referenceType,
                        "referenceId", referenceId == null ? "" : referenceId.toString())));
    }
}
