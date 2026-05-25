package com.lebhas.creativesaas.payment.cache;

import com.lebhas.creativesaas.payment.application.dto.CreditPackageView;
import com.lebhas.creativesaas.redis.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreditPackageCacheService {

    private static final Logger log = LoggerFactory.getLogger(CreditPackageCacheService.class);

    private final RedisCacheService redisCacheService;
    private final PaymentRedisTtlStrategy ttlStrategy;

    public CreditPackageCacheService(RedisCacheService redisCacheService, PaymentRedisTtlStrategy ttlStrategy) {
        this.redisCacheService = redisCacheService;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<CreditPackageView> getCreditPackage(UUID creditPackageId) {
        return read(PaymentRedisKeys.creditPackage(creditPackageId), CreditPackageView.class, "get_credit_package");
    }

    public void cacheCreditPackage(CreditPackageView creditPackage) {
        if (creditPackage == null || creditPackage.id() == null) {
            return;
        }
        String key = PaymentRedisKeys.creditPackage(creditPackage.id());
        try {
            redisCacheService.set(key, creditPackage, ttlStrategy.creditPackageTtl());
        } catch (RuntimeException exception) {
            logFailure("cache_credit_package", key, exception);
        }
    }

    public Optional<List<CreditPackageView>> getActiveCreditPackages() {
        return read(PaymentRedisKeys.activeCreditPackages(), CreditPackageView[].class, "get_active_credit_packages")
                .map(Arrays::asList);
    }

    public void cacheActiveCreditPackages(List<CreditPackageView> creditPackages) {
        String key = PaymentRedisKeys.activeCreditPackages();
        try {
            redisCacheService.set(key, creditPackages == null ? List.of() : creditPackages, ttlStrategy.activeCreditPackagesTtl());
        } catch (RuntimeException exception) {
            logFailure("cache_active_credit_packages", key, exception);
        }
    }

    public void invalidateCreditPackage(UUID creditPackageId) {
        if (creditPackageId != null) {
            delete(PaymentRedisKeys.creditPackage(creditPackageId), "invalidate_credit_package");
        }
        invalidateActiveCreditPackages();
    }

    public void invalidateActiveCreditPackages() {
        delete(PaymentRedisKeys.activeCreditPackages(), "invalidate_active_credit_packages");
    }

    private <T> Optional<T> read(String key, Class<T> type, String operation) {
        try {
            return redisCacheService.get(key, type);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
            return Optional.empty();
        }
    }

    private void delete(String key, String operation) {
        try {
            redisCacheService.delete(key);
        } catch (RuntimeException exception) {
            logFailure(operation, key, exception);
        }
    }

    private void logFailure(String operation, String key, RuntimeException exception) {
        String reason = exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage().replaceAll("\\s+", " ").trim();
        log.warn("payment_redis_failure operation={} key={} reason={}", operation, key, reason);
    }
}
