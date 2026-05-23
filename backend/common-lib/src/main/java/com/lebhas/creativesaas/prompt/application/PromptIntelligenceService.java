package com.lebhas.creativesaas.prompt.application;

import com.lebhas.ai.cache.AiPromptResponseRedisCacheService;
import com.lebhas.ai.cache.PromptResponseCacheEntry;
import com.lebhas.creativesaas.campaign.application.ProjectCampaignService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.generation.event.CreativeGenerationEventProducer;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementView;
import com.lebhas.creativesaas.prompt.application.dto.PromptRewriteCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptRewriteView;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionListView;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionsView;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryEntity;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.PromptSuggestionEntity;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import com.lebhas.creativesaas.prompt.event.PromptEnhancedEvent;
import com.lebhas.creativesaas.prompt.event.PromptSuggestionGeneratedEvent;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptSuggestionRepository;
import com.lebhas.creativesaas.prompt.provider.AiProviderRouter;
import com.lebhas.creativesaas.prompt.provider.AiResponse;
import com.lebhas.creativesaas.prompt.rate.PromptThrottleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class PromptIntelligenceService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectCampaignService projectCampaignService;
    private final PromptThrottleService promptThrottleService;
    private final PromptContextAssembler promptContextAssembler;
    private final PromptInstructionFactory promptInstructionFactory;
    private final AiProviderRouter aiProviderRouter;
    private final PromptResponseParser promptResponseParser;
    private final PromptHistoryService promptHistoryService;
    private final PromptActivityLogger promptActivityLogger;
    private final PromptJsonCodec promptJsonCodec;
    private final CreativeGenerationEventProducer creativeGenerationEventProducer;
    private final AiPromptResponseRedisCacheService aiPromptResponseRedisCacheService;
    private final PromptSuggestionRepository promptSuggestionRepository;

    public PromptIntelligenceService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ProjectCampaignService projectCampaignService,
            PromptThrottleService promptThrottleService,
            PromptContextAssembler promptContextAssembler,
            PromptInstructionFactory promptInstructionFactory,
            AiProviderRouter aiProviderRouter,
            PromptResponseParser promptResponseParser,
            PromptHistoryService promptHistoryService,
            PromptActivityLogger promptActivityLogger,
            PromptJsonCodec promptJsonCodec,
            CreativeGenerationEventProducer creativeGenerationEventProducer,
            AiPromptResponseRedisCacheService aiPromptResponseRedisCacheService,
            PromptSuggestionRepository promptSuggestionRepository
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.projectCampaignService = projectCampaignService;
        this.promptThrottleService = promptThrottleService;
        this.promptContextAssembler = promptContextAssembler;
        this.promptInstructionFactory = promptInstructionFactory;
        this.aiProviderRouter = aiProviderRouter;
        this.promptResponseParser = promptResponseParser;
        this.promptHistoryService = promptHistoryService;
        this.promptActivityLogger = promptActivityLogger;
        this.promptJsonCodec = promptJsonCodec;
        this.creativeGenerationEventProducer = creativeGenerationEventProducer;
        this.aiPromptResponseRedisCacheService = aiPromptResponseRedisCacheService;
        this.promptSuggestionRepository = promptSuggestionRepository;
    }

    @Transactional
    public PromptEnhancementView enhance(PromptEnhancementCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requirePromptAccess(command.workspaceId(), command.projectId());
        validatePromptText(command.customPrompt(), "customPrompt");
        enforceTemplateUsagePermission(access, command.templateId(), "enhance");
        promptThrottleService.assertAllowed(command.workspaceId(), access.currentUser().userId(), command.clientIp(), "enhance");

        PromptContextAssembler.ResolvedPromptContext context = promptContextAssembler.assemble(
                command.workspaceId(),
                command.projectId(),
                command.templateId(),
                command.useBrandProfile(),
                command.assetIds());
        promptActivityLogger.logPromptRequest(
                "enhance",
                command.workspaceId(),
                access.currentUser().userId(),
                command.platform(),
                command.campaignObjective(),
                command.language(),
                command.customPrompt().trim().length(),
                context.assets().size(),
                command.useBrandProfile(),
                Set.of(SuggestionType.ENHANCEMENT),
                command.customPrompt());

        String sourcePrompt = command.customPrompt().trim();
        String promptHash = promptHash("enhance", command.workspaceId(), command.projectId(), enhancementCachePayload(command, context, sourcePrompt));
        Optional<PromptEnhancementView> cached = cachedView(promptHash, PromptEnhancementView.class);
        if (cached.isPresent()) {
            PromptEnhancementView result = cached.get();
            PromptHistoryEntity history = promptHistoryService.recordSuccess(
                    command.workspaceId(),
                    command.projectId(),
                    null,
                    access.currentUser().userId(),
                    sourcePrompt,
                    promptJsonCodec.write(result, ErrorCode.PROMPT_CONTEXT_INVALID, "Prompt enhancement result could not be persisted"),
                    command.language(),
                    command.platform(),
                    command.campaignObjective(),
                    command.businessType(),
                    context.brandContextSnapshotJson(),
                    SuggestionType.ENHANCEMENT,
                    result.aiProvider(),
                    result.aiModel(),
                    result.tokenUsage());
            publishPromptEnhanced(command.workspaceId(), history.getId(), access.currentUser().userId(), sourcePrompt, result);
            promptActivityLogger.logPromptCompleted("enhance", command.workspaceId(), access.currentUser().userId(), result.aiProvider(), result.aiModel(), result.tokenUsage());
            return result;
        }

        try {
            AiResponse aiResponse = aiProviderRouter.generate(promptInstructionFactory.buildEnhancementRequest(command, context));
            PromptEnhancementView result = promptResponseParser.parseEnhancement(
                    aiResponse.content(),
                    aiResponse.provider(),
                    aiResponse.model(),
                    aiResponse.tokenUsage());
            cacheView(promptHash, result, result.aiProvider(), result.aiModel());
            PromptHistoryEntity history = promptHistoryService.recordSuccess(
                    command.workspaceId(),
                    command.projectId(),
                    null,
                    access.currentUser().userId(),
                    sourcePrompt,
                    promptJsonCodec.write(result, ErrorCode.PROMPT_CONTEXT_INVALID, "Prompt enhancement result could not be persisted"),
                    command.language(),
                    command.platform(),
                    command.campaignObjective(),
                    command.businessType(),
                    context.brandContextSnapshotJson(),
                    SuggestionType.ENHANCEMENT,
                    aiResponse.provider(),
                    aiResponse.model(),
                    aiResponse.tokenUsage());
            publishPromptEnhanced(command.workspaceId(), history.getId(), access.currentUser().userId(), sourcePrompt, result);
            promptActivityLogger.logPromptCompleted("enhance", command.workspaceId(), access.currentUser().userId(), aiResponse.provider(), aiResponse.model(), aiResponse.tokenUsage());
            return result;
        } catch (RuntimeException exception) {
            recordFailureQuietly(
                    command.workspaceId(),
                    command.projectId(),
                    access.currentUser().userId(),
                    sourcePrompt,
                    command.language(),
                    command.platform(),
                    command.campaignObjective(),
                    command.businessType(),
                    context.brandContextSnapshotJson(),
                    SuggestionType.ENHANCEMENT);
            promptActivityLogger.logProviderFailure("enhance", command.workspaceId(), access.currentUser().userId(), aiProviderRouter.activeProviderName(), exception.getMessage());
            throw exception;
        }
    }

    @Transactional
    public PromptRewriteView rewrite(PromptRewriteCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requirePromptAccess(command.workspaceId(), command.projectId());
        validatePromptText(command.existingPrompt(), "existingPrompt");
        enforceTemplateUsagePermission(access, command.templateId(), "rewrite");
        promptThrottleService.assertAllowed(command.workspaceId(), access.currentUser().userId(), command.clientIp(), "rewrite");

        PromptContextAssembler.ResolvedPromptContext context = promptContextAssembler.assemble(
                command.workspaceId(),
                command.projectId(),
                command.templateId(),
                command.useBrandProfile(),
                command.assetIds());
        promptActivityLogger.logPromptRequest(
                "rewrite",
                command.workspaceId(),
                access.currentUser().userId(),
                command.platform(),
                command.campaignObjective(),
                command.language(),
                command.existingPrompt().trim().length(),
                context.assets().size(),
                command.useBrandProfile(),
                Set.of(SuggestionType.REWRITE),
                command.existingPrompt());

        String sourcePrompt = command.existingPrompt().trim();
        try {
            AiResponse aiResponse = aiProviderRouter.generate(promptInstructionFactory.buildRewriteRequest(command, context));
            PromptRewriteView result = promptResponseParser.parseRewrite(
                    aiResponse.content(),
                    aiResponse.provider(),
                    aiResponse.model(),
                    aiResponse.tokenUsage());
            promptHistoryService.recordSuccess(
                    command.workspaceId(),
                    command.projectId(),
                    null,
                    access.currentUser().userId(),
                    sourcePrompt,
                    promptJsonCodec.write(result, ErrorCode.PROMPT_CONTEXT_INVALID, "Prompt rewrite result could not be persisted"),
                    command.language(),
                    command.platform(),
                    command.campaignObjective(),
                    command.businessType(),
                    context.brandContextSnapshotJson(),
                    SuggestionType.REWRITE,
                    aiResponse.provider(),
                    aiResponse.model(),
                    aiResponse.tokenUsage());
            promptActivityLogger.logPromptCompleted("rewrite", command.workspaceId(), access.currentUser().userId(), aiResponse.provider(), aiResponse.model(), aiResponse.tokenUsage());
            return result;
        } catch (RuntimeException exception) {
            recordFailureQuietly(
                    command.workspaceId(),
                    command.projectId(),
                    access.currentUser().userId(),
                    sourcePrompt,
                    command.language(),
                    command.platform(),
                    command.campaignObjective(),
                    command.businessType(),
                    context.brandContextSnapshotJson(),
                    SuggestionType.REWRITE);
            promptActivityLogger.logProviderFailure("rewrite", command.workspaceId(), access.currentUser().userId(), aiProviderRouter.activeProviderName(), exception.getMessage());
            throw exception;
        }
    }

    @Transactional
    public PromptSuggestionsView generateSuggestions(PromptSuggestionCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requirePromptAccess(command.workspaceId(), command.projectId());
        validateSuggestionContext(command);
        enforceTemplateUsagePermission(access, command.templateId(), "suggestions");
        promptThrottleService.assertAllowed(command.workspaceId(), access.currentUser().userId(), command.clientIp(), "suggestions");

        PromptContextAssembler.ResolvedPromptContext context = promptContextAssembler.assemble(
                command.workspaceId(),
                command.projectId(),
                command.templateId(),
                command.useBrandProfile(),
                command.assetIds());
        String sourcePrompt = resolveSuggestionSource(command);
        Set<SuggestionType> suggestionTypes = normalizeSuggestionTypes(command.suggestionTypes());
        promptActivityLogger.logPromptRequest(
                "suggestions",
                command.workspaceId(),
                access.currentUser().userId(),
                command.platform(),
                command.campaignObjective(),
                command.language(),
                sourcePrompt.length(),
                context.assets().size(),
                command.useBrandProfile(),
                suggestionTypes,
                sourcePrompt);

        String promptHash = promptHash("suggestions", command.workspaceId(), command.projectId(), suggestionCachePayload(command, context, sourcePrompt, suggestionTypes));
        Optional<PromptSuggestionsView> cached = cachedView(promptHash, PromptSuggestionsView.class);
        if (cached.isPresent()) {
            PromptSuggestionsView result = cached.get();
            persistSuggestions(command.workspaceId(), command.projectId(), result, suggestionTypes);
            PromptHistoryEntity history = promptHistoryService.recordSuccess(
                    command.workspaceId(),
                    command.projectId(),
                    null,
                    access.currentUser().userId(),
                    sourcePrompt,
                    promptJsonCodec.write(result, ErrorCode.PROMPT_CONTEXT_INVALID, "Prompt suggestions result could not be persisted"),
                    command.language(),
                    command.platform(),
                    command.campaignObjective(),
                    command.businessType(),
                    context.brandContextSnapshotJson(),
                    historySuggestionType(suggestionTypes),
                    result.aiProvider(),
                    result.aiModel(),
                    result.tokenUsage());
            publishPromptSuggestionGenerated(command.workspaceId(), history.getId(), access.currentUser().userId(), suggestionTypes, result);
            promptActivityLogger.logPromptCompleted("suggestions", command.workspaceId(), access.currentUser().userId(), result.aiProvider(), result.aiModel(), result.tokenUsage());
            return result;
        }

        try {
            AiResponse aiResponse = aiProviderRouter.generate(promptInstructionFactory.buildSuggestionRequest(
                    new PromptSuggestionCommand(
                            command.workspaceId(),
                            command.projectId(),
                            sourcePrompt,
                            command.assetIds(),
                            command.templateId(),
                            command.businessType(),
                            command.campaignObjective(),
                            command.platform(),
                            command.creativeStyle(),
                            command.language(),
                            command.tone(),
                            command.targetAudience(),
                            command.offerDetails(),
                            command.ctaPreference(),
                            command.useBrandProfile(),
                            suggestionTypes,
                            command.clientIp()),
                    context));
            PromptSuggestionsView result = promptResponseParser.parseSuggestions(
                    aiResponse.content(),
                    aiResponse.provider(),
                    aiResponse.model(),
                    aiResponse.tokenUsage());
            cacheView(promptHash, result, result.aiProvider(), result.aiModel());
            persistSuggestions(command.workspaceId(), command.projectId(), result, suggestionTypes);
            PromptHistoryEntity history = promptHistoryService.recordSuccess(
                    command.workspaceId(),
                    command.projectId(),
                    null,
                    access.currentUser().userId(),
                    sourcePrompt,
                    promptJsonCodec.write(result, ErrorCode.PROMPT_CONTEXT_INVALID, "Prompt suggestions result could not be persisted"),
                    command.language(),
                    command.platform(),
                    command.campaignObjective(),
                    command.businessType(),
                    context.brandContextSnapshotJson(),
                    historySuggestionType(suggestionTypes),
                    aiResponse.provider(),
                    aiResponse.model(),
                    aiResponse.tokenUsage());
            publishPromptSuggestionGenerated(command.workspaceId(), history.getId(), access.currentUser().userId(), suggestionTypes, result);
            promptActivityLogger.logPromptCompleted("suggestions", command.workspaceId(), access.currentUser().userId(), aiResponse.provider(), aiResponse.model(), aiResponse.tokenUsage());
            return result;
        } catch (RuntimeException exception) {
            recordFailureQuietly(
                    command.workspaceId(),
                    command.projectId(),
                    access.currentUser().userId(),
                    sourcePrompt,
                    command.language(),
                    command.platform(),
                    command.campaignObjective(),
                    command.businessType(),
                    context.brandContextSnapshotJson(),
                    historySuggestionType(suggestionTypes));
            promptActivityLogger.logProviderFailure("suggestions", command.workspaceId(), access.currentUser().userId(), aiProviderRouter.activeProviderName(), exception.getMessage());
            throw exception;
        }
    }

    @Transactional
    public PromptSuggestionListView generateSuggestionList(PromptSuggestionCommand command, SuggestionType suggestionType) {
        PromptSuggestionsView suggestions = generateSuggestions(new PromptSuggestionCommand(
                command.workspaceId(),
                command.projectId(),
                command.customPrompt(),
                command.assetIds(),
                command.templateId(),
                command.businessType(),
                command.campaignObjective(),
                command.platform(),
                command.creativeStyle(),
                command.language(),
                command.tone(),
                command.targetAudience(),
                command.offerDetails(),
                command.ctaPreference(),
                command.useBrandProfile(),
                Set.of(suggestionType),
                command.clientIp()));
        return new PromptSuggestionListView(
                suggestionType,
                switch (suggestionType) {
                    case CTA_SUGGESTIONS -> suggestions.ctaSuggestions();
                    case HEADLINE_SUGGESTIONS -> suggestions.headlineSuggestions();
                    case OFFER_SUGGESTIONS -> suggestions.offerSuggestions();
                    case CREATIVE_ANGLE_SUGGESTIONS -> suggestions.creativeAngleSuggestions();
                    case CAMPAIGN_TONE_SUGGESTIONS -> suggestions.campaignToneSuggestions();
                    case BUSINESS_CATEGORY_SUGGESTIONS -> suggestions.businessCategorySuggestions();
                    default -> List.of();
                },
                suggestions.reasoningSummary(),
                suggestions.aiProvider(),
                suggestions.aiModel(),
                suggestions.tokenUsage());
    }

    private WorkspaceAuthorizationService.WorkspaceAccess requirePromptAccess(UUID workspaceId, UUID projectId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.PROMPT_INTELLIGENCE_USE);
        if (projectId != null) {
            projectCampaignService.requireProjectCampaign(workspaceId, projectId);
        }
        return access;
    }

    private void validatePromptText(String prompt, String fieldName) {
        if (!StringUtils.hasText(prompt)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, fieldName + " must be provided");
        }
        int length = prompt.trim().length();
        if (length < 5 || length > 5000) {
            throw new BusinessException(ErrorCode.PROMPT_LENGTH_INVALID, fieldName + " must be between 5 and 5000 characters");
        }
    }

    private void validateSuggestionContext(PromptSuggestionCommand command) {
        if (StringUtils.hasText(command.customPrompt())) {
            validatePromptText(command.customPrompt(), "customPrompt");
            return;
        }
        boolean hasContext = StringUtils.hasText(command.businessType())
                || StringUtils.hasText(command.targetAudience())
                || StringUtils.hasText(command.offerDetails())
                || StringUtils.hasText(command.ctaPreference())
                || command.useBrandProfile()
                || (command.assetIds() != null && !command.assetIds().isEmpty())
                || command.templateId() != null
                || command.platform() != null
                || command.campaignObjective() != null;
        if (!hasContext) {
            throw new BusinessException(ErrorCode.PROMPT_UNSUPPORTED_COMBINATION, "Suggestions require a prompt or at least one meaningful campaign context input");
        }
    }

    private void enforceTemplateUsagePermission(WorkspaceAuthorizationService.WorkspaceAccess access, UUID templateId, String operation) {
        if (templateId == null) {
            return;
        }
        if (access.effectiveRole().isMaster()
                || access.permissions().contains(Permission.PROMPT_TEMPLATE_VIEW)
                || access.permissions().contains(Permission.PROMPT_TEMPLATE_MANAGE)) {
            return;
        }
        promptActivityLogger.logAuthorizationFailure(operation, access.workspace().getId(), access.currentUser().userId(), "missing_prompt_template_view_permission");
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private Set<SuggestionType> normalizeSuggestionTypes(Set<SuggestionType> suggestionTypes) {
        if (suggestionTypes == null || suggestionTypes.isEmpty()) {
            return EnumSet.of(
                    SuggestionType.CTA_SUGGESTIONS,
                    SuggestionType.HEADLINE_SUGGESTIONS,
                    SuggestionType.OFFER_SUGGESTIONS,
                    SuggestionType.CREATIVE_ANGLE_SUGGESTIONS,
                    SuggestionType.CAMPAIGN_TONE_SUGGESTIONS,
                    SuggestionType.BUSINESS_CATEGORY_SUGGESTIONS);
        }
        EnumSet<SuggestionType> normalized = EnumSet.copyOf(suggestionTypes);
        normalized.remove(SuggestionType.ENHANCEMENT);
        normalized.remove(SuggestionType.REWRITE);
        normalized.remove(SuggestionType.GENERAL_SUGGESTIONS);
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.PROMPT_UNSUPPORTED_COMBINATION, "Suggestion request must include at least one supported suggestion category");
        }
        return Set.copyOf(normalized);
    }

    private String resolveSuggestionSource(PromptSuggestionCommand command) {
        if (StringUtils.hasText(command.customPrompt())) {
            return command.customPrompt().trim();
        }
        StringBuilder builder = new StringBuilder("Prompt suggestion context");
        append(builder, "businessType", command.businessType());
        append(builder, "campaignObjective", command.campaignObjective() == null ? null : command.campaignObjective().name());
        append(builder, "platform", command.platform() == null ? null : command.platform().name());
        append(builder, "creativeStyle", command.creativeStyle() == null ? null : command.creativeStyle().name());
        append(builder, "language", command.language() == null ? null : command.language().name());
        append(builder, "tone", command.tone() == null ? null : command.tone().name());
        append(builder, "targetAudience", command.targetAudience());
        append(builder, "offerDetails", command.offerDetails());
        append(builder, "ctaPreference", command.ctaPreference());
        return builder.toString();
    }

    private void append(StringBuilder builder, String label, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        builder.append(" | ").append(label).append('=').append(value.trim());
    }

    private <T> Optional<T> cachedView(String promptHash, Class<T> type) {
        return aiPromptResponseRedisCacheService.get(promptHash)
                .flatMap(entry -> {
                    try {
                        return Optional.of(promptJsonCodec.readValue(
                                entry.payload(),
                                type,
                                ErrorCode.PROMPT_AI_RESPONSE_INVALID,
                                "Cached prompt response could not be read"));
                    } catch (BusinessException exception) {
                        return Optional.empty();
                    }
                });
    }

    private void cacheView(String promptHash, Object view, String provider, String model) {
        String payload = promptJsonCodec.write(view, ErrorCode.PROMPT_CONTEXT_INVALID, "Prompt cache payload could not be serialized");
        aiPromptResponseRedisCacheService.store(new PromptResponseCacheEntry(
                promptHash,
                provider,
                model,
                payload,
                Instant.now()));
    }

    private String promptHash(String operation, UUID workspaceId, UUID projectId, Map<String, Object> payload) {
        LinkedHashMap<String, Object> values = new LinkedHashMap<>();
        values.put("operation", operation);
        values.put("workspaceId", workspaceId);
        values.put("projectId", projectId);
        values.putAll(payload);
        return aiPromptResponseRedisCacheService.hash(promptJsonCodec.write(
                values,
                ErrorCode.PROMPT_CONTEXT_INVALID,
                "Prompt cache key could not be serialized"));
    }

    private Map<String, Object> enhancementCachePayload(
            PromptEnhancementCommand command,
            PromptContextAssembler.ResolvedPromptContext context,
            String sourcePrompt
    ) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourcePrompt", sourcePrompt);
        payload.put("templateId", command.templateId());
        payload.put("templateBody", context.template() == null ? null : context.template().getPromptBody());
        payload.put("businessType", command.businessType());
        payload.put("campaignObjective", enumName(command.campaignObjective()));
        payload.put("platform", enumName(command.platform()));
        payload.put("creativeStyle", enumName(command.creativeStyle()));
        payload.put("language", enumName(command.language()));
        payload.put("tone", enumName(command.tone()));
        payload.put("targetAudience", command.targetAudience());
        payload.put("offerDetails", command.offerDetails());
        payload.put("ctaPreference", command.ctaPreference());
        payload.put("useBrandProfile", command.useBrandProfile());
        payload.put("brandContextSnapshot", context.brandContextSnapshotJson());
        payload.put("assetIds", command.assetIds() == null ? List.of() : List.copyOf(command.assetIds()));
        return payload;
    }

    private Map<String, Object> suggestionCachePayload(
            PromptSuggestionCommand command,
            PromptContextAssembler.ResolvedPromptContext context,
            String sourcePrompt,
            Set<SuggestionType> suggestionTypes
    ) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("sourcePrompt", sourcePrompt);
        payload.put("suggestionTypes", suggestionTypes.stream().map(Enum::name).sorted().toList());
        payload.put("templateId", command.templateId());
        payload.put("templateBody", context.template() == null ? null : context.template().getPromptBody());
        payload.put("businessType", command.businessType());
        payload.put("campaignObjective", enumName(command.campaignObjective()));
        payload.put("platform", enumName(command.platform()));
        payload.put("creativeStyle", enumName(command.creativeStyle()));
        payload.put("language", enumName(command.language()));
        payload.put("tone", enumName(command.tone()));
        payload.put("targetAudience", command.targetAudience());
        payload.put("offerDetails", command.offerDetails());
        payload.put("ctaPreference", command.ctaPreference());
        payload.put("useBrandProfile", command.useBrandProfile());
        payload.put("brandContextSnapshot", context.brandContextSnapshotJson());
        payload.put("assetIds", command.assetIds() == null ? List.of() : List.copyOf(command.assetIds()));
        return payload;
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }

    private void persistSuggestions(
            UUID workspaceId,
            UUID projectId,
            PromptSuggestionsView result,
            Set<SuggestionType> suggestionTypes
    ) {
        List<PromptSuggestionEntity> entities = suggestionTypes.stream()
                .flatMap(type -> suggestionTexts(type, result).stream()
                        .map(text -> PromptSuggestionEntity.create(workspaceId, projectId, type, text)))
                .toList();
        if (!entities.isEmpty()) {
            promptSuggestionRepository.saveAll(entities);
        }
    }

    private List<String> suggestionTexts(SuggestionType type, PromptSuggestionsView result) {
        return switch (type) {
            case CTA_SUGGESTIONS -> result.ctaSuggestions();
            case HEADLINE_SUGGESTIONS -> result.headlineSuggestions();
            case OFFER_SUGGESTIONS -> result.offerSuggestions();
            case CREATIVE_ANGLE_SUGGESTIONS -> result.creativeAngleSuggestions();
            case CAMPAIGN_TONE_SUGGESTIONS -> result.campaignToneSuggestions();
            case BUSINESS_CATEGORY_SUGGESTIONS -> result.businessCategorySuggestions();
            default -> List.of();
        };
    }

    private SuggestionType historySuggestionType(Set<SuggestionType> suggestionTypes) {
        return suggestionTypes.size() == 1 ? suggestionTypes.iterator().next() : SuggestionType.GENERAL_SUGGESTIONS;
    }

    private void publishPromptEnhanced(
            UUID workspaceId,
            UUID historyId,
            UUID userId,
            String sourcePrompt,
            PromptEnhancementView result
    ) {
        creativeGenerationEventProducer.publishPromptEnhanced(new PromptEnhancedEvent(
                null,
                null,
                workspaceId,
                historyId,
                userId,
                sourcePrompt,
                result.enhancedPrompt(),
                result.aiProvider(),
                result.aiModel(),
                result.tokenUsage()));
    }

    private void publishPromptSuggestionGenerated(
            UUID workspaceId,
            UUID historyId,
            UUID userId,
            Set<SuggestionType> suggestionTypes,
            PromptSuggestionsView result
    ) {
        creativeGenerationEventProducer.publishPromptSuggestionGenerated(new PromptSuggestionGeneratedEvent(
                null,
                null,
                workspaceId,
                historyId,
                userId,
                suggestionTypes,
                Map.of(
                        "ctaSuggestions", result.ctaSuggestions(),
                        "headlineSuggestions", result.headlineSuggestions(),
                        "offerSuggestions", result.offerSuggestions(),
                        "creativeAngleSuggestions", result.creativeAngleSuggestions(),
                        "campaignToneSuggestions", result.campaignToneSuggestions(),
                        "businessCategorySuggestions", result.businessCategorySuggestions()),
                result.aiProvider(),
                result.aiModel(),
                result.tokenUsage()));
    }

    private void recordFailureQuietly(
            UUID workspaceId,
            UUID projectId,
            UUID userId,
            String sourcePrompt,
            PromptLanguage language,
            PromptPlatform platform,
            com.lebhas.creativesaas.prompt.domain.CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType
    ) {
        try {
            promptHistoryService.recordFailure(
                    workspaceId,
                    projectId,
                    null,
                    userId,
                    sourcePrompt,
                    language,
                    platform,
                    campaignObjective,
                    businessType,
                    brandContextSnapshot,
                    suggestionType,
                    aiProviderRouter.activeProviderName(),
                    aiProviderRouter.activeModelName());
        } catch (RuntimeException ignored) {
        }
    }
}
