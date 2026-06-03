package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.prompt.application.dto.CreatePromptTemplateCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptDraftCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptEnhancementView;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptSuggestionsView;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateFilter;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateReuseView;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateView;
import com.lebhas.creativesaas.prompt.application.dto.PromptValidationCommand;
import com.lebhas.creativesaas.prompt.cache.PromptEnhancementCacheService;
import com.lebhas.creativesaas.prompt.cache.PromptRedisAccessSupport;
import com.lebhas.creativesaas.prompt.cache.PromptRedisKeys;
import com.lebhas.creativesaas.prompt.cache.PromptRedisOperationContext;
import com.lebhas.creativesaas.prompt.cache.dto.PromptEnhancementCacheEntry;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import com.lebhas.creativesaas.redis.RedisLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class PromptBuilderService {

    private static final String PROVIDER = "INTERNAL_FOUNDATION";
    private static final String MODEL = "PROMPT_BUILDER_PLACEHOLDER_V1";

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final PromptReadinessService promptReadinessService;
    private final PromptHistoryService promptHistoryService;
    private final PromptJsonCodec promptJsonCodec;
    private final PromptTemplateService promptTemplateService;
    private final PromptDraftService promptDraftService;
    private final PromptEnhancementCacheService promptEnhancementCacheService;
    private final PromptRedisAccessSupport promptRedisAccessSupport;
    private final DomainEventPublisher domainEventPublisher;

    public PromptBuilderService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            PromptReadinessService promptReadinessService,
            PromptHistoryService promptHistoryService,
            PromptJsonCodec promptJsonCodec,
            PromptTemplateService promptTemplateService,
            PromptDraftService promptDraftService,
            PromptEnhancementCacheService promptEnhancementCacheService,
            PromptRedisAccessSupport promptRedisAccessSupport,
            DomainEventPublisher domainEventPublisher
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.promptReadinessService = promptReadinessService;
        this.promptHistoryService = promptHistoryService;
        this.promptJsonCodec = promptJsonCodec;
        this.promptTemplateService = promptTemplateService;
        this.promptDraftService = promptDraftService;
        this.promptEnhancementCacheService = promptEnhancementCacheService;
        this.promptRedisAccessSupport = promptRedisAccessSupport;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public PromptEnhancementView enhance(PromptEnhancementCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.PROMPT_INTELLIGENCE_USE);
        promptReadinessService.assertReady(new PromptValidationCommand(
                command.workspaceId(),
                command.projectId(),
                command.customPrompt(),
                command.language(),
                command.assetIds(),
                true,
                false,
                false));
        publish(KafkaTopicConstants.PROMPT_ENHANCEMENT_REQUESTED, command.workspaceId(), command.projectId(), access.currentUser().userId());
        String promptHash = promptHash(command);
        Optional<PromptEnhancementView> cached = promptEnhancementCacheService.get(promptHash).map(this::toEnhancementView);
        if (cached.isPresent()) {
            return cached.get();
        }
        Optional<RedisLockService.RedisLockToken> lock = promptRedisAccessSupport.acquireLock(
                PromptRedisKeys.promptEnhancementLock(promptHash),
                Duration.ofSeconds(30),
                "prompt_enhancement_duplicate_lock",
                PromptRedisOperationContext.promptHash(promptHash));
        PromptEnhancementView view;
        try {
            view = new PromptEnhancementView(
                    buildEnhancedPrompt(command),
                    "Deterministic placeholder enhancement generated from prompt builder context. No external AI provider was called.",
                    missingFields(command),
                    PROVIDER,
                    MODEL,
                    null);
            promptEnhancementCacheService.store(view, promptHash);
        } finally {
            lock.ifPresent(token -> promptRedisAccessSupport.releaseLock(
                    token,
                    "prompt_enhancement_duplicate_lock_release",
                    PromptRedisOperationContext.promptHash(promptHash)));
        }
        promptHistoryService.recordSuccess(
                command.workspaceId(),
                command.projectId(),
                command.creativeRequestId(),
                access.currentUser().userId(),
                command.customPrompt(),
                promptJsonCodec.write(view, com.lebhas.creativesaas.common.exception.ErrorCode.PROMPT_CONTEXT_INVALID, "Prompt enhancement result could not be serialized"),
                command.language(),
                command.platform(),
                command.campaignObjective(),
                command.businessType(),
                null,
                SuggestionType.ENHANCEMENT,
                PROVIDER,
                MODEL,
                null);
        publish(KafkaTopicConstants.PROMPT_ENHANCEMENT_COMPLETED, command.workspaceId(), command.projectId(), access.currentUser().userId());
        return view;
    }

    @Transactional
    public PromptSuggestionsView suggestions(PromptSuggestionCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.PROMPT_INTELLIGENCE_USE);
        promptReadinessService.assertReady(new PromptValidationCommand(
                command.workspaceId(),
                command.projectId(),
                command.customPrompt(),
                command.language(),
                command.assetIds(),
                false,
                true,
                false));
        String seed = StringUtils.hasText(command.customPrompt()) ? command.customPrompt().trim() : "campaign context";
        PromptSuggestionsView view = new PromptSuggestionsView(
                List.of("Shop now", "Create yours", "Discover the look"),
                List.of("Turn %s into a premium campaign".formatted(seed), "Make the offer impossible to miss", "Launch with brand-ready visuals"),
                List.of("Bundle value with urgency", "Highlight the strongest product benefit", "Lead with a limited-time incentive"),
                List.of("Lifestyle-led product story", "Benefit-first conversion angle", "Premium brand attire showcase"),
                List.of("Confident and refined", "Clear and direct", "Warm and aspirational"),
                List.of("Fashion retail", "Brand attire", "Creative commerce"),
                "Deterministic placeholder suggestions generated without calling an AI provider.",
                PROVIDER,
                MODEL,
                null);
        promptHistoryService.recordSuccess(
                command.workspaceId(),
                command.projectId(),
                null,
                access.currentUser().userId(),
                seed,
                promptJsonCodec.write(view, com.lebhas.creativesaas.common.exception.ErrorCode.PROMPT_CONTEXT_INVALID, "Prompt suggestions result could not be serialized"),
                command.language(),
                command.platform(),
                command.campaignObjective(),
                command.businessType(),
                null,
                SuggestionType.GENERAL_SUGGESTIONS,
                PROVIDER,
                MODEL,
                null);
        return view;
    }

    @Transactional
    public PromptTemplateView createTemplate(CreatePromptTemplateCommand command) {
        promptReadinessService.assertReady(new PromptValidationCommand(
                command.workspaceId(),
                null,
                command.templateText(),
                command.language(),
                List.of(),
                false,
                false,
                true));
        return promptTemplateService.createTemplate(command);
    }

    @Transactional(readOnly = true)
    public List<PromptTemplateView> listTemplates(PromptTemplateFilter filter) {
        return promptTemplateService.listTemplates(filter);
    }

    @Transactional
    public PromptTemplateReuseView reuseTemplate(UUID workspaceId, UUID projectId, UUID templateId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.PROMPT_TEMPLATE_VIEW);
        promptReadinessService.assertReady(new PromptValidationCommand(
                workspaceId,
                projectId,
                null,
                null,
                List.of(),
                false,
                false,
                true));
        PromptTemplateView template = promptTemplateService.getTemplate(workspaceId, templateId);
        PromptTemplateReuseView view = new PromptTemplateReuseView(
                template.id(),
                template.templateText(),
                promptDraftService.create(new PromptDraftCommand(
                        workspaceId,
                        projectId,
                        "Draft from %s".formatted(template.name()),
                        template.templateText(),
                        template.language(),
                        template.platform(),
                        template.campaignObjective(),
                        template.id())));
        domainEventPublisher.publish(KafkaTopicConstants.PROMPT_TEMPLATE_REUSED, new BaseDomainEvent(
                KafkaTopicConstants.PROMPT_TEMPLATE_REUSED,
                workspaceId,
                templateId,
                Instant.now(),
                Map.of("templateId", templateId, "projectId", projectId, "actorUserId", access.currentUser().userId())));
        return view;
    }

    private String buildEnhancedPrompt(PromptEnhancementCommand command) {
        StringBuilder builder = new StringBuilder(command.customPrompt().trim());
        append(builder, "Business type", command.businessType());
        append(builder, "Target audience", command.targetAudience());
        append(builder, "Offer", command.offerDetails());
        append(builder, "CTA", command.ctaPreference());
        if (command.language() != null && command.language() != PromptLanguage.MIXED) {
            append(builder, "Language", command.language().name());
        }
        return builder.toString();
    }

    private String promptHash(PromptEnhancementCommand command) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("workspaceId", command.workspaceId());
        payload.put("projectId", command.projectId());
        payload.put("prompt", command.customPrompt());
        payload.put("language", command.language());
        payload.put("platform", command.platform());
        payload.put("campaignObjective", command.campaignObjective());
        payload.put("businessType", command.businessType());
        payload.put("targetAudience", command.targetAudience());
        payload.put("offerDetails", command.offerDetails());
        payload.put("ctaPreference", command.ctaPreference());
        payload.put("templateId", command.templateId());
        payload.put("assetIds", command.assetIds() == null ? List.of() : command.assetIds());
        return promptEnhancementCacheService.sha256(promptJsonCodec.write(
                payload,
                com.lebhas.creativesaas.common.exception.ErrorCode.PROMPT_CONTEXT_INVALID,
                "Prompt enhancement cache key could not be serialized"));
    }

    private PromptEnhancementView toEnhancementView(PromptEnhancementCacheEntry entry) {
        return new PromptEnhancementView(
                entry.enhancedPrompt(),
                entry.reasoningSummary(),
                entry.suggestedMissingFields() == null ? List.of() : entry.suggestedMissingFields(),
                entry.aiProvider(),
                entry.aiModel(),
                entry.tokenUsage());
    }

    private List<String> missingFields(PromptEnhancementCommand command) {
        return java.util.stream.Stream.of(
                        missing("businessType", command.businessType()),
                        missing("targetAudience", command.targetAudience()),
                        missing("offerDetails", command.offerDetails()),
                        missing("ctaPreference", command.ctaPreference()))
                .filter(StringUtils::hasText)
                .toList();
    }

    private String missing(String field, String value) {
        return StringUtils.hasText(value) ? null : field;
    }

    private void append(StringBuilder builder, String label, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(" ").append(label).append(": ").append(value.trim()).append(".");
        }
    }

    private void publish(String topic, UUID workspaceId, UUID projectId, UUID actorUserId) {
        domainEventPublisher.publish(topic, new BaseDomainEvent(
                topic,
                workspaceId,
                projectId,
                Instant.now(),
                Map.of("projectId", projectId, "actorUserId", actorUserId)));
    }
}
