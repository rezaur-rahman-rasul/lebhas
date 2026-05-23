package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.campaign.application.ProjectCampaignService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generation.event.CreativeGenerationEventProducer;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementView;
import com.lebhas.creativesaas.prompt.cache.PromptEnhancementCacheService;
import com.lebhas.creativesaas.prompt.cache.dto.PromptEnhancementCacheEntry;
import com.lebhas.creativesaas.prompt.domain.PromptEnhancementHistoryEntity;
import com.lebhas.creativesaas.prompt.domain.PromptEnhancementType;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryEntity;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import com.lebhas.creativesaas.prompt.event.PromptEnhancedEvent;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptEnhancementHistoryRepository;
import com.lebhas.creativesaas.prompt.rate.PromptThrottleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PromptEnhancementService {

    private static final String FOUNDATION_PROVIDER = "INTERNAL_FOUNDATION";
    private static final String FOUNDATION_MODEL = "PROMPT_ENHANCEMENT_RULESET_V1";

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectCampaignService projectCampaignService;
    private final PromptThrottleService promptThrottleService;
    private final PromptContextAssembler promptContextAssembler;
    private final PromptEnhancementCacheService promptEnhancementCacheService;
    private final PromptEnhancementHistoryRepository promptEnhancementHistoryRepository;
    private final PromptHistoryService promptHistoryService;
    private final PromptActivityLogger promptActivityLogger;
    private final PromptJsonCodec promptJsonCodec;
    private final CreativeGenerationEventProducer creativeGenerationEventProducer;
    private final CreativeRequestRepository creativeRequestRepository;

    public PromptEnhancementService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ProjectCampaignService projectCampaignService,
            PromptThrottleService promptThrottleService,
            PromptContextAssembler promptContextAssembler,
            PromptEnhancementCacheService promptEnhancementCacheService,
            PromptEnhancementHistoryRepository promptEnhancementHistoryRepository,
            PromptHistoryService promptHistoryService,
            PromptActivityLogger promptActivityLogger,
            PromptJsonCodec promptJsonCodec,
            CreativeGenerationEventProducer creativeGenerationEventProducer,
            CreativeRequestRepository creativeRequestRepository
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.projectCampaignService = projectCampaignService;
        this.promptThrottleService = promptThrottleService;
        this.promptContextAssembler = promptContextAssembler;
        this.promptEnhancementCacheService = promptEnhancementCacheService;
        this.promptEnhancementHistoryRepository = promptEnhancementHistoryRepository;
        this.promptHistoryService = promptHistoryService;
        this.promptActivityLogger = promptActivityLogger;
        this.promptJsonCodec = promptJsonCodec;
        this.creativeGenerationEventProducer = creativeGenerationEventProducer;
        this.creativeRequestRepository = creativeRequestRepository;
    }

    @Transactional
    public PromptEnhancementView enhance(PromptEnhancementCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requirePromptAccess(command.workspaceId(), command.projectId());
        validatePromptText(command.customPrompt());
        enforceTemplateUsagePermission(access, command.templateId());
        promptThrottleService.assertAllowed(command.workspaceId(), access.currentUser().userId(), command.clientIp(), "enhance");

        CreativeRequestEntity creativeRequest = resolveCreativeRequest(command.workspaceId(), command.projectId(), command.creativeRequestId());
        PromptContextAssembler.ResolvedPromptContext context = promptContextAssembler.assemble(
                command.workspaceId(),
                command.projectId(),
                command.templateId(),
                command.useBrandProfile(),
                command.assetIds());

        String sourcePrompt = command.customPrompt().trim();
        PromptLanguage effectiveLanguage = resolveLanguage(command, creativeRequest, context);
        PromptPlatform effectivePlatform = command.platform() != null
                ? command.platform()
                : creativeRequest == null ? null : creativeRequest.getPlatform();

        promptActivityLogger.logPromptRequest(
                "enhance",
                command.workspaceId(),
                access.currentUser().userId(),
                effectivePlatform,
                command.campaignObjective(),
                effectiveLanguage,
                sourcePrompt.length(),
                context.assets().size(),
                command.useBrandProfile(),
                Set.of(SuggestionType.ENHANCEMENT),
                sourcePrompt);

        try {
            String promptHash = buildPromptHash(command, context, creativeRequest, sourcePrompt, effectiveLanguage);
            PromptEnhancementView result = promptEnhancementCacheService.get(promptHash)
                    .map(this::toView)
                    .orElseGet(() -> {
                        PromptEnhancementView generated = buildFoundationEnhancement(command, context, creativeRequest, sourcePrompt, effectiveLanguage);
                        promptEnhancementCacheService.store(generated, promptHash);
                        return generated;
                    });

            PromptHistoryEntity promptHistory = promptHistoryService.recordSuccess(
                    command.workspaceId(),
                    command.projectId(),
                    creativeRequest == null ? null : creativeRequest.getId(),
                    access.currentUser().userId(),
                    sourcePrompt,
                    promptJsonCodec.write(result, ErrorCode.PROMPT_CONTEXT_INVALID, "Prompt enhancement result could not be serialized"),
                    effectiveLanguage,
                    effectivePlatform,
                    command.campaignObjective(),
                    command.businessType(),
                    context.brandContextSnapshotJson(),
                    SuggestionType.ENHANCEMENT,
                    result.aiProvider(),
                    result.aiModel(),
                    result.tokenUsage());

            createEnhancementHistoryIfPresent(creativeRequest, sourcePrompt, result.enhancedPrompt());
            creativeGenerationEventProducer.publishPromptEnhanced(new PromptEnhancedEvent(
                    null,
                    null,
                    command.workspaceId(),
                    promptHistory.getId(),
                    access.currentUser().userId(),
                    sourcePrompt,
                    result.enhancedPrompt(),
                    result.aiProvider(),
                    result.aiModel(),
                    result.tokenUsage()));
            promptActivityLogger.logPromptCompleted(
                    "enhance",
                    command.workspaceId(),
                    access.currentUser().userId(),
                    result.aiProvider(),
                    result.aiModel(),
                    result.tokenUsage());
            return result;
        } catch (RuntimeException exception) {
            recordFailureQuietly(command, access.currentUser().userId(), context.brandContextSnapshotJson(), effectiveLanguage);
            promptActivityLogger.logProviderFailure("enhance", command.workspaceId(), access.currentUser().userId(), FOUNDATION_PROVIDER, exception.getMessage());
            throw exception;
        }
    }

    private WorkspaceAuthorizationService.WorkspaceAccess requirePromptAccess(UUID workspaceId, UUID projectId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.PROMPT_INTELLIGENCE_USE);
        if (projectId != null) {
            projectCampaignService.requireProjectCampaign(workspaceId, projectId);
        }
        return access;
    }

    private void validatePromptText(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "customPrompt must be provided");
        }
        int length = prompt.trim().length();
        if (length < 5 || length > 5000) {
            throw new BusinessException(ErrorCode.PROMPT_LENGTH_INVALID, "customPrompt must be between 5 and 5000 characters");
        }
    }

    private void enforceTemplateUsagePermission(WorkspaceAuthorizationService.WorkspaceAccess access, UUID templateId) {
        if (templateId == null) {
            return;
        }
        if (access.effectiveRole().isMaster()
                || access.permissions().contains(Permission.PROMPT_TEMPLATE_VIEW)
                || access.permissions().contains(Permission.PROMPT_TEMPLATE_MANAGE)) {
            return;
        }
        promptActivityLogger.logAuthorizationFailure("enhance", access.workspace().getId(), access.currentUser().userId(), "missing_prompt_template_view_permission");
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private CreativeRequestEntity resolveCreativeRequest(UUID workspaceId, UUID projectId, UUID creativeRequestId) {
        if (creativeRequestId == null) {
            return null;
        }
        CreativeRequestEntity request = creativeRequestRepository
                .findByIdAndWorkspaceIdAndDeletedFalse(creativeRequestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND));
        if (projectId != null && !projectId.equals(request.getProjectCampaignId())) {
            throw new BusinessException(ErrorCode.CREATIVE_REQUEST_FOUNDATION_NOT_FOUND);
        }
        return request;
    }

    private PromptLanguage resolveLanguage(
            PromptEnhancementCommand command,
            CreativeRequestEntity creativeRequest,
            PromptContextAssembler.ResolvedPromptContext context
    ) {
        if (command.language() != null) {
            return command.language();
        }
        if (creativeRequest != null && creativeRequest.getLanguagePreference() != null) {
            return switch (creativeRequest.getLanguagePreference()) {
                case BANGLA -> PromptLanguage.BANGLA;
                case ENGLISH -> PromptLanguage.ENGLISH;
                case BOTH -> PromptLanguage.MIXED;
            };
        }
        if (context.template() != null && context.template().getLanguage() != null) {
            return context.template().getLanguage();
        }
        return PromptLanguage.MIXED;
    }

    private String buildPromptHash(
            PromptEnhancementCommand command,
            PromptContextAssembler.ResolvedPromptContext context,
            CreativeRequestEntity creativeRequest,
            String sourcePrompt,
            PromptLanguage effectiveLanguage
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", command.workspaceId());
        payload.put("projectId", command.projectId());
        payload.put("creativeRequestId", creativeRequest == null ? null : creativeRequest.getId());
        payload.put("sourcePrompt", sourcePrompt);
        payload.put("templateId", command.templateId());
        payload.put("templateBody", context.template() == null ? null : context.template().getTemplateBody());
        payload.put("businessType", normalize(command.businessType()));
        payload.put("campaignObjective", enumName(command.campaignObjective()));
        payload.put("platform", enumName(command.platform()));
        payload.put("creativeStyle", enumName(command.creativeStyle()));
        payload.put("language", enumName(effectiveLanguage));
        payload.put("tone", enumName(command.tone()));
        payload.put("targetAudience", normalize(command.targetAudience()));
        payload.put("offerDetails", normalize(command.offerDetails()));
        payload.put("ctaPreference", normalize(command.ctaPreference()));
        payload.put("useBrandProfile", command.useBrandProfile());
        payload.put("brandContextSnapshot", context.brandContextSnapshotJson());
        payload.put("assetIds", command.assetIds() == null ? List.of() : List.copyOf(command.assetIds()));
        return promptEnhancementCacheService.sha256(
                promptJsonCodec.write(payload, ErrorCode.PROMPT_CONTEXT_INVALID, "Prompt enhancement cache payload could not be serialized"));
    }

    private PromptEnhancementView toView(PromptEnhancementCacheEntry entry) {
        return new PromptEnhancementView(
                entry.enhancedPrompt(),
                entry.reasoningSummary(),
                entry.suggestedMissingFields() == null ? List.of() : List.copyOf(entry.suggestedMissingFields()),
                entry.aiProvider(),
                entry.aiModel(),
                entry.tokenUsage());
    }

    private PromptEnhancementView buildFoundationEnhancement(
            PromptEnhancementCommand command,
            PromptContextAssembler.ResolvedPromptContext context,
            CreativeRequestEntity creativeRequest,
            String sourcePrompt,
            PromptLanguage effectiveLanguage
    ) {
        List<String> missingFields = new ArrayList<>();
        collectMissingField(missingFields, "businessType", command.businessType());
        collectMissingField(missingFields, "campaignObjective", enumName(command.campaignObjective()));
        collectMissingField(missingFields, "targetAudience", command.targetAudience());
        collectMissingField(missingFields, "offerDetails", command.offerDetails());
        collectMissingField(missingFields, "ctaPreference", command.ctaPreference());

        StringBuilder builder = new StringBuilder();
        builder.append("Create a ");
        if (command.creativeStyle() != null) {
            builder.append(command.creativeStyle().name().toLowerCase(Locale.ROOT).replace('_', ' ')).append(' ');
        }
        builder.append("marketing creative");
        appendSentence(builder, "Language", humanizeLanguage(effectiveLanguage));
        appendSentence(builder, "Platform", humanize(command.platform()));
        appendSentence(builder, "Objective", humanize(command.campaignObjective()));
        appendSentence(builder, "Target audience", normalize(command.targetAudience()));
        appendSentence(builder, "Offer details", normalize(command.offerDetails()));
        appendSentence(builder, "CTA preference", normalize(command.ctaPreference()));
        if (creativeRequest != null) {
            appendSentence(builder, "Requested versions", String.valueOf(creativeRequest.getRequestedVersions()));
        }
        if (context.template() != null) {
            appendSentence(builder, "Template guidance", context.template().getTemplateBody());
        }
        if (context.brandContext() != null && !context.brandContext().asMap().isEmpty()) {
            appendSentence(builder, "Brand context", summarizeBrandContext(context.brandContext().asMap()));
        }
        if (!context.assets().isEmpty()) {
            appendSentence(builder, "Asset context", summarizeAssets(context.assets()));
        }
        appendSentence(builder, "Core prompt", sourcePrompt);

        String reasoningSummary = missingFields.isEmpty()
                ? "Expanded the prompt with available template, campaign, and workspace context."
                : "Expanded the prompt with the available context and flagged the missing fields that would improve downstream generation quality.";
        return new PromptEnhancementView(
                builder.toString().trim(),
                reasoningSummary,
                List.copyOf(missingFields),
                FOUNDATION_PROVIDER,
                FOUNDATION_MODEL,
                null);
    }

    private void createEnhancementHistoryIfPresent(
            CreativeRequestEntity creativeRequest,
            String originalPrompt,
            String enhancedPrompt
    ) {
        if (creativeRequest == null) {
            return;
        }
        promptEnhancementHistoryRepository.save(PromptEnhancementHistoryEntity.create(
                creativeRequest.getId(),
                originalPrompt,
                enhancedPrompt,
                PromptEnhancementType.ENHANCE));
    }

    private void recordFailureQuietly(
            PromptEnhancementCommand command,
            UUID actorUserId,
            String brandContextSnapshot,
            PromptLanguage effectiveLanguage
    ) {
        try {
            promptHistoryService.recordFailure(
                    command.workspaceId(),
                    command.projectId(),
                    command.creativeRequestId(),
                    actorUserId,
                    command.customPrompt(),
                    effectiveLanguage,
                    command.platform(),
                    command.campaignObjective(),
                    command.businessType(),
                    brandContextSnapshot,
                    SuggestionType.ENHANCEMENT,
                    FOUNDATION_PROVIDER,
                    FOUNDATION_MODEL);
        } catch (RuntimeException ignored) {
        }
    }

    private void collectMissingField(List<String> missingFields, String fieldName, String value) {
        if (!StringUtils.hasText(value)) {
            missingFields.add(fieldName);
        }
    }

    private void appendSentence(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (!builder.isEmpty() && builder.charAt(builder.length() - 1) != ' ') {
            builder.append(' ');
        }
        builder.append(label).append(": ").append(value.trim()).append('.');
    }

    private String summarizeBrandContext(Map<String, Object> context) {
        return context.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .limit(6)
                .reduce((left, right) -> left + "; " + right)
                .orElse("");
    }

    private String summarizeAssets(List<PromptContextAssembler.AssetContext> assets) {
        return assets.stream()
                .limit(4)
                .map(asset -> asset.originalFileName() + " (" + asset.assetCategory() + ")")
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private String humanize(Enum<?> value) {
        if (value == null) {
            return null;
        }
        return value.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String humanizeLanguage(PromptLanguage language) {
        if (language == null) {
            return null;
        }
        return switch (language) {
            case BANGLA -> "Bangla";
            case ENGLISH -> "English";
            case MIXED -> "Bangla and English";
        };
    }
}
