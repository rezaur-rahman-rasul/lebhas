package com.lebhas.creativesaas.generation.cache;

import com.lebhas.creativesaas.credit.domain.CreditReservationEntity;
import com.lebhas.creativesaas.credit.domain.CreditReservationStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class CreditReservationCacheService {

    private final GenerationRedisKeys redisKeys;
    private final GenerationRedisAccessSupport redisAccessSupport;
    private final GenerationRedisTtlStrategy ttlStrategy;

    public CreditReservationCacheService(
            GenerationRedisKeys redisKeys,
            GenerationRedisAccessSupport redisAccessSupport,
            GenerationRedisTtlStrategy ttlStrategy
    ) {
        this.redisKeys = redisKeys;
        this.redisAccessSupport = redisAccessSupport;
        this.ttlStrategy = ttlStrategy;
    }

    public Optional<CreditReservationCacheEntry> get(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.read(
                redisKeys.creditReservation(creativeRequestId),
                CreditReservationCacheEntry.class,
                "credit_reservation_get",
                GenerationRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public boolean store(CreditReservationEntity entity) {
        return store(CreditReservationCacheEntry.from(entity));
    }

    public boolean store(CreditReservationCacheEntry entry) {
        if (entry == null || entry.creativeRequestId() == null) {
            return false;
        }
        return redisAccessSupport.write(
                redisKeys.creditReservation(entry.creativeRequestId()),
                entry,
                ttlStrategy.creditReservationTtl(),
                "credit_reservation_put",
                GenerationRedisOperationContext.request(entry.workspaceId(), entry.creativeRequestId()));
    }

    public boolean invalidate(UUID workspaceId, UUID creativeRequestId) {
        return redisAccessSupport.delete(
                redisKeys.creditReservation(creativeRequestId),
                "credit_reservation_delete",
                GenerationRedisOperationContext.request(workspaceId, creativeRequestId));
    }

    public record CreditReservationCacheEntry(
            UUID id,
            UUID workspaceId,
            UUID creativeRequestId,
            BigDecimal reservedCredits,
            BigDecimal finalizedCredits,
            BigDecimal refundedCredits,
            CreditReservationStatus status,
            Instant createdAt,
            Instant finalizedAt,
            Instant cachedAt
    ) {
        public static CreditReservationCacheEntry from(CreditReservationEntity entity) {
            if (entity == null) {
                return null;
            }
            return new CreditReservationCacheEntry(
                    entity.getId(),
                    entity.getWorkspaceId(),
                    entity.getCreativeRequestId(),
                    entity.getReservedCredits(),
                    entity.getFinalizedCredits(),
                    entity.getRefundedCredits(),
                    entity.getStatus(),
                    entity.getCreatedAt(),
                    entity.getFinalizedAt(),
                    Instant.now());
        }
    }
}
