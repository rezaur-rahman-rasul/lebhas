package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.asset.application.AssetMetadataSerializer;
import com.lebhas.creativesaas.asset.domain.AssetEntity;
import com.lebhas.creativesaas.asset.domain.AssetStatus;
import com.lebhas.creativesaas.asset.infrastructure.persistence.AssetRepository;
import com.lebhas.creativesaas.asset.storage.StorageService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.generation.application.dto.SubmitCreativeGenerationCommand;
import com.lebhas.creativesaas.generation.domain.CreativeOutputFormat;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.project.application.ProjectService;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryEntity;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptHistoryRepository;
import com.lebhas.creativesaas.workspace.domain.BrandProfileEntity;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.BrandProfileRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class CreativeGenerationContextAssembler {

    private static final int MAX_ASSET_CONTEXT = 12;

    private final PromptHistoryRepository promptHistoryRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final AssetRepository assetRepository;
    private final AssetMetadataSerializer assetMetadataSerializer;
    private final StorageService storageService;
    private final GenerationJsonCodec jsonCodec;
    private final ProjectService projectService;

    public CreativeGenerationContextAssembler(
            PromptHistoryRepository promptHistoryRepository,
            BrandProfileRepository brandProfileRepository,
            AssetRepository assetRepository,
            AssetMetadataSerializer assetMetadataSerializer,
            StorageService storageService,
            GenerationJsonCodec jsonCodec,
            ProjectService projectService
    ) {
        this.promptHistoryRepository = promptHistoryRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.assetRepository = assetRepository;
        this.assetMetadataSerializer = assetMetadataSerializer;
        this.storageService = storageService;
        this.jsonCodec = jsonCodec;
        this.projectService = projectService;
    }

    public CreativeGenerationContext assemble(
            SubmitCreativeGenerationCommand command,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        PromptHistoryEntity promptHistory = resolvePromptHistory(command, access);
        String sourcePrompt = firstText(command.sourcePrompt(), promptHistory == null ? null : promptHistory.getSourcePrompt());
        String enhancedPrompt = firstText(command.enhancedPrompt(), promptHistory == null ? null : promptHistory.getEnhancedPrompt());
        PromptPlatform platform = command.platform() == null && promptHistory != null ? promptHistory.getPlatform() : command.platform();
        CampaignObjective campaignObjective = command.campaignObjective() == null && promptHistory != null
                ? promptHistory.getCampaignObjective()
                : command.campaignObjective();
        PromptLanguage language = command.language() == null && promptHistory != null ? promptHistory.getLanguage() : command.language();
        UUID projectId = promptHistory != null && promptHistory.getProjectId() != null
                ? promptHistory.getProjectId()
                : projectService.requireDefaultProjectId(command.workspaceId());

        Integer width = defaultWidth(command);
        Integer height = defaultHeight(command);
        Long duration = command.duration();
        Map<String, Object> brandContext = command.useBrandContext() ? resolveBrandContext(command.workspaceId()) : Map.of();
        List<Map<String, Object>> assetContext = resolveAssetContext(command.workspaceId(), command.assetIds());
        Map<String, Object> normalizedConfig = normalizeGenerationConfig(command.generationConfig());
        normalizedConfig = withRequestedOutputConfig(normalizedConfig, width, height, duration);

        return new CreativeGenerationContext(
                promptHistory == null ? null : promptHistory.getId(),
                projectId,
                sourcePrompt,
                enhancedPrompt,
                platform,
                campaignObjective,
                command.creativeType(),
                command.outputFormat() == null ? defaultOutputFormat(command.creativeType()) : command.outputFormat(),
                language,
                width,
                height,
                duration,
                jsonCodec.write(brandContext, "Brand context snapshot could not be serialized"),
                jsonCodec.write(assetContext, "Asset context snapshot could not be serialized"),
                jsonCodec.write(normalizedConfig, "Generation config could not be serialized"),
                normalizedConfig,
                assetContext.size());
    }

    private PromptHistoryEntity resolvePromptHistory(
            SubmitCreativeGenerationCommand command,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        if (command.promptHistoryId() == null) {
            return null;
        }
        PromptHistoryEntity promptHistory = promptHistoryRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(command.promptHistoryId(), command.workspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_HISTORY_NOT_FOUND));
        boolean privileged = access.effectiveRole().isMaster() || access.effectiveRole() == Role.ADMIN;
        if (!privileged && !promptHistory.getUserId().equals(access.currentUser().userId())) {
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        return promptHistory;
    }

    private Map<String, Object> resolveBrandContext(UUID workspaceId) {
        BrandProfileEntity profile = brandProfileRepository.findByWorkspaceIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_CONTEXT_INVALID, "Brand profile is required for generation"));
        Map<String, Object> values = new LinkedHashMap<>();
        put(values, "brandName", profile.getBrandName());
        put(values, "businessType", profile.getBusinessType());
        put(values, "industry", profile.getIndustry());
        put(values, "targetAudience", profile.getTargetAudience());
        put(values, "brandVoice", profile.getBrandVoice());
        put(values, "preferredCTA", profile.getPreferredCta());
        put(values, "website", profile.getWebsite());
        put(values, "description", profile.getDescription());
        return Map.copyOf(values);
    }

    private List<Map<String, Object>> resolveAssetContext(UUID workspaceId, List<UUID> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return List.of();
        }
        Set<UUID> uniqueIds = new LinkedHashSet<>(assetIds);
        if (uniqueIds.size() > MAX_ASSET_CONTEXT) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "A generation request can include at most " + MAX_ASSET_CONTEXT + " assets");
        }
        List<Map<String, Object>> assets = new ArrayList<>(uniqueIds.size());
        for (UUID assetId : uniqueIds) {
            AssetEntity asset = assetRepository.findByIdAndWorkspaceIdAndDeletedFalse(assetId, workspaceId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ASSET_NOT_FOUND));
            if (!asset.isReady()) {
                throw new BusinessException(ErrorCode.ASSET_NOT_FOUND);
            }
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("id", asset.getId());
            put(values, "originalFileName", asset.getOriginalFileName());
            values.put("assetCategory", asset.getAssetCategory().name());
            if (asset.getFileType() != null) {
                values.put("fileType", asset.getFileType().name());
            }
            put(values, "mimeType", asset.getMimeType());
            put(values, "fileExtension", asset.getFileExtension());
            put(values, "previewUrl", storageService.generatePreviewUrl(asset).url());
            put(values, "width", asset.getWidth());
            put(values, "height", asset.getHeight());
            put(values, "duration", asset.getDuration());
            values.put("tags", asset.getTags());
            values.put("metadata", assetMetadataSerializer.deserialize(asset.getMetadataJson()));
            assets.add(java.util.Collections.unmodifiableMap(values));
        }
        return List.copyOf(assets);
    }

    private Map<String, Object> normalizeGenerationConfig(Map<String, Object> generationConfig) {
        if (generationConfig == null || generationConfig.isEmpty()) {
            return Map.of();
        }
        if (generationConfig.size() > 50) {
            throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Generation config can contain at most 50 keys");
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        generationConfig.forEach((key, value) -> {
            if (!StringUtils.hasText(key)) {
                throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Generation config keys must not be blank");
            }
            Object normalizedValue = normalizeConfigValue(value);
            normalized.put(key.trim(), normalizedValue);
        });
        return java.util.Collections.unmodifiableMap(normalized);
    }

    private Map<String, Object> withRequestedOutputConfig(Map<String, Object> generationConfig, Integer width, Integer height, Long duration) {
        Map<String, Object> values = new LinkedHashMap<>(generationConfig);
        if (width != null) {
            values.put("width", width);
        }
        if (height != null) {
            values.put("height", height);
        }
        if (duration != null) {
            values.put("duration", duration);
        }
        return java.util.Collections.unmodifiableMap(values);
    }

    private Object normalizeConfigValue(Object value) {
        if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Map<?, ?> map) {
            if (map.size() > 20) {
                throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Nested generation config objects can contain at most 20 keys");
            }
            Map<String, Object> normalized = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> normalized.put(String.valueOf(key), normalizeConfigValue(nestedValue)));
            return java.util.Collections.unmodifiableMap(normalized);
        }
        if (value instanceof List<?> list) {
            if (list.size() > 50) {
                throw new BusinessException(ErrorCode.GENERATION_VALIDATION_FAILED, "Generation config arrays can contain at most 50 values");
            }
            return list.stream().map(this::normalizeConfigValue).toList();
        }
        return String.valueOf(value);
    }

    private CreativeOutputFormat defaultOutputFormat(com.lebhas.creativesaas.generation.domain.CreativeType creativeType) {
        return creativeType != null && creativeType.isVideo() ? CreativeOutputFormat.MP4 : CreativeOutputFormat.PNG;
    }

    private Integer defaultWidth(SubmitCreativeGenerationCommand command) {
        if (command.width() != null) {
            return command.width();
        }
        return command.height() == null && command.creativeType() != null && command.creativeType().isImage() ? 1024 : null;
    }

    private Integer defaultHeight(SubmitCreativeGenerationCommand command) {
        if (command.height() != null) {
            return command.height();
        }
        return command.width() == null && command.creativeType() != null && command.creativeType().isImage() ? 1024 : null;
    }

    private String firstText(String primary, String fallback) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        return StringUtils.hasText(fallback) ? fallback.trim() : null;
    }

    private void put(Map<String, Object> values, String key, String value) {
        if (StringUtils.hasText(value)) {
            values.put(key, value.trim());
        }
    }

    private void put(Map<String, Object> values, String key, Object value) {
        if (value != null) {
            values.put(key, value);
        }
    }
}
