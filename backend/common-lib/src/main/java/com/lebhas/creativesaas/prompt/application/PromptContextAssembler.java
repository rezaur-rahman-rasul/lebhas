package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.asset.application.AssetMetadataSerializer;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateEntity;
import com.lebhas.creativesaas.workspace.domain.BrandProfileEntity;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.BrandProfileRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class PromptContextAssembler {

    private final BrandProfileRepository brandProfileRepository;
    private final AssetRepository assetRepository;
    private final AssetMetadataSerializer assetMetadataSerializer;
    private final PromptJsonCodec promptJsonCodec;
    private final PromptTemplateService promptTemplateService;

    public PromptContextAssembler(
            BrandProfileRepository brandProfileRepository,
            AssetRepository assetRepository,
            AssetMetadataSerializer assetMetadataSerializer,
            PromptJsonCodec promptJsonCodec,
            PromptTemplateService promptTemplateService
    ) {
        this.brandProfileRepository = brandProfileRepository;
        this.assetRepository = assetRepository;
        this.assetMetadataSerializer = assetMetadataSerializer;
        this.promptJsonCodec = promptJsonCodec;
        this.promptTemplateService = promptTemplateService;
    }

    public ResolvedPromptContext assemble(
            UUID workspaceId,
            UUID projectId,
            UUID templateId,
            boolean useBrandProfile,
            List<UUID> assetIds
    ) {
        PromptTemplateEntity template = templateId == null ? null : promptTemplateService.requireUsableTemplate(workspaceId, templateId);
        BrandContextSnapshot brandContext = useBrandProfile ? resolveBrandContext(workspaceId) : null;
        List<AssetContext> assets = resolveAssets(workspaceId, projectId, assetIds);
        String brandSnapshotJson = brandContext == null
                ? null
                : promptJsonCodec.write(brandContext.asMap(), ErrorCode.PROMPT_CONTEXT_INVALID, "Brand context snapshot could not be serialized");
        return new ResolvedPromptContext(template, brandContext, assets, brandSnapshotJson);
    }

    private BrandContextSnapshot resolveBrandContext(UUID workspaceId) {
        BrandProfileEntity profile = brandProfileRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_BRAND_CONTEXT_REQUIRED, "Brand profile is required for this request"));
        return new BrandContextSnapshot(
                profile.getBrandName(),
                profile.getBusinessType(),
                profile.getIndustry(),
                profile.getTargetAudience(),
                profile.getBrandVoice(),
                profile.getPreferredCta(),
                profile.getWebsite(),
                profile.getFacebookUrl(),
                profile.getInstagramUrl(),
                profile.getLinkedinUrl(),
                profile.getTiktokUrl(),
                profile.getDescription());
    }

    private List<AssetContext> resolveAssets(UUID workspaceId, UUID projectId, List<UUID> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> uniqueIds = new LinkedHashSet<>(assetIds);
        List<AssetContext> assets = new ArrayList<>(uniqueIds.size());
        for (UUID assetId : uniqueIds) {
            AssetEntity asset = assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(assetId, workspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
            if (projectId != null && !projectId.equals(asset.getProjectId())) {
                throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
            }
            assets.add(new AssetContext(
                    asset.getId(),
                    asset.getOriginalFileName(),
                    asset.getAssetCategory().name(),
                    asset.getFileType() == null ? null : asset.getFileType().name(),
                    asset.getMimeType(),
                    asset.getWidth(),
                    asset.getHeight(),
                    asset.getDuration(),
                    asset.getTags(),
                    assetMetadataSerializer.deserialize(asset.getMetadataJson())));
        }
        return List.copyOf(assets);
    }

    public record ResolvedPromptContext(
            PromptTemplateEntity template,
            BrandContextSnapshot brandContext,
            List<AssetContext> assets,
            String brandContextSnapshotJson
    ) {
    }

    public record BrandContextSnapshot(
            String brandName,
            String businessType,
            String industry,
            String targetAudience,
            String brandVoice,
            String preferredCta,
            String website,
            String facebookUrl,
            String instagramUrl,
            String linkedinUrl,
            String tiktokUrl,
            String description
    ) {
        public Map<String, Object> asMap() {
            Map<String, Object> values = new LinkedHashMap<>();
            put(values, "brandName", brandName);
            put(values, "businessType", businessType);
            put(values, "industry", industry);
            put(values, "targetAudience", targetAudience);
            put(values, "brandVoice", brandVoice);
            put(values, "preferredCta", preferredCta);
            put(values, "website", website);
            put(values, "facebookUrl", facebookUrl);
            put(values, "instagramUrl", instagramUrl);
            put(values, "linkedinUrl", linkedinUrl);
            put(values, "tiktokUrl", tiktokUrl);
            put(values, "description", description);
            return Map.copyOf(values);
        }

        private void put(Map<String, Object> values, String key, String value) {
            if (value != null && !value.isBlank()) {
                values.put(key, value);
            }
        }
    }

    public record AssetContext(
            UUID id,
            String originalFileName,
            String assetCategory,
            String fileType,
            String mimeType,
            Integer width,
            Integer height,
            Long duration,
            Set<String> tags,
            Map<String, Object> metadata
    ) {
    }
}
