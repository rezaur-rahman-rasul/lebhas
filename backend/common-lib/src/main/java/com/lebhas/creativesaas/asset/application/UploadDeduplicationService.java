package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.redis.RedisKeyBuilder;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UploadDeduplicationService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final RedisLockService redisLockService;
    private final RedisKeyBuilder redisKeyBuilder;
    private final AssetUploadStateService assetUploadStateService;

    public UploadDeduplicationService(
            RedisLockService redisLockService,
            RedisKeyBuilder redisKeyBuilder,
            AssetUploadStateService assetUploadStateService
    ) {
        this.redisLockService = redisLockService;
        this.redisKeyBuilder = redisKeyBuilder;
        this.assetUploadStateService = assetUploadStateService;
    }

    public RedisLockService.RedisLockToken acquire(String sha256) {
        return redisLockService.acquire(redisKeyBuilder.lockUpload(sha256), LOCK_TTL)
                .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_DUPLICATE_UPLOAD_IN_PROGRESS));
    }

    public void release(RedisLockService.RedisLockToken token) {
        redisLockService.release(token);
    }

    public java.util.Optional<java.util.UUID> findRememberedDuplicate(String sha256) {
        return assetUploadStateService.findDuplicate(sha256);
    }

    public void rememberDuplicate(String sha256, java.util.UUID assetId, java.util.UUID workspaceId, java.util.UUID projectId) {
        assetUploadStateService.rememberDuplicate(sha256, assetId, workspaceId, projectId);
    }
}
