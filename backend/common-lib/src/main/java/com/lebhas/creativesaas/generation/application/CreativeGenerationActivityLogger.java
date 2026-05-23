package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.generation.domain.CreativeGenerationStatus;
import com.lebhas.creativesaas.generation.domain.CreativeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CreativeGenerationActivityLogger {

    private static final Logger log = LoggerFactory.getLogger(CreativeGenerationActivityLogger.class);

    public void logRequestCreated(UUID workspaceId, UUID requestId, UUID actorUserId, CreativeType creativeType, int promptLength, int assetCount) {
        log.info("generation_event type=request_created workspaceId={} requestId={} actorUserId={} creativeType={} promptLength={} assetCount={}",
                workspaceId, requestId, actorUserId, creativeType, promptLength, assetCount);
    }

    public void logJobQueued(UUID workspaceId, UUID requestId, UUID jobId, String queueName) {
        log.info("generation_event type=job_queued workspaceId={} requestId={} jobId={} queueName={}",
                workspaceId, requestId, jobId, queueName);
    }

    public void logJobStarted(UUID workspaceId, UUID requestId, UUID jobId, int attempt) {
        log.info("generation_event type=job_started workspaceId={} requestId={} jobId={} attempt={}",
                workspaceId, requestId, jobId, attempt);
    }

    public void logJobCompleted(UUID workspaceId, UUID requestId, UUID jobId, UUID outputId, UUID assetId, String provider, String model) {
        log.info("generation_event type=job_completed workspaceId={} requestId={} jobId={} outputId={} assetId={} provider={} model={}",
                workspaceId, requestId, jobId, outputId, assetId, provider, model);
    }

    public void logJobFailed(UUID workspaceId, UUID requestId, UUID jobId, CreativeGenerationStatus status, int attempt, String reason) {
        log.warn("generation_event type=job_failed workspaceId={} requestId={} jobId={} status={} attempt={} reason={}",
                workspaceId, requestId, jobId, status, attempt, abbreviate(reason));
    }

    public void logRetryRequested(UUID workspaceId, UUID requestId, UUID actorUserId, UUID jobId) {
        log.info("generation_event type=retry_requested workspaceId={} requestId={} actorUserId={} jobId={}",
                workspaceId, requestId, actorUserId, jobId);
    }

    public void logCancelRequested(UUID workspaceId, UUID requestId, UUID actorUserId) {
        log.info("generation_event type=cancel_requested workspaceId={} requestId={} actorUserId={}",
                workspaceId, requestId, actorUserId);
    }

    public void logProviderError(UUID workspaceId, UUID requestId, UUID jobId, String provider, String reason) {
        log.warn("generation_event type=provider_error workspaceId={} requestId={} jobId={} provider={} reason={}",
                workspaceId, requestId, jobId, provider, abbreviate(reason));
    }

    public void logValidationFailure(UUID workspaceId, UUID actorUserId, String reason) {
        log.warn("generation_event type=validation_failure workspaceId={} actorUserId={} reason={}",
                workspaceId, actorUserId, abbreviate(reason));
    }

    public void logAuthorizationFailure(UUID workspaceId, UUID actorUserId, String reason) {
        log.warn("generation_event type=authorization_failure workspaceId={} actorUserId={} reason={}",
                workspaceId, actorUserId, reason);
    }

    public void logRateLimitTriggered(UUID workspaceId, UUID actorUserId, String clientIp, String scope, long attempts) {
        log.warn("generation_event type=rate_limit_triggered workspaceId={} actorUserId={} clientIp={} scope={} attempts={}",
                workspaceId, actorUserId, clientIp, scope, attempts);
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180);
    }
}
