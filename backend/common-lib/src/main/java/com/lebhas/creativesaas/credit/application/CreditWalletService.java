package com.lebhas.creativesaas.credit.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.credit.application.dto.CreditWalletView;
import com.lebhas.creativesaas.credit.domain.CreditTransactionEntity;
import com.lebhas.creativesaas.credit.domain.CreditTransactionStatus;
import com.lebhas.creativesaas.credit.domain.CreditTransactionType;
import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditTransactionRepository;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditWalletRepository;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisWalletCache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreditWalletService {

    private final CreditWalletRepository creditWalletRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final CreditWalletViewMapper creditWalletViewMapper;
    private final RedisWalletCache redisWalletCache;
    private final RedisLockService redisLockService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final DomainEventPublisher domainEventPublisher;

    public CreditWalletService(
            CreditWalletRepository creditWalletRepository,
            CreditTransactionRepository creditTransactionRepository,
            CreditWalletViewMapper creditWalletViewMapper,
            RedisWalletCache redisWalletCache,
            RedisLockService redisLockService,
            RedisKeyBuilder redisKeyBuilder,
            DomainEventPublisher domainEventPublisher
    ) {
        this.creditWalletRepository = creditWalletRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.creditWalletViewMapper = creditWalletViewMapper;
        this.redisWalletCache = redisWalletCache;
        this.redisLockService = redisLockService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public CreditWalletEntity initializeWallet(UUID workspaceId) {
        return creditWalletRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
                .orElseGet(() -> {
                    CreditWalletEntity wallet = creditWalletRepository.save(CreditWalletEntity.initialize(workspaceId));
                    cache(wallet);
                    return wallet;
                });
    }

    @Transactional(readOnly = true)
    public CreditWalletView getWallet(UUID workspaceId) {
        CreditWalletEntity wallet = creditWalletRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_WALLET_NOT_FOUND));
        return creditWalletViewMapper.toView(wallet);
    }

    @Transactional
    public CreditTransactionEntity purchase(UUID workspaceId, BigDecimal amount, String referenceType, UUID referenceId) {
        return withWalletLock(workspaceId, () -> {
            CreditWalletEntity wallet = initializeWallet(workspaceId);
            wallet.addBalance(amount);
            creditWalletRepository.save(wallet);
            cache(wallet);
            return creditTransactionRepository.save(CreditTransactionEntity.create(
                    workspaceId,
                    CreditTransactionType.PURCHASE,
                    amount,
                    referenceType,
                    referenceId,
                    CreditTransactionStatus.COMPLETED));
        });
    }

    @Transactional
    public CreditTransactionEntity reserve(UUID workspaceId, BigDecimal amount, String referenceType, UUID referenceId) {
        CreditTransactionEntity transaction = withWalletLock(workspaceId, () -> {
            CreditWalletEntity wallet = initializeWallet(workspaceId);
            wallet.reserve(amount);
            creditWalletRepository.save(wallet);
            cache(wallet);
            return creditTransactionRepository.save(CreditTransactionEntity.create(
                    workspaceId,
                    CreditTransactionType.RESERVE,
                    amount,
                    referenceType,
                    referenceId,
                    CreditTransactionStatus.COMPLETED));
        });
        publish(KafkaTopicConstants.CREDIT_RESERVED, workspaceId, transaction.getId(), transaction.getAmount(), transaction.getReferenceType());
        return transaction;
    }

    @Transactional
    public CreditTransactionEntity finalizeReservation(UUID workspaceId, BigDecimal amount, String referenceType, UUID referenceId) {
        CreditTransactionEntity transaction = withWalletLock(workspaceId, () -> {
            CreditWalletEntity wallet = creditWalletRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_WALLET_NOT_FOUND));
            wallet.finalizeReservation(amount);
            creditWalletRepository.save(wallet);
            cache(wallet);
            return creditTransactionRepository.save(CreditTransactionEntity.create(
                    workspaceId,
                    CreditTransactionType.FINALIZE,
                    amount,
                    referenceType,
                    referenceId,
                    CreditTransactionStatus.COMPLETED));
        });
        publish(KafkaTopicConstants.CREDIT_FINALIZED, workspaceId, transaction.getId(), transaction.getAmount(), transaction.getReferenceType());
        return transaction;
    }

    @Transactional
    public CreditTransactionEntity refund(UUID workspaceId, BigDecimal amount, String referenceType, UUID referenceId) {
        CreditTransactionEntity transaction = withWalletLock(workspaceId, () -> {
            CreditWalletEntity wallet = creditWalletRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_WALLET_NOT_FOUND));
            wallet.refundReservation(amount);
            creditWalletRepository.save(wallet);
            cache(wallet);
            return creditTransactionRepository.save(CreditTransactionEntity.create(
                    workspaceId,
                    CreditTransactionType.REFUND,
                    amount,
                    referenceType,
                    referenceId,
                    CreditTransactionStatus.COMPLETED));
        });
        publish(KafkaTopicConstants.CREDIT_REFUNDED, workspaceId, transaction.getId(), transaction.getAmount(), transaction.getReferenceType());
        return transaction;
    }

    private void cache(CreditWalletEntity wallet) {
        redisWalletCache.store(
                wallet.getWorkspaceId(),
                new RedisWalletCache.WalletSnapshot(wallet.getBalance(), wallet.getReservedBalance(), Instant.now()));
    }

    private void publish(String topic, UUID workspaceId, UUID aggregateId, BigDecimal amount, String referenceType) {
        domainEventPublisher.publish(topic, new BaseDomainEvent(
                topic,
                workspaceId,
                aggregateId,
                Instant.now(),
                Map.of(
                        "amount", amount,
                        "referenceType", Optional.ofNullable(referenceType).orElse("UNKNOWN"))));
    }

    private <T> T withWalletLock(UUID workspaceId, java.util.concurrent.Callable<T> action) {
        RedisLockService.RedisLockToken token = redisLockService.acquire(
                        redisKeyBuilder.lockWallet(workspaceId),
                        java.time.Duration.ofSeconds(5))
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Wallet lock could not be acquired"));
        try {
            return action.call();
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, exception.getMessage());
        } finally {
            redisLockService.release(token);
        }
    }
}
