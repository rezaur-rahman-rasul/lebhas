package com.lebhas.creativesaas.asset.application;

import com.lebhas.creativesaas.asset.cache.UploadSessionRedisCacheService;
import com.lebhas.creativesaas.asset.domain.UploadSessionEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.UploadSessionRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UploadSessionService {

    private final UploadSessionRepository uploadSessionRepository;
    private final UploadSessionRedisCacheService uploadSessionRedisCacheService;
    private final AssetUploadStateService assetUploadStateService;
    private final UploadChunkTracker uploadChunkTracker;

    public UploadSessionService(
            UploadSessionRepository uploadSessionRepository,
            UploadSessionRedisCacheService uploadSessionRedisCacheService,
            AssetUploadStateService assetUploadStateService,
            UploadChunkTracker uploadChunkTracker
    ) {
        this.uploadSessionRepository = uploadSessionRepository;
        this.uploadSessionRedisCacheService = uploadSessionRedisCacheService;
        this.assetUploadStateService = assetUploadStateService;
        this.uploadChunkTracker = uploadChunkTracker;
    }

    @Transactional
    public UploadSessionEntity createSession(
            UUID workspaceId,
            UUID brandId,
            UUID productServiceId,
            UUID projectId,
            UUID uploadedBy,
            String originalFileName,
            String mimeType,
            long fileSize,
            String hash,
            int chunkCount
    ) {
        UploadSessionEntity session = uploadSessionRepository.saveAndFlush(UploadSessionEntity.create(
                workspaceId,
                brandId,
                productServiceId,
                projectId,
                uploadedBy,
                originalFileName,
                mimeType,
                fileSize,
                hash,
                chunkCount));
        uploadSessionRedisCacheService.cacheSession(session);
        assetUploadStateService.initialize(session);
        uploadChunkTracker.initialize(session.getId().toString(), session.getChunkCount());
        return session;
    }

    @Transactional(readOnly = true)
    public UploadSessionEntity requireSession(UUID workspaceId, UUID uploadSessionId) {
        return uploadSessionRepository.findByIdAndWorkspaceIdAndDeletedFalse(uploadSessionId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UPLOAD_SESSION_NOT_FOUND));
    }

    @Transactional
    public UploadSessionEntity attachAsset(UploadSessionEntity session, UUID assetId) {
        session.attachAsset(assetId);
        session.markUploading();
        UploadSessionEntity saved = uploadSessionRepository.save(session);
        uploadSessionRedisCacheService.cacheSession(saved);
        assetUploadStateService.markUploading(saved);
        return saved;
    }

    @Transactional
    public UploadSessionEntity markSingleChunkUploaded(UploadSessionEntity session) {
        session.markChunkCompleted(1);
        UploadSessionEntity saved = uploadSessionRepository.save(session);
        uploadSessionRedisCacheService.cacheSession(saved);
        uploadChunkTracker.markChunkUploaded(saved.getId().toString(), 1);
        return saved;
    }

    @Transactional
    public UploadSessionEntity markCompleted(UploadSessionEntity session) {
        session.markCompleted();
        UploadSessionEntity saved = uploadSessionRepository.save(session);
        uploadSessionRedisCacheService.cacheSession(saved);
        assetUploadStateService.markCompleted(saved, saved.getAssetId());
        uploadChunkTracker.markChunkUploaded(saved.getId().toString(), saved.getChunkCount());
        return saved;
    }

    @Transactional
    public UploadSessionEntity markFailed(UploadSessionEntity session, String reason) {
        session.markFailed(reason);
        UploadSessionEntity saved = uploadSessionRepository.save(session);
        uploadSessionRedisCacheService.cacheSession(saved);
        assetUploadStateService.markFailed(saved);
        uploadChunkTracker.markFailed(saved.getId().toString());
        return saved;
    }
}
