package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.domain.CreativeApprovalAction;
import com.lebhas.creativesaas.approval.domain.CreativeApprovalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Deprecated(forRemoval = true)
public class CreativeApprovalActivityLogger {

    private static final Logger log = LoggerFactory.getLogger(CreativeApprovalActivityLogger.class);

    public void logApprovalCreated(UUID workspaceId, UUID approvalId, UUID creativeOutputId, UUID actorUserId) {
        log.info("approval_event type=approval_created workspaceId={} approvalId={} creativeOutputId={} actorUserId={}",
                workspaceId, approvalId, creativeOutputId, actorUserId);
    }

    public void logAction(UUID workspaceId, UUID approvalId, UUID creativeOutputId, UUID actorUserId, CreativeApprovalAction action) {
        log.info("approval_event type={} workspaceId={} approvalId={} creativeOutputId={} actorUserId={}",
                action.name().toLowerCase(), workspaceId, approvalId, creativeOutputId, actorUserId);
    }

    public void logCommentAdded(UUID workspaceId, UUID approvalId, UUID creativeOutputId, UUID actorUserId, int commentLength) {
        log.info("approval_event type=comment_added workspaceId={} approvalId={} creativeOutputId={} actorUserId={} commentLength={}",
                workspaceId, approvalId, creativeOutputId, actorUserId, commentLength);
    }

    public void logInvalidTransition(
            UUID workspaceId,
            UUID approvalId,
            UUID actorUserId,
            CreativeApprovalStatus currentStatus,
            CreativeApprovalStatus requestedStatus
    ) {
        log.warn("approval_event type=invalid_transition workspaceId={} approvalId={} actorUserId={} currentStatus={} requestedStatus={}",
                workspaceId, approvalId, actorUserId, currentStatus, requestedStatus);
    }

    public void logValidationFailure(UUID workspaceId, UUID actorUserId, String reason) {
        log.warn("approval_event type=validation_failure workspaceId={} actorUserId={} reason={}",
                workspaceId, actorUserId, abbreviate(reason));
    }

    public void logAuthorizationFailure(UUID workspaceId, UUID actorUserId, String reason) {
        log.warn("approval_event type=authorization_failure workspaceId={} actorUserId={} reason={}",
                workspaceId, actorUserId, reason);
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180 ? normalized : normalized.substring(0, 180);
    }
}
