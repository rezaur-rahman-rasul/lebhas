package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.cache.AssetProcessingRedisCacheService;
import com.lebhas.creativesaas.asset.cache.dto.AssetProcessingStateCacheEntry;
import com.lebhas.creativesaas.asset.domain.PreviewStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class PreviewStateService {

    private final AssetProcessingRedisCacheService assetProcessingRedisCacheService;

    public PreviewStateService(
            AssetProcessingRedisCacheService assetProcessingRedisCacheService
    ) {
        this.assetProcessingRedisCacheService = assetProcessingRedisCacheService;
    }

    public void markPending(UUID assetId) {
        assetProcessingRedisCacheService.markPending(null, assetId);
    }

    public void markProcessing(UUID assetId) {
        assetProcessingRedisCacheService.markProcessing(null, assetId);
    }

    public void markReady(UUID assetId, boolean thumbnailReady) {
        assetProcessingRedisCacheService.markReady(null, assetId, thumbnailReady);
    }

    public void markFailed(UUID assetId, String errorMessage) {
        assetProcessingRedisCacheService.markFailed(null, assetId, errorMessage);
    }

    public Optional<PreviewJobState> get(UUID assetId) {
        return assetProcessingRedisCacheService.get(null, assetId)
                .map(this::toPreviewJobState);
    }

    public void invalidate(UUID assetId) {
        assetProcessingRedisCacheService.invalidate(null, assetId);
    }

    private PreviewJobState toPreviewJobState(AssetProcessingStateCacheEntry entry) {
        return new PreviewJobState(
                entry.assetId(),
                entry.previewStatus() == null ? PreviewStatus.PENDING.name() : entry.previewStatus(),
                entry.startedAt(),
                entry.completedAt(),
                entry.errorMessage(),
                entry.thumbnailReady());
    }

    public record PreviewJobState(
            UUID assetId,
            String previewStatus,
            Instant startedAt,
            Instant completedAt,
            String errorMessage,
            boolean thumbnailReady
    ) {
    }
}
