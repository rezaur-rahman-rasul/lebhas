package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;

@Component
public class PromptActivityLogger {

    private static final Logger log = LoggerFactory.getLogger(PromptActivityLogger.class);

    public void logTemplateCreated(UUID workspaceId, UUID actorUserId, UUID templateId, boolean systemDefault) {
        log.info("prompt_event type=template_created workspaceId={} actorUserId={} templateId={} systemDefault={}",
                workspaceId, actorUserId, templateId, systemDefault);
    }

    public void logTemplateUpdated(UUID workspaceId, UUID actorUserId, UUID templateId, boolean systemDefault) {
        log.info("prompt_event type=template_updated workspaceId={} actorUserId={} templateId={} systemDefault={}",
                workspaceId, actorUserId, templateId, systemDefault);
    }

    public void logTemplateDeleted(UUID workspaceId, UUID actorUserId, UUID templateId, boolean systemDefault) {
        log.info("prompt_event type=template_deleted workspaceId={} actorUserId={} templateId={} systemDefault={}",
                workspaceId, actorUserId, templateId, systemDefault);
    }

    public void logPromptRequest(
            String operation,
            UUID workspaceId,
            UUID actorUserId,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            PromptLanguage language,
            int promptLength,
            int assetCount,
            boolean brandContextEnabled,
            Set<SuggestionType> suggestionTypes,
            String promptPreview
    ) {
        log.info(
                "prompt_event type=intelligence_request operation={} workspaceId={} actorUserId={} platform={} campaignObjective={} language={} promptLength={} assetCount={} brandContextEnabled={} suggestionTypes={} promptPreview={}",
                operation,
                workspaceId,
                actorUserId,
                platform,
                campaignObjective,
                language,
                promptLength,
                assetCount,
                brandContextEnabled,
                suggestionTypes,
                promptPreview(promptPreview));
    }

    public void logPromptCompleted(String operation, UUID workspaceId, UUID actorUserId, String aiProvider, String aiModel, Integer tokenUsage) {
        log.info("prompt_event type=intelligence_completed operation={} workspaceId={} actorUserId={} aiProvider={} aiModel={} tokenUsage={}",
                operation, workspaceId, actorUserId, aiProvider, aiModel, tokenUsage);
    }

    public void logProviderFailure(String operation, UUID workspaceId, UUID actorUserId, String aiProvider, String message) {
        log.warn("prompt_event type=provider_failure operation={} workspaceId={} actorUserId={} aiProvider={} reason={}",
                operation, workspaceId, actorUserId, aiProvider, abbreviate(message));
    }

    public void logValidationFailure(String operation, UUID workspaceId, UUID actorUserId, String message) {
        log.warn("prompt_event type=validation_failure operation={} workspaceId={} actorUserId={} reason={}",
                operation, workspaceId, actorUserId, abbreviate(message));
    }

    public void logAuthorizationFailure(String operation, UUID workspaceId, UUID actorUserId, String reason) {
        log.warn("prompt_event type=authorization_failure operation={} workspaceId={} actorUserId={} reason={}",
                operation, workspaceId, actorUserId, reason);
    }

    public void logRateLimitTriggered(String operation, UUID workspaceId, UUID actorUserId, String clientIp, String scope, long attempts) {
        log.warn("prompt_event type=rate_limit_triggered operation={} workspaceId={} actorUserId={} clientIp={} scope={} attempts={}",
                operation, workspaceId, actorUserId, clientIp, scope, attempts);
    }

    private String promptPreview(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return abbreviate(value.replaceAll("\\s+", " "));
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 160 ? value : value.substring(0, 160);
    }
}
