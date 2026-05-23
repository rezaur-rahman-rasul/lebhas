package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditWalletRepository;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import com.lebhas.creativesaas.redis.RedisWalletCache;
import com.lebhas.creativesaas.usage.application.dto.CreditBalanceView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class CreditBalanceService {

    private static final Duration CREDIT_LOCK_TTL = Duration.ofSeconds(10);

    private final CreditWalletRepository creditWalletRepository;
    private final RedisWalletCache redisWalletCache;
    private final RedisLockService redisLockService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final CreditUsageMapper creditUsageMapper;

    public CreditBalanceService(
            CreditWalletRepository creditWalletRepository,
            RedisWalletCache redisWalletCache,
            RedisLockService redisLockService,
            RedisKeyBuilder redisKeyBuilder,
            CreditUsageMapper creditUsageMapper
    ) {
        this.creditWalletRepository = creditWalletRepository;
        this.redisWalletCache = redisWalletCache;
        this.redisLockService = redisLockService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.creditUsageMapper = creditUsageMapper;
    }

    @Transactional(readOnly = true)
    public CreditBalanceView getBalance(UUID workspaceId) {
        CreditWalletEntity wallet = creditWalletRepository.findByWorkspaceIdAndDeletedFalse(requireWorkspaceId(workspaceId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_WALLET_NOT_FOUND));
        return creditUsageMapper.toBalanceView(wallet);
    }

    public <T> T withCreditLock(UUID workspaceId, Supplier<T> action) {
        RedisLockService.RedisLockToken token = redisLockService.acquire(
                        redisKeyBuilder.lockWallet(requireWorkspaceId(workspaceId)),
                        CREDIT_LOCK_TTL)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_RESERVE_INVALID, "Credit usage lock could not be acquired"));
        try {
            return action.get();
        } finally {
            redisLockService.releaseQuietly(token);
        }
    }

    CreditWalletEntity initializeWallet(UUID workspaceId) {
        return creditWalletRepository.findByWorkspaceIdAndDeletedFalse(requireWorkspaceId(workspaceId))
                .orElseGet(() -> {
                    CreditWalletEntity wallet = creditWalletRepository.save(CreditWalletEntity.initialize(workspaceId));
                    cache(wallet);
                    return wallet;
                });
    }

    BalanceMovement reserve(UUID workspaceId, BigDecimal amount) {
        CreditWalletEntity wallet = initializeWallet(workspaceId);
        BigDecimal before = wallet.getBalance();
        wallet.reserve(amount);
        wallet = creditWalletRepository.save(wallet);
        cache(wallet);
        return new BalanceMovement(wallet, before, wallet.getBalance());
    }

    BalanceMovement finalizeReservation(UUID workspaceId, BigDecimal amount) {
        CreditWalletEntity wallet = creditWalletRepository.findByWorkspaceIdAndDeletedFalse(requireWorkspaceId(workspaceId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_WALLET_NOT_FOUND));
        BigDecimal before = wallet.getBalance();
        wallet.finalizeReservation(amount);
        wallet = creditWalletRepository.save(wallet);
        cache(wallet);
        return new BalanceMovement(wallet, before, wallet.getBalance());
    }

    BalanceMovement refundReservation(UUID workspaceId, BigDecimal amount) {
        CreditWalletEntity wallet = creditWalletRepository.findByWorkspaceIdAndDeletedFalse(requireWorkspaceId(workspaceId))
                .orElseThrow(() -> new BusinessException(ErrorCode.CREDIT_WALLET_NOT_FOUND));
        BigDecimal before = wallet.getBalance();
        wallet.refundReservation(amount);
        wallet = creditWalletRepository.save(wallet);
        cache(wallet);
        return new BalanceMovement(wallet, before, wallet.getBalance());
    }

    private void cache(CreditWalletEntity wallet) {
        redisWalletCache.store(
                wallet.getWorkspaceId(),
                new RedisWalletCache.WalletSnapshot(wallet.getBalance(), wallet.getReservedBalance(), Instant.now()));
    }

    private UUID requireWorkspaceId(UUID workspaceId) {
        if (workspaceId == null) {
            throw new IllegalArgumentException("workspaceId must not be null");
        }
        return workspaceId;
    }

    record BalanceMovement(
            CreditWalletEntity wallet,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter
    ) {
    }
}
