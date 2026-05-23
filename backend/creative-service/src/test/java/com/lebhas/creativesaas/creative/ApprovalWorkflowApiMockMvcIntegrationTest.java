package com.lebhas.creativesaas.creative;

import com.lebhas.creativesaas.approval.domain.ApprovalAssignmentStatus;
import com.lebhas.creativesaas.approval.domain.ApprovalAuditAction;
import com.lebhas.creativesaas.approval.domain.ApprovalDecision;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.approval.dto.ApprovalRejectRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class ApprovalWorkflowApiMockMvcIntegrationTest extends AbstractDay6ApprovalIntegrationTest {

    @Test
    void shouldSubmitGeneratedVersionForApproval() throws Exception {
        var version = createReadyGeneratedVersion(adminUser, "Submit Generated Version");

        MvcResult result = submitApproval(adminUser, Role.ADMIN, version.getId(), Instant.parse("2026-12-31T00:00:00Z"), "Please review");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(result).at("/success").asBoolean()).isTrue();
        assertThat(json(result).at("/data/status").asText()).isEqualTo(ApprovalStatus.SUBMITTED.name());
        GeneratedVersionEntity updatedVersion = reloadGeneratedVersion(version.getId());
        assertThat(updatedVersion.getApprovalStatus())
                .isEqualTo(com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.SUBMITTED);
        assertThat(updatedVersion.getSubmittedForApprovalAt()).isNotNull();
        assertThat(updatedVersion.isEditableBeforeApproval()).isFalse();
    }

    @Test
    void shouldCreateApprovalRequestSuccessfully() throws Exception {
        var version = createReadyGeneratedVersion(adminUser, "Create Approval Request");

        MvcResult result = submitApproval(adminUser, Role.ADMIN, version.getId(), Instant.parse("2026-12-30T00:00:00Z"), "Approval body");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        var approvalRequest = reloadApprovalRequest(uuidAt(result, "/data/id"));
        assertThat(approvalRequest.getWorkspaceId()).isEqualTo(workspaceOne.getId());
        assertThat(approvalRequest.getGeneratedVersionId()).isEqualTo(version.getId());
        assertThat(approvalRequest.getSubmittedBy()).isEqualTo(adminUser.getId());
        assertThat(approvalRequest.getCurrentStatus()).isEqualTo(ApprovalStatus.SUBMITTED);
        assertThat(approvalRequest.getDueAt()).isEqualTo(Instant.parse("2026-12-30T00:00:00Z"));
        assertThat(approvalRequest.getLatestComment()).isEqualTo("Approval body");
    }

    @Test
    void shouldAssignReviewerSuccessfully() throws Exception {
        ApprovalSetup setup = createSubmittedApproval(adminUser, Role.ADMIN, "Assign reviewer");

        MvcResult result = assignReviewer(adminUser, Role.ADMIN, setup.approvalRequest().getId(), reviewerUser.getId());

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        var assignment = approvalAssignmentRepository
                .findFirstByWorkspaceIdAndApprovalRequestIdAndAssignmentStatusAndDeletedFalseOrderByAssignedAtDesc(
                        workspaceOne.getId(),
                        setup.approvalRequest().getId(),
                        ApprovalAssignmentStatus.ACTIVE)
                .orElseThrow();
        assertThat(assignment.getAssignedTo()).isEqualTo(reviewerUser.getId());
        assertThat(reloadApprovalRequest(setup.approvalRequest().getId()).getAssignedReviewerId()).isEqualTo(reviewerUser.getId());
        assertThat(reloadGeneratedVersion(setup.generatedVersion().getId()).getLatestReviewerId()).isEqualTo(reviewerUser.getId());
        assertThat(json(result).at("/data/reviewerId").asText()).isEqualTo(reviewerUser.getId().toString());
    }

    @Test
    void shouldBlockUnauthorizedReviewer() throws Exception {
        ApprovalSetup setup = createAssignedApproval(adminUser, Role.ADMIN, reviewerUser);

        MvcResult result = approve(alternateReviewerUser, Role.CREW, setup.approvalRequest().getId(), "Not my review");

        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(result).at("/success").asBoolean()).isFalse();
        assertThat(json(result).at("/errors/0/code").asText()).isEqualTo("COMMON-403");
        assertThat(approvalReviewRepository.findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByReviewedAtAsc(
                workspaceOne.getId(),
                setup.approvalRequest().getId())).isEmpty();
    }

    @Test
    void shouldUpdateStatusWhenApprovalDecisionIsApproved() throws Exception {
        ApprovalSetup setup = createAssignedApproval(adminUser, Role.ADMIN, reviewerUser);

        MvcResult result = approve(reviewerUser, Role.CREW, setup.approvalRequest().getId(), "Approved");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(reloadApprovalRequest(setup.approvalRequest().getId()).getCurrentStatus()).isEqualTo(ApprovalStatus.APPROVED);
        assertThat(reloadGeneratedVersion(setup.generatedVersion().getId()).getApprovalStatus())
                .isEqualTo(com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.APPROVED);
        assertThat(approvalReviewRepository.findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByReviewedAtAsc(
                workspaceOne.getId(),
                setup.approvalRequest().getId()))
                .extracting(review -> review.getDecision())
                .containsExactly(ApprovalDecision.APPROVED);
    }

    @Test
    void shouldCompleteRejectionFlow() throws Exception {
        ApprovalSetup setup = createAssignedApproval(adminUser, Role.ADMIN, reviewerUser);

        MvcResult result = reject(reviewerUser, Role.CREW, setup.approvalRequest().getId(), "Does not meet the brief");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(reloadApprovalRequest(setup.approvalRequest().getId()).getCurrentStatus()).isEqualTo(ApprovalStatus.REJECTED);
        var version = reloadGeneratedVersion(setup.generatedVersion().getId());
        assertThat(version.getApprovalStatus()).isEqualTo(com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.REJECTED);
        assertThat(version.getLatestApprovalComment()).isEqualTo("Does not meet the brief");
        assertThat(version.isEditableBeforeApproval()).isFalse();
    }

    @Test
    void shouldCompleteChangesRequestedFlow() throws Exception {
        ApprovalSetup setup = createAssignedApproval(adminUser, Role.ADMIN, reviewerUser);

        MvcResult result = requestChanges(reviewerUser, Role.CREW, setup.approvalRequest().getId(), "Please update the CTA");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(reloadApprovalRequest(setup.approvalRequest().getId()).getCurrentStatus()).isEqualTo(ApprovalStatus.CHANGES_REQUESTED);
        var version = reloadGeneratedVersion(setup.generatedVersion().getId());
        assertThat(version.getApprovalStatus())
                .isEqualTo(com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.CHANGES_REQUESTED);
        assertThat(version.getLatestReviewerId()).isEqualTo(reviewerUser.getId());
        assertThat(version.getLatestApprovalComment()).isEqualTo("Please update the CTA");
        assertThat(version.isEditableBeforeApproval()).isTrue();
    }

    @Test
    void shouldCompleteResubmissionFlow() throws Exception {
        ApprovalSetup setup = createAssignedApproval(adminUser, Role.ADMIN, reviewerUser);
        requestChanges(reviewerUser, Role.CREW, setup.approvalRequest().getId(), "Adjust the headline");

        MvcResult result = resubmit(adminUser, Role.ADMIN, setup.approvalRequest().getId(), "Headline updated");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        var request = reloadApprovalRequest(setup.approvalRequest().getId());
        assertThat(request.getCurrentStatus()).isEqualTo(ApprovalStatus.RESUBMITTED);
        assertThat(request.getRevisionCount()).isEqualTo(1);
        assertThat(request.getLatestComment()).isEqualTo("Headline updated");
        var version = reloadGeneratedVersion(setup.generatedVersion().getId());
        assertThat(version.getApprovalStatus()).isEqualTo(com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.RESUBMITTED);
        assertThat(version.getRevisionNumber()).isEqualTo(1);
        assertThat(version.getLatestApprovalComment()).isEqualTo("Headline updated");
    }

    @Test
    void shouldBlockInvalidWorkflowTransitions() throws Exception {
        ApprovalSetup setup = createSubmittedApproval(adminUser, Role.ADMIN, "Invalid transition");

        MvcResult result = resubmit(adminUser, Role.ADMIN, setup.approvalRequest().getId(), "Too early");

        assertThat(result.getResponse().getStatus()).isEqualTo(409);
        assertThat(json(result).at("/success").asBoolean()).isFalse();
        assertThat(json(result).at("/errors/0/code").asText()).isEqualTo("APPROVAL-409-02");
        assertThat(reloadApprovalRequest(setup.approvalRequest().getId()).getCurrentStatus()).isEqualTo(ApprovalStatus.SUBMITTED);
    }

    @Test
    void shouldSaveApprovalCommentsCorrectly() throws Exception {
        ApprovalSetup setup = createAssignedApproval(adminUser, Role.ADMIN, reviewerUser);

        MvcResult result = addComment(reviewerUser, Role.CREW, setup.approvalRequest().getId(), "Internal reviewer note", true);

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        var comments = approvalCommentRepository.findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByCreatedAtAsc(
                workspaceOne.getId(),
                setup.approvalRequest().getId());
        assertThat(comments).hasSize(1);
        assertThat(comments.getFirst().getCommentText()).isEqualTo("Internal reviewer note");
        assertThat(comments.getFirst().isInternalOnly()).isTrue();

        MvcResult getComments = mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}/comments",
                        workspaceOne.getId(),
                        setup.approvalRequest().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN))))
                .andReturn();
        assertThat(getComments.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(getComments).at("/data/0/commentText").asText()).isEqualTo("Internal reviewer note");
    }

    @Test
    void shouldCreateApprovalAuditLog() throws Exception {
        ApprovalSetup setup = createSubmittedApproval(adminUser, Role.ADMIN, "Audit me");

        var auditLogs = approvalAuditLogRepository.findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByCreatedAtAsc(
                workspaceOne.getId(),
                setup.approvalRequest().getId());

        assertThat(auditLogs).hasSize(1);
        assertThat(auditLogs.getFirst().getAction()).isEqualTo(ApprovalAuditAction.SUBMITTED);
        assertThat(auditLogs.getFirst().getPreviousStatus()).isEqualTo(ApprovalStatus.NOT_SUBMITTED);
        assertThat(auditLogs.getFirst().getNewStatus()).isEqualTo(ApprovalStatus.SUBMITTED);
        assertThat(auditLogs.getFirst().getGeneratedVersionId()).isEqualTo(setup.generatedVersion().getId());
    }

    @Test
    void shouldCreateAssignmentAuditLog() throws Exception {
        ApprovalSetup setup = createSubmittedApproval(adminUser, Role.ADMIN, "Assign audit");

        assignReviewer(adminUser, Role.ADMIN, setup.approvalRequest().getId(), reviewerUser.getId());

        var auditLogs = approvalAuditLogRepository.findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByCreatedAtAsc(
                workspaceOne.getId(),
                setup.approvalRequest().getId());

        assertThat(auditLogs).extracting(log -> log.getAction().name())
                .containsExactly("SUBMITTED", "ASSIGNED");
    }

    @Test
    void shouldEnforceWorkspaceIsolation() throws Exception {
        ApprovalSetup setup = createSubmittedApproval(adminUser, Role.ADMIN, "Tenant boundary");

        MvcResult result = mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}",
                        workspaceOne.getId(),
                        setup.approvalRequest().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(workspaceTwoAdmin, workspaceTwo.getId(), Role.ADMIN))))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(result).at("/errors/0/code").asText()).isEqualTo("TENANT-403");
    }

    @Test
    void shouldEnforceCrewPermissionRestrictions() throws Exception {
        var version = createReadyGeneratedVersion(crewUser, "Crew Restricted Version");

        MvcResult result = submitApproval(crewUser, Role.CREW, version.getId(), Instant.parse("2026-12-31T00:00:00Z"), "Crew submit");

        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(json(result).at("/errors/0/code").asText()).isEqualTo("COMMON-403");
        assertThat(approvalRequestRepository.findFirstByWorkspaceIdAndGeneratedVersionIdAndDeletedFalseOrderByCreatedAtDesc(
                workspaceOne.getId(),
                version.getId())).isEmpty();
    }

    @Test
    void shouldApplyApprovalListingFilters() throws Exception {
        ApprovalSetup approved = createAssignedApproval(adminUser, Role.ADMIN, reviewerUser);
        approve(reviewerUser, Role.CREW, approved.approvalRequest().getId(), "Approved");
        setSubmittedAt(approved.approvalRequest().getId(), Instant.parse("2026-01-01T00:00:00Z"));

        ApprovalSetup changesRequested = createAssignedApproval(submitterCrewUser, Role.CREW, reviewerUser);
        requestChanges(reviewerUser, Role.CREW, changesRequested.approvalRequest().getId(), "Revise the copy");
        setSubmittedAt(changesRequested.approvalRequest().getId(), Instant.parse("2026-01-02T00:00:00Z"));

        ApprovalSetup pending = createAssignedApproval(adminUser, Role.ADMIN, reviewerUser);
        setSubmittedAt(pending.approvalRequest().getId(), Instant.parse("2026-01-03T00:00:00Z"));

        MvcResult statusFiltered = listApprovals(adminUser, Role.ADMIN, "status", ApprovalStatus.CHANGES_REQUESTED.name());
        assertThat(json(statusFiltered).at("/data").size()).isEqualTo(1);
        assertThat(json(statusFiltered).at("/data/0/id").asText()).isEqualTo(changesRequested.approvalRequest().getId().toString());

        MvcResult reviewerFiltered = listApprovals(adminUser, Role.ADMIN, "reviewer", reviewerUser.getId().toString());
        assertThat(json(reviewerFiltered).at("/data").size()).isEqualTo(3);

        MvcResult submitterFiltered = listApprovals(adminUser, Role.ADMIN, "submittedBy", submitterCrewUser.getId().toString());
        assertThat(json(submitterFiltered).at("/data").size()).isEqualTo(1);
        assertThat(json(submitterFiltered).at("/data/0/id").asText()).isEqualTo(changesRequested.approvalRequest().getId().toString());

        MvcResult pendingFiltered = listApprovals(adminUser, Role.ADMIN, "pendingOnly", "true");
        assertThat(json(pendingFiltered).at("/data").size()).isEqualTo(1);
        assertThat(json(pendingFiltered).at("/data/0/id").asText()).isEqualTo(pending.approvalRequest().getId().toString());

        MvcResult approvedFiltered = listApprovals(adminUser, Role.ADMIN, "approvedOnly", "true");
        assertThat(json(approvedFiltered).at("/data").size()).isEqualTo(1);
        assertThat(json(approvedFiltered).at("/data/0/id").asText()).isEqualTo(approved.approvalRequest().getId().toString());

        MvcResult dateRangeFiltered = listApprovals(
                adminUser,
                Role.ADMIN,
                "fromDate", Instant.parse("2026-01-02T12:00:00Z").toString(),
                "toDate", Instant.parse("2026-01-03T12:00:00Z").toString());
        assertThat(json(dateRangeFiltered).at("/data").size()).isEqualTo(1);
        assertThat(json(dateRangeFiltered).at("/data/0/id").asText()).isEqualTo(pending.approvalRequest().getId().toString());
    }

    @Test
    void shouldUpdateGeneratedVersionApprovalStateCorrectly() throws Exception {
        ApprovalSetup setup = createAssignedApproval(adminUser, Role.ADMIN, reviewerUser);

        requestChanges(reviewerUser, Role.CREW, setup.approvalRequest().getId(), "Need a clearer headline");
        var changesVersion = reloadGeneratedVersion(setup.generatedVersion().getId());
        assertThat(changesVersion.getApprovalStatus())
                .isEqualTo(com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.CHANGES_REQUESTED);
        assertThat(changesVersion.isEditableBeforeApproval()).isTrue();

        resubmit(adminUser, Role.ADMIN, setup.approvalRequest().getId(), "Headline refined");
        var resubmittedVersion = reloadGeneratedVersion(setup.generatedVersion().getId());
        assertThat(resubmittedVersion.getApprovalStatus())
                .isEqualTo(com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.RESUBMITTED);
        assertThat(resubmittedVersion.getRevisionNumber()).isEqualTo(1);
        assertThat(resubmittedVersion.isEditableBeforeApproval()).isFalse();

        approve(reviewerUser, Role.CREW, setup.approvalRequest().getId(), "Final approval");
        var approvedVersion = reloadGeneratedVersion(setup.generatedVersion().getId());
        assertThat(approvedVersion.getApprovalStatus())
                .isEqualTo(com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.APPROVED);
        assertThat(approvedVersion.getLatestReviewerId()).isEqualTo(reviewerUser.getId());
        assertThat(approvedVersion.getLatestApprovalComment()).isEqualTo("Final approval");
        assertThat(approvedVersion.getApprovalCompletedAt()).isNotNull();
    }

    @Test
    void shouldReturnStandardApiResponseFormat() throws Exception {
        ApprovalSetup setup = createSubmittedApproval(adminUser, Role.ADMIN, "Response envelope");

        MvcResult success = mockMvc.perform(get("/api/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}",
                        workspaceOne.getId(),
                        setup.approvalRequest().getId())
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN))))
                .andReturn();
        assertThat(success.getResponse().getStatus()).isEqualTo(200);
        assertThat(json(success).at("/success").asBoolean()).isTrue();
        assertThat(json(success).at("/message").asText()).isNotBlank();
        assertThat(json(success).at("/data/approvalRequest/id").asText()).isEqualTo(setup.approvalRequest().getId().toString());
        assertThat(json(success).at("/errors").isArray()).isTrue();
        assertThat(json(success).at("/timestamp").asText()).isNotBlank();

        MvcResult failure = mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}/reject",
                        workspaceOne.getId(),
                        setup.approvalRequest().getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalRejectRequest(" ")))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(adminUser, workspaceOne.getId(), Role.ADMIN))))
                .andReturn();
        assertThat(failure.getResponse().getStatus()).isEqualTo(400);
        assertThat(json(failure).at("/success").asBoolean()).isFalse();
        assertThat(json(failure).at("/message").asText()).isEqualTo("Validation failed");
        assertThat(json(failure).at("/errors/0/code").asText()).isEqualTo("COMMON-400");
        assertThat(json(failure).at("/timestamp").asText()).isNotBlank();
    }
}
