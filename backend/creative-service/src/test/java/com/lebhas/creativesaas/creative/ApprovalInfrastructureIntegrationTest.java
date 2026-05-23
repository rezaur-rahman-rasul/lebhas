package com.lebhas.creativesaas.creative;

import com.fasterxml.jackson.databind.JsonNode;
import com.lebhas.approval.event.ApprovalLifecycleEvent;
import com.lebhas.creativesaas.approval.domain.ApprovalStatus;
import com.lebhas.creativesaas.common.security.Role;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ApprovalInfrastructureIntegrationTest extends AbstractDay6ApprovalIntegrationTest {

    @Test
    void shouldCacheApprovalDataInRedis() throws Exception {
        ApprovalSetup setup = createSubmittedApproval(adminUser, Role.ADMIN, "Cache me");

        assertThat(approvalCacheService.getApprovalRequest(setup.approvalRequest().getId())).isPresent();
        assertThat(approvalCacheService.getApprovalStatus(setup.generatedVersion().getId())).isPresent();
        assertThat(redisTemplate.hasKey(approvalRedisKeys.approvalRequest(setup.approvalRequest().getId())))
                .isEqualTo(Boolean.TRUE);
        assertThat(redisTemplate.hasKey(approvalRedisKeys.approvalStatus(setup.generatedVersion().getId())))
                .isEqualTo(Boolean.TRUE);
    }

    @Test
    void shouldPublishApprovalEventsToKafka() throws Exception {
        Consumer<String, String> consumer = createConsumer(
                approvalKafkaTopicNames.approvalRequestSubmitted(),
                approvalKafkaTopicNames.approvalAssigned(),
                approvalKafkaTopicNames.approvalReviewStarted(),
                approvalKafkaTopicNames.approvalApproved());
        try {
            ApprovalSetup setup = createSubmittedApproval(adminUser, Role.ADMIN, "Publish events");
            assignReviewer(adminUser, Role.ADMIN, setup.approvalRequest().getId(), reviewerUser.getId());
            approve(reviewerUser, Role.CREW, setup.approvalRequest().getId(), "Kafka approved");

            List<ConsumerRecord<String, String>> records = pollRecords(consumer, 4, Duration.ofSeconds(15));
            Set<String> topics = records.stream().map(ConsumerRecord::topic).collect(Collectors.toSet());
            assertThat(topics).contains(
                    approvalKafkaTopicNames.approvalRequestSubmitted(),
                    approvalKafkaTopicNames.approvalAssigned(),
                    approvalKafkaTopicNames.approvalReviewStarted(),
                    approvalKafkaTopicNames.approvalApproved());

            JsonNode submittedEvent = records.stream()
                    .filter(record -> approvalKafkaTopicNames.approvalRequestSubmitted().equals(record.topic()))
                    .findFirst()
                    .map(ConsumerRecord::value)
                    .map(this::readJson)
                    .orElseThrow();
            assertThat(submittedEvent.path("approvalRequestId").asText()).isEqualTo(setup.approvalRequest().getId().toString());
            assertThat(submittedEvent.path("generatedVersionId").asText()).isEqualTo(setup.generatedVersion().getId().toString());
            assertThat(submittedEvent.path("currentStatus").asText()).isEqualTo(ApprovalStatus.SUBMITTED.name());
        } finally {
            closeConsumer(consumer);
        }
    }

    @Test
    void shouldPreventDuplicateReviewWhenRedisLockIsHeld() throws Exception {
        ApprovalSetup setup = createAssignedApproval(adminUser, Role.ADMIN, reviewerUser);
        var token = approvalLockService.acquireReviewSubmissionLock(
                        workspaceOne.getId(),
                        setup.approvalRequest().getId(),
                        setup.generatedVersion().getId(),
                        reviewerUser.getId())
                .orElseThrow();
        try {
            var result = approve(reviewerUser, Role.CREW, setup.approvalRequest().getId(), "Blocked by lock");

            assertThat(result.getResponse().getStatus()).isEqualTo(409);
            assertThat(json(result).at("/errors/0/code").asText()).isEqualTo("APPROVAL-409-03");
            assertThat(approvalReviewRepository.findAllByWorkspaceIdAndApprovalRequestIdAndDeletedFalseOrderByReviewedAtAsc(
                    workspaceOne.getId(),
                    setup.approvalRequest().getId())).isEmpty();
        } finally {
            approvalLockService.releaseLock(
                    token,
                    workspaceOne.getId(),
                    setup.approvalRequest().getId(),
                    setup.generatedVersion().getId(),
                    reviewerUser.getId());
        }
    }

    @Test
    void shouldSynchronizeApprovalStatusFromKafkaEvent() throws Exception {
        ApprovalSetup setup = createSubmittedApproval(adminUser, Role.ADMIN, "Sync status");
        assignReviewer(adminUser, Role.ADMIN, setup.approvalRequest().getId(), reviewerUser.getId());
        kafkaTemplate.send(
                        approvalKafkaTopicNames.approvalApproved(),
                        setup.approvalRequest().getId().toString(),
                        new ApprovalLifecycleEvent(
                                "status-sync-" + UUID.randomUUID(),
                                Instant.now().plusSeconds(30),
                                workspaceOne.getId(),
                                setup.approvalRequest().getId(),
                                setup.generatedVersion().getId(),
                                projectCampaignOne.getId(),
                                adminUser.getId(),
                                reviewerUser.getId(),
                                reviewerUser.getId(),
                                null,
                                ApprovalStatus.IN_REVIEW,
                                ApprovalStatus.APPROVED,
                                Instant.parse("2026-12-31T00:00:00Z"),
                                "Approved from Kafka",
                                false,
                                0))
                .get();

        var synchronizedVersion = awaitGeneratedVersion(
                setup.generatedVersion().getId(),
                version -> version.getApprovalStatus() == com.lebhas.creativesaas.generatedversion.domain.ApprovalStatus.APPROVED
                        && reviewerUser.getId().equals(version.getLatestReviewerId())
                        && "Approved from Kafka".equals(version.getLatestApprovalComment()));

        assertThat(synchronizedVersion.getApprovalCompletedAt()).isNotNull();
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not parse Kafka payload", exception);
        }
    }
}
