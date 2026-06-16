package com.lebhas.creativesaas.generatedversion.application;

import com.lebhas.ai.provider.AiProviderType;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.generation.cache.GeneratedVersionCountCacheService;
import com.lebhas.creativesaas.generation.event.GeneratedVersionCreatedEventDto;
import com.lebhas.creativesaas.generation.event.GenerationEventProducer;
import com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.pricing.application.WorkspacePlanContextService;
import com.lebhas.creativesaas.pricing.application.dto.PlanFeaturePolicyView;
import com.lebhas.creativesaas.pricing.application.dto.WorkspacePlanContextView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class GeneratedVersionService {

    private final GeneratedVersionRepository generatedVersionRepository;
    private final WorkspacePlanContextService workspacePlanContextService;
    private final GeneratedVersionCountCacheService generatedVersionCountCacheService;
    private final GenerationEventProducer generationEventProducer;

    public GeneratedVersionService(
            GeneratedVersionRepository generatedVersionRepository,
            WorkspacePlanContextService workspacePlanContextService,
            GeneratedVersionCountCacheService generatedVersionCountCacheService,
            GenerationEventProducer generationEventProducer
    ) {
        this.generatedVersionRepository = generatedVersionRepository;
        this.workspacePlanContextService = workspacePlanContextService;
        this.generatedVersionCountCacheService = generatedVersionCountCacheService;
        this.generationEventProducer = generationEventProducer;
    }

    @Transactional
    public GeneratedVersionEntity createQueuedPlaceholder(
            CreativeRequestEntity request,
            UUID createdByUserId,
            int versionNumber,
            AiProviderType providerType,
            String model
    ) {
        validateVersionCapacity(request.getWorkspaceId(), request.getId(), 1);
        GeneratedVersionEntity version = GeneratedVersionEntity.create(
                request.getWorkspaceId(),
                request.getId(),
                request.getProjectCampaignId(),
                versionNumber,
                "Version " + versionNumber,
                null,
                null,
                providerType == null ? null : providerType.name(),
                model,
                createdByUserId);
        version = generatedVersionRepository.save(version);
        cacheVersionCount(request.getWorkspaceId(), request.getId());
        publishGeneratedVersionCreated(version);
        return version;
    }

    @Transactional
    public GeneratedVersionEntity createCompleted(
            CreativeRequestEntity request,
            UUID createdByUserId,
            int versionNumber,
            UUID storageFileId,
            UUID assetId,
            UUID previewAssetId,
            UUID thumbnailAssetId,
            String provider,
            String model
    ) {
        validateVersionCapacity(request.getWorkspaceId(), request.getId(), 1);
        GeneratedVersionEntity version = GeneratedVersionEntity.create(
                request.getWorkspaceId(),
                request.getId(),
                request.getProjectCampaignId(),
                versionNumber,
                "Version " + versionNumber,
                storageFileId,
                assetId,
                GenerationStatus.READY,
                ApprovalStatus.NOT_SUBMITTED,
                true,
                provider,
                model,
                createdByUserId,
                com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus.ACTIVE);
        if (assetId != null || previewAssetId != null || thumbnailAssetId != null) {
            version.recordGeneratedAsset(assetId, previewAssetId, thumbnailAssetId, null, null, null, null, null, null);
        }
        version = generatedVersionRepository.save(version);
        cacheVersionCount(request.getWorkspaceId(), request.getId());
        publishGeneratedVersionCreated(version);
        return version;
    }

    @Transactional(readOnly = true)
    public List<GeneratedVersionEntity> listByCreativeRequest(UUID workspaceId, UUID creativeRequestId) {
        return generatedVersionRepository.findAllByWorkspaceIdAndCreativeRequestIdAndDeletedFalseOrderByVersionNumberDesc(
                workspaceId,
                creativeRequestId);
    }

    @Transactional(readOnly = true)
    public List<GeneratedVersionEntity> listByWorkspace(UUID workspaceId) {
        return generatedVersionRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId);
    }

    @Transactional(readOnly = true)
    public Optional<GeneratedVersionEntity> latestByCreativeRequest(UUID workspaceId, UUID creativeRequestId) {
        return generatedVersionRepository.findFirstByWorkspaceIdAndCreativeRequestIdAndDeletedFalseOrderByVersionNumberDesc(
                workspaceId,
                creativeRequestId);
    }

    @Transactional(readOnly = true)
    public Optional<GeneratedVersionEntity> findByIdAndWorkspaceId(UUID workspaceId, UUID generatedVersionId) {
        return generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceId);
    }

    @Transactional(readOnly = true)
    public GeneratedVersionEntity requireByIdAndWorkspaceId(UUID workspaceId, UUID generatedVersionId) {
        return findByIdAndWorkspaceId(workspaceId, generatedVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATED_VERSION_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public int nextVersionNumber(UUID workspaceId, UUID creativeRequestId) {
        return latestByCreativeRequest(workspaceId, creativeRequestId)
                .map(version -> version.getVersionNumber() + 1)
                .orElse(1);
    }

    @Transactional
    public GeneratedVersionEntity save(GeneratedVersionEntity version) {
        GeneratedVersionEntity saved = generatedVersionRepository.save(version);
        cacheVersionCount(saved.getWorkspaceId(), saved.getCreativeRequestId());
        return saved;
    }

    @Transactional(readOnly = true)
    public void validateVersionCapacity(UUID workspaceId, UUID creativeRequestId, int additionalVersions) {
        int normalizedAdditionalVersions = Math.max(1, additionalVersions);
        WorkspacePlanContextView planContext = workspacePlanContextService.getWorkspacePlanContext(workspaceId);
        PlanFeaturePolicyView featurePolicy = planContext.featurePolicy();
        if (featurePolicy == null) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "The active workspace plan feature policy is not available");
        }
        Integer maxGeneratedVersionsPerRequest = featurePolicy.maxGeneratedVersionsPerRequest();
        if (maxGeneratedVersionsPerRequest == null || maxGeneratedVersionsPerRequest < 1) {
            return;
        }
        long existingCount = generatedVersionRepository.countByWorkspaceIdAndCreativeRequestIdAndDeletedFalse(
                workspaceId,
                creativeRequestId);
        if (existingCount + normalizedAdditionalVersions > maxGeneratedVersionsPerRequest) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Generated version count exceeds the current pricing plan limit");
        }
    }

    public void cacheVersionCount(UUID workspaceId, UUID creativeRequestId) {
        long versionCount = generatedVersionRepository.countByWorkspaceIdAndCreativeRequestIdAndDeletedFalse(
                workspaceId,
                creativeRequestId);
        generatedVersionCountCacheService.store(workspaceId, creativeRequestId, Math.toIntExact(versionCount));
    }

    public void publishGeneratedVersionCreated(GeneratedVersionEntity version) {
        generationEventProducer.publishGeneratedVersionCreated(new GeneratedVersionCreatedEventDto(
                version.getWorkspaceId(),
                version.getCreativeRequestId(),
                version.getId(),
                version.getAssetId(),
                version.getStorageFileId(),
                version.getVersionNumber(),
                version.getGenerationStatus().name(),
                null));
    }
}
