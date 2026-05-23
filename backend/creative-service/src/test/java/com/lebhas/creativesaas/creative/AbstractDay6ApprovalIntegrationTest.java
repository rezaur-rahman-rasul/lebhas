package com.lebhas.creativesaas.creative;

import com.lebhas.approval.dto.ApprovalApproveRequest;
import com.lebhas.approval.dto.ApprovalAssignRequest;
import com.lebhas.approval.dto.ApprovalCommentCreateRequest;
import com.lebhas.approval.dto.ApprovalRejectRequest;
import com.lebhas.approval.dto.ApprovalRequestChangesRequest;
import com.lebhas.approval.dto.ApprovalResubmitRequest;
import com.lebhas.approval.dto.ApprovalSubmitRequest;
import com.lebhas.approval.event.ApprovalKafkaTopicNames;
import com.lebhas.creativesaas.approval.cache.ApprovalCacheService;
import com.lebhas.creativesaas.approval.cache.ApprovalLockService;
import com.lebhas.creativesaas.approval.cache.ApprovalRedisKeys;
import com.lebhas.creativesaas.approval.domain.ApprovalRequest;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalAssignmentRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalAuditLogRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalCommentRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalRequestRepository;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalReviewRepository;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.creativerequest.domain.CreativeRequestEntity;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionEntity;
import com.lebhas.creativesaas.generatedversion.domain.GeneratedVersionStatus;
import com.lebhas.creativesaas.generatedversion.domain.GenerationStatus;
import com.lebhas.creativesaas.identity.domain.UserEntity;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import org.apache.kafka.clients.consumer.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

abstract class AbstractDay6ApprovalIntegrationTest extends AbstractDay4BackendIntegrationTest {

    @Autowired
    protected ApprovalRequestRepository approvalRequestRepository;

    @Autowired
    protected ApprovalAssignmentRepository approvalAssignmentRepository;

    @Autowired
    protected ApprovalReviewRepository approvalReviewRepository;

    @Autowired
    protected ApprovalCommentRepository approvalCommentRepository;

    @Autowired
    protected ApprovalAuditLogRepository approvalAuditLogRepository;

    @Autowired
    protected ApprovalCacheService approvalCacheService;

    @Autowired
    protected ApprovalRedisKeys approvalRedisKeys;

    @Autowired
    protected ApprovalLockService approvalLockService;

    @Autowired
    protected ApprovalKafkaTopicNames approvalKafkaTopicNames;

    @Autowired
    protected KafkaTemplate<String, Object> kafkaTemplate;

    protected UserEntity reviewerUser;
    protected UserEntity alternateReviewerUser;
    protected UserEntity submitterCrewUser;

    @BeforeEach
    void setUpApprovalFixtures() {
        reviewerUser = createWorkspaceUser("day6-reviewer", Role.CREW, Set.of(Permission.CREATIVE_SUBMIT));
        alternateReviewerUser = createWorkspaceUser("day6-alt-reviewer", Role.CREW, Set.of(Permission.CREATIVE_SUBMIT));
        submitterCrewUser = createWorkspaceUser("day6-submitter", Role.CREW, Set.of(Permission.CREATIVE_SUBMIT));
    }

    protected GeneratedVersionEntity createReadyGeneratedVersion(UserEntity requestedBy, String requestName) {
        CreativeRequestEntity creativeRequest = creativeRequestRepository.save(CreativeRequestEntity.create(
                workspaceOne.getId(),
                projectCampaignOne.getId(),
                requestedBy.getId(),
                requestName,
                "Create a launch-ready approval asset",
                null,
                "Awareness",
                "Instagram",
                "Square",
                java.util.List.of(),
                null));
        return generatedVersionRepository.save(GeneratedVersionEntity.create(
                workspaceOne.getId(),
                creativeRequest.getId(),
                projectCampaignOne.getId(),
                1,
                requestName + " v1",
                null,
                null,
                GenerationStatus.READY,
                com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.NOT_SUBMITTED,
                true,
                "mock-provider",
                "mock-model",
                requestedBy.getId(),
                GeneratedVersionStatus.ACTIVE));
    }

    protected ApprovalSetup createSubmittedApproval(UserEntity submitter, Role role, String comment) throws Exception {
        GeneratedVersionEntity version = createReadyGeneratedVersion(submitter, "Day 6 Approval " + UUID.randomUUID());
        MvcResult result = submitApproval(submitter, role, version.getId(), Instant.parse("2026-12-31T00:00:00Z"), comment);
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        UUID approvalRequestId = uuidAt(result, "/data/id");
        return new ApprovalSetup(
                reloadGeneratedVersion(version.getId()),
                reloadApprovalRequest(approvalRequestId));
    }

    protected ApprovalSetup createAssignedApproval(UserEntity submitter, Role submitterRole, UserEntity reviewer) throws Exception {
        ApprovalSetup setup = createSubmittedApproval(submitter, submitterRole, "Please review this version");
        MvcResult result = assignReviewer(adminUser, Role.ADMIN, setup.approvalRequest().getId(), reviewer.getId());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        return new ApprovalSetup(
                reloadGeneratedVersion(setup.generatedVersion().getId()),
                reloadApprovalRequest(setup.approvalRequest().getId()));
    }

    protected MvcResult submitApproval(
            UserEntity actor,
            Role role,
            UUID generatedVersionId,
            Instant dueAt,
            String submissionComment
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/generated-versions/{generatedVersionId}/submit-approval",
                        workspaceOne.getId(),
                        generatedVersionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalSubmitRequest(dueAt, submissionComment)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(actor, workspaceOne.getId(), role))))
                .andReturn();
    }

    protected MvcResult assignReviewer(
            UserEntity actor,
            Role role,
            UUID approvalRequestId,
            UUID reviewerId
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}/assign",
                        workspaceOne.getId(),
                        approvalRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalAssignRequest(reviewerId)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(actor, workspaceOne.getId(), role))))
                .andReturn();
    }

    protected MvcResult approve(
            UserEntity actor,
            Role role,
            UUID approvalRequestId,
            String feedback
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}/approve",
                        workspaceOne.getId(),
                        approvalRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalApproveRequest(feedback)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(actor, workspaceOne.getId(), role))))
                .andReturn();
    }

    protected MvcResult reject(
            UserEntity actor,
            Role role,
            UUID approvalRequestId,
            String feedback
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}/reject",
                        workspaceOne.getId(),
                        approvalRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalRejectRequest(feedback)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(actor, workspaceOne.getId(), role))))
                .andReturn();
    }

    protected MvcResult requestChanges(
            UserEntity actor,
            Role role,
            UUID approvalRequestId,
            String feedback
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}/request-changes",
                        workspaceOne.getId(),
                        approvalRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalRequestChangesRequest(feedback)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(actor, workspaceOne.getId(), role))))
                .andReturn();
    }

    protected MvcResult resubmit(
            UserEntity actor,
            Role role,
            UUID approvalRequestId,
            String comment
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}/resubmit",
                        workspaceOne.getId(),
                        approvalRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalResubmitRequest(comment)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(actor, workspaceOne.getId(), role))))
                .andReturn();
    }

    protected MvcResult addComment(
            UserEntity actor,
            Role role,
            UUID approvalRequestId,
            String commentText,
            boolean internalOnly
    ) throws Exception {
        return mockMvc.perform(post("/api/v1/workspaces/{workspaceId}/approval-requests/{approvalRequestId}/comments",
                        workspaceOne.getId(),
                        approvalRequestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalCommentCreateRequest(commentText, internalOnly)))
                        .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(actor, workspaceOne.getId(), role))))
                .andReturn();
    }

    protected MvcResult listApprovals(UserEntity actor, Role role, String... params) throws Exception {
        var builder = get("/api/v1/workspaces/{workspaceId}/approval-requests", workspaceOne.getId())
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenFor(actor, workspaceOne.getId(), role)));
        for (int index = 0; index + 1 < params.length; index += 2) {
            builder.param(params[index], params[index + 1]);
        }
        return mockMvc.perform(builder).andReturn();
    }

    protected ApprovalRequest reloadApprovalRequest(UUID approvalRequestId) {
        return approvalRequestRepository.findByIdAndWorkspaceIdAndDeletedFalse(approvalRequestId, workspaceOne.getId())
                .orElseThrow();
    }

    protected GeneratedVersionEntity reloadGeneratedVersion(UUID generatedVersionId) {
        return generatedVersionRepository.findByIdAndWorkspaceIdAndDeletedFalse(generatedVersionId, workspaceOne.getId())
                .orElseThrow();
    }

    protected void setSubmittedAt(UUID approvalRequestId, Instant submittedAt) {
        ApprovalRequest request = reloadApprovalRequest(approvalRequestId);
        ReflectionTestUtils.setField(request, "submittedAt", submittedAt);
        approvalRequestRepository.save(request);
    }

    protected GeneratedVersionEntity awaitGeneratedVersion(
            UUID generatedVersionId,
            Predicate<GeneratedVersionEntity> predicate
    ) throws InterruptedException {
        Instant deadline = Instant.now().plusSeconds(10);
        GeneratedVersionEntity current = null;
        while (Instant.now().isBefore(deadline)) {
            current = reloadGeneratedVersion(generatedVersionId);
            if (predicate.test(current)) {
                return current;
            }
            Thread.sleep(200L);
        }
        throw new AssertionError("GeneratedVersion condition was not satisfied in time for " + generatedVersionId + ": " + current);
    }

    protected UserEntity createWorkspaceUser(String emailPrefix, Role role, Set<Permission> permissions) {
        UserEntity user = userRepository.save(UserEntity.register(
                "Approval",
                emailPrefix,
                emailPrefix + "-" + UUID.randomUUID() + "@example.com",
                null,
                "{noop}unused",
                role,
                UserStatus.ACTIVE,
                true));
        workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                workspaceOne.getId(),
                user.getId(),
                role,
                WorkspaceMembershipStatus.ACTIVE,
                permissions,
                Instant.now(),
                adminUser.getId()));
        return user;
    }

    protected void closeConsumer(Consumer<String, String> consumer) {
        if (consumer != null) {
            consumer.close();
        }
    }

    protected record ApprovalSetup(
            GeneratedVersionEntity generatedVersion,
            ApprovalRequest approvalRequest
    ) {
    }
}
