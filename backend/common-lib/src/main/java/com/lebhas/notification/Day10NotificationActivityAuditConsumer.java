package com.lebhas.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lebhas.ai.event.AiProviderHealthChangedEvent;
import com.lebhas.approval.event.ApprovalLifecycleEvent;
import com.lebhas.creativesaas.activity.application.ActivityFeedCommand;
import com.lebhas.creativesaas.activity.application.ActivityFeedService;
import com.lebhas.creativesaas.activity.domain.ActivityCategory;
import com.lebhas.creativesaas.auditlog.application.AuditLogService;
import com.lebhas.creativesaas.auditlog.domain.AuditActionType;
import com.lebhas.creativesaas.auditlog.domain.AuditOutcome;
import com.lebhas.creativesaas.generation.event.CreativeRequestCreatedEvent;
import com.lebhas.creativesaas.generation.event.GenerationCompletedEventDto;
import com.lebhas.creativesaas.generation.event.GenerationFailedEventDto;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.monitoring.application.MonitoringAlertCommand;
import com.lebhas.creativesaas.monitoring.application.MonitoringAlertService;
import com.lebhas.creativesaas.monitoring.application.SystemHealthEventService;
import com.lebhas.creativesaas.monitoring.domain.MonitoringSeverity;
import com.lebhas.creativesaas.monitoring.domain.SystemComponentType;
import com.lebhas.creativesaas.payment.application.event.PaymentTransactionEventDto;
import com.lebhas.creativesaas.payment.application.event.SubscriptionOrderEventDto;
import com.lebhas.creativesaas.sharing.event.ShareLinkCreatedEvent;
import com.lebhas.creativesaas.usage.event.DownloadTrackedEventDto;
import com.lebhas.creativesaas.usage.event.WorkspaceLimitExceededEventDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class Day10NotificationActivityAuditConsumer {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final ActivityFeedService activityFeedService;
    private final AuditLogService auditLogService;
    private final SystemHealthEventService systemHealthEventService;
    private final MonitoringAlertService monitoringAlertService;

    public Day10NotificationActivityAuditConsumer(
            ObjectMapper objectMapper,
            NotificationService notificationService,
            ActivityFeedService activityFeedService,
            AuditLogService auditLogService,
            SystemHealthEventService systemHealthEventService,
            MonitoringAlertService monitoringAlertService
    ) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.activityFeedService = activityFeedService;
        this.auditLogService = auditLogService;
        this.systemHealthEventService = systemHealthEventService;
        this.monitoringAlertService = monitoringAlertService;
    }

    @KafkaListener(
            topics = KafkaTopicConstants.CREATIVE_REQUEST_CREATED,
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeCreativeRequestCreated(Object payload) {
        CreativeRequestCreatedEvent event = objectMapper.convertValue(payload, CreativeRequestCreatedEvent.class);
        String sourceEventId = event.eventId();
        activityFeedService.creativeRequestCreated(
                event.workspaceId(),
                sourceEventId,
                event.requestedBy(),
                event.creativeRequestId(),
                "Creative request created");
        auditLogService.appendUserAction(
                event.workspaceId(),
                sourceEventId,
                event.requestedBy(),
                AuditActionType.CREATE,
                AuditOutcome.SUCCESS,
                "CREATIVE_REQUEST",
                event.creativeRequestId(),
                "Creative request created",
                details("status", event.status(), "projectCampaignId", event.projectCampaignId()),
                null,
                null);
    }

    @KafkaListener(
            topics = KafkaTopicConstants.GENERATION_COMPLETED,
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeGenerationCompleted(Object payload) {
        GenerationCompletedEventDto event = objectMapper.convertValue(payload, GenerationCompletedEventDto.class);
        String sourceEventId = sourceEventId(KafkaTopicConstants.GENERATION_COMPLETED, event.generationJobId(), event.generatedVersionId());
        activityFeedService.generationCompleted(
                event.workspaceId(),
                sourceEventId,
                null,
                event.generatedVersionId(),
                "Generation completed");
        auditLogService.appendUserAction(
                event.workspaceId(),
                sourceEventId,
                null,
                AuditActionType.PROCESS,
                AuditOutcome.SUCCESS,
                "GENERATED_VERSION",
                event.generatedVersionId(),
                "Generation completed",
                details("providerName", event.providerName(), "model", event.model(), "finalizedCredits", event.finalizedCredits()),
                null,
                null);
    }

    @KafkaListener(
            topics = KafkaTopicConstants.GENERATION_FAILED,
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeGenerationFailed(Object payload) {
        GenerationFailedEventDto event = objectMapper.convertValue(payload, GenerationFailedEventDto.class);
        String sourceEventId = sourceEventId(KafkaTopicConstants.GENERATION_FAILED, event.generationJobId(), event.creativeRequestId());
        activityFeedService.create(new ActivityFeedCommand(
                event.workspaceId(),
                sourceEventId,
                null,
                ActivityCategory.GENERATED_VERSION,
                "GENERATION_FAILED",
                "Generation failed",
                event.failureReason(),
                "CREATIVE_REQUEST",
                event.creativeRequestId(),
                event.occurredAt()));
        auditLogService.appendUserAction(
                event.workspaceId(),
                sourceEventId,
                null,
                AuditActionType.PROCESS,
                AuditOutcome.FAILURE,
                "CREATIVE_REQUEST",
                event.creativeRequestId(),
                "Generation failed",
                details("failureReason", event.failureReason(), "retryable", event.retryable()),
                null,
                null);
        systemHealthEventService.generationFailure(
                event.workspaceId(),
                sourceEventId,
                event.failureReason() == null ? "Generation failed" : event.failureReason(),
                details("generationJobId", event.generationJobId(), "retryable", event.retryable()));
        openAlert(
                event.workspaceId(),
                sourceEventId,
                SystemComponentType.CREATIVE,
                "generation",
                MonitoringSeverity.ERROR,
                "Generation failed",
                event.failureReason() == null ? "A generation request failed." : event.failureReason());
    }

    @KafkaListener(
            topics = {KafkaTopicConstants.APPROVAL_APPROVED, KafkaTopicConstants.APPROVAL_REJECTED},
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeApprovalDecision(Object payload) {
        ApprovalLifecycleEvent event = objectMapper.convertValue(payload, ApprovalLifecycleEvent.class);
        boolean approved = event.currentStatus() != null && event.currentStatus().name().contains("APPROVED");
        String sourceEventId = event.eventId();
        activityFeedService.approvalAction(
                event.workspaceId(),
                sourceEventId,
                event.actorId(),
                event.approvalRequestId(),
                approved ? "Approval approved" : "Approval rejected",
                event.details());
        auditLogService.appendUserAction(
                event.workspaceId(),
                sourceEventId,
                event.actorId(),
                approved ? AuditActionType.APPROVE : AuditActionType.REJECT,
                AuditOutcome.SUCCESS,
                "APPROVAL_REQUEST",
                event.approvalRequestId(),
                approved ? "Approval approved" : "Approval rejected",
                details("generatedVersionId", event.generatedVersionId(), "currentStatus", event.currentStatus()),
                null,
                null);
        createNotificationIfPossible(notification(
                event.workspaceId(),
                event.submittedBy(),
                event.actorId(),
                approved ? NotificationType.APPROVAL_APPROVED : NotificationType.APPROVAL_REJECTED,
                approved ? "Approval approved" : "Approval rejected",
                event.details() == null ? (approved ? "Your approval request was approved." : "Your approval request was rejected.") : event.details(),
                "APPROVAL_REQUEST",
                event.approvalRequestId(),
                sourceEventId + ":notification"));
    }

    @KafkaListener(
            topics = KafkaTopicConstants.SHARE_LINK_CREATED,
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeShareLinkCreated(Object payload) {
        ShareLinkCreatedEvent event = objectMapper.convertValue(payload, ShareLinkCreatedEvent.class);
        String sourceEventId = sourceEventId(KafkaTopicConstants.SHARE_LINK_CREATED, event.shareLinkId(), event.generatedVersionId());
        activityFeedService.shareCreated(event.workspaceId(), sourceEventId, event.createdBy(), event.shareLinkId());
        auditLogService.appendUserAction(
                event.workspaceId(),
                sourceEventId,
                event.createdBy(),
                AuditActionType.CREATE,
                AuditOutcome.SUCCESS,
                "SHARE_LINK",
                event.shareLinkId(),
                "Share link created",
                details("generatedVersionId", event.generatedVersionId(), "expiresAt", event.expiresAt()),
                null,
                null);
        createNotificationIfPossible(notification(
                event.workspaceId(),
                event.createdBy(),
                event.createdBy(),
                NotificationType.SHARE_LINK_CREATED,
                "Share link created",
                "A share link was created.",
                "SHARE_LINK",
                event.shareLinkId(),
                sourceEventId + ":notification"));
    }

    @KafkaListener(
            topics = KafkaTopicConstants.DOWNLOAD_TRACKED,
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeDownloadTracked(Object payload) {
        DownloadTrackedEventDto event = objectMapper.convertValue(payload, DownloadTrackedEventDto.class);
        String sourceEventId = sourceEventId(KafkaTopicConstants.DOWNLOAD_TRACKED, event.downloadUsageLogId(), event.assetId());
        activityFeedService.downloadCompleted(event.workspaceId(), sourceEventId, event.downloadedBy(), event.assetId(), "ASSET");
        auditLogService.appendUserAction(
                event.workspaceId(),
                sourceEventId,
                event.downloadedBy(),
                AuditActionType.ACCESS,
                AuditOutcome.SUCCESS,
                "ASSET",
                event.assetId(),
                "Download tracked",
                details("downloadType", event.downloadType(), "generatedVersionId", event.generatedVersionId()),
                null,
                null);
    }

    @KafkaListener(
            topics = KafkaTopicConstants.PAYMENT_TRANSACTION_SUCCEEDED,
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentSucceeded(Object payload) {
        PaymentTransactionEventDto event = objectMapper.convertValue(payload, PaymentTransactionEventDto.class);
        String sourceEventId = sourceEventId(KafkaTopicConstants.PAYMENT_TRANSACTION_SUCCEEDED, event.transactionId(), event.referenceId());
        activityFeedService.paymentCompleted(event.workspaceId(), sourceEventId, event.userId(), event.transactionId());
        auditLogService.appendUserAction(
                event.workspaceId(),
                sourceEventId,
                event.userId(),
                AuditActionType.PURCHASE,
                AuditOutcome.SUCCESS,
                "PAYMENT_TRANSACTION",
                event.transactionId(),
                "Payment transaction succeeded",
                details("paymentPurpose", event.paymentPurpose(), "amount", event.amount(), "currency", event.currency()),
                null,
                null);
        createNotificationIfPossible(notification(
                event.workspaceId(),
                event.userId(),
                event.userId(),
                NotificationType.PAYMENT_TRANSACTION_SUCCEEDED,
                "Payment succeeded",
                "Your payment was completed.",
                "PAYMENT_TRANSACTION",
                event.transactionId(),
                sourceEventId + ":notification"));
    }

    @KafkaListener(
            topics = KafkaTopicConstants.PAYMENT_TRANSACTION_FAILED,
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumePaymentFailed(Object payload) {
        PaymentTransactionEventDto event = objectMapper.convertValue(payload, PaymentTransactionEventDto.class);
        String sourceEventId = sourceEventId(KafkaTopicConstants.PAYMENT_TRANSACTION_FAILED, event.transactionId(), event.referenceId());
        auditLogService.appendUserAction(
                event.workspaceId(),
                sourceEventId,
                event.userId(),
                AuditActionType.PURCHASE,
                AuditOutcome.FAILURE,
                "PAYMENT_TRANSACTION",
                event.transactionId(),
                "Payment transaction failed",
                details("paymentPurpose", event.paymentPurpose(), "failureReason", event.failureReason()),
                null,
                null);
        createNotificationIfPossible(notification(
                event.workspaceId(),
                event.userId(),
                event.userId(),
                NotificationType.PAYMENT_TRANSACTION_FAILED,
                "Payment failed",
                event.failureReason() == null ? "Your payment failed." : event.failureReason(),
                "PAYMENT_TRANSACTION",
                event.transactionId(),
                sourceEventId + ":notification"));
        systemHealthEventService.paymentFailure(
                event.workspaceId(),
                sourceEventId,
                event.failureReason() == null ? "Payment transaction failed" : event.failureReason(),
                details("transactionId", event.transactionId(), "paymentPurpose", event.paymentPurpose()));
        openAlert(
                event.workspaceId(),
                sourceEventId,
                SystemComponentType.PAYMENT,
                "payment",
                MonitoringSeverity.ERROR,
                "Payment failed",
                event.failureReason() == null ? "Payment transaction failed." : event.failureReason());
    }

    @KafkaListener(
            topics = KafkaTopicConstants.SUBSCRIPTION_CHANGED,
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeSubscriptionChanged(Object payload) {
        SubscriptionOrderEventDto event = objectMapper.convertValue(payload, SubscriptionOrderEventDto.class);
        String sourceEventId = sourceEventId(KafkaTopicConstants.SUBSCRIPTION_CHANGED, event.subscriptionOrderId(), event.transactionId());
        activityFeedService.subscriptionChanged(event.workspaceId(), sourceEventId, event.requestedBy(), event.subscriptionOrderId());
        auditLogService.appendUserAction(
                event.workspaceId(),
                sourceEventId,
                event.requestedBy(),
                AuditActionType.UPDATE,
                AuditOutcome.SUCCESS,
                "SUBSCRIPTION_ORDER",
                event.subscriptionOrderId(),
                "Subscription changed",
                details("pricingPlanId", event.pricingPlanId(), "billingCycle", event.billingCycle(), "status", event.status()),
                null,
                null);
        createNotificationIfPossible(notification(
                event.workspaceId(),
                event.requestedBy(),
                event.requestedBy(),
                NotificationType.SUBSCRIPTION_CHANGED,
                "Subscription changed",
                "Workspace subscription was changed.",
                "SUBSCRIPTION_ORDER",
                event.subscriptionOrderId(),
                sourceEventId + ":notification"));
    }

    @KafkaListener(
            topics = {KafkaTopicConstants.AI_PROVIDER_HEALTH_CHANGED, "ai.provider.failed", "ai.provider.recovered"},
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeAiProviderHealth(Object payload) {
        AiProviderHealthChangedEvent event = objectMapper.convertValue(payload, AiProviderHealthChangedEvent.class);
        boolean recovered = event.healthStatus() != null && (event.healthStatus().contains("RECOVER") || event.healthStatus().contains("HEALTH"));
        String sourceEventId = event.eventId();
        if (recovered) {
            systemHealthEventService.aiProviderRecovery(
                    sourceEventId,
                    providerName(event.providerId()),
                    "AI provider recovered",
                    details("healthStatus", event.healthStatus(), "reliabilityScore", event.reliabilityScore()));
            return;
        }
        systemHealthEventService.aiProviderFailure(
                sourceEventId,
                providerName(event.providerId()),
                "AI provider health degraded",
                details("healthStatus", event.healthStatus(), "failedRequests", event.failedRequests()));
        openAlert(
                null,
                sourceEventId,
                SystemComponentType.AI,
                providerName(event.providerId()),
                MonitoringSeverity.ERROR,
                "AI provider degraded",
                "AI provider health changed to " + event.healthStatus());
    }

    @KafkaListener(
            topics = KafkaTopicConstants.WORKSPACE_LIMIT_EXCEEDED,
            groupId = "${platform.day10.kafka.consumer-group:${spring.application.name}-day10}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consumeWorkspaceLimitExceeded(Object payload) {
        WorkspaceLimitExceededEventDto event = objectMapper.convertValue(payload, WorkspaceLimitExceededEventDto.class);
        String sourceEventId = sourceEventId(KafkaTopicConstants.WORKSPACE_LIMIT_EXCEEDED, event.referenceId(), event.workspaceId());
        activityFeedService.workspaceEvent(
                event.workspaceId(),
                sourceEventId,
                null,
                "Workspace limit exceeded",
                event.reason());
        auditLogService.appendUserAction(
                event.workspaceId(),
                sourceEventId,
                null,
                AuditActionType.SYSTEM,
                AuditOutcome.DENIED,
                event.referenceType() == null ? "WORKSPACE_LIMIT" : event.referenceType(),
                event.referenceId(),
                "Workspace limit exceeded",
                details("limitType", event.limitType(), "reason", event.reason()),
                null,
                null);
        systemHealthEventService.storageLimitExceeded(
                event.workspaceId(),
                sourceEventId,
                event.reason() == null ? "Workspace limit exceeded" : event.reason(),
                details("limitType", event.limitType(), "referenceType", event.referenceType()));
        openAlert(
                event.workspaceId(),
                sourceEventId,
                SystemComponentType.STORAGE,
                event.limitType() == null ? "workspace-limit" : event.limitType(),
                MonitoringSeverity.WARNING,
                "Workspace limit exceeded",
                event.reason() == null ? "A workspace limit was exceeded." : event.reason());
    }

    private NotificationCreateRequest notification(
            UUID workspaceId,
            UUID recipientUserId,
            UUID actorUserId,
            NotificationType notificationType,
            String title,
            String message,
            String referenceType,
            UUID referenceId,
            String sourceEventId
    ) {
        if (workspaceId == null || recipientUserId == null || referenceId == null) {
            return null;
        }
        return new NotificationCreateRequest(
                workspaceId,
                recipientUserId,
                actorUserId == null ? recipientUserId : actorUserId,
                notificationType,
                title,
                message,
                referenceType,
                referenceId,
                sourceEventId);
    }

    private void createNotificationIfPossible(NotificationCreateRequest request) {
        if (request != null) {
            notificationService.createInAppNotification(request);
        }
    }

    private void openAlert(
            UUID workspaceId,
            String sourceEventId,
            SystemComponentType componentType,
            String componentName,
            MonitoringSeverity severity,
            String title,
            String description
    ) {
        monitoringAlertService.openAlert(new MonitoringAlertCommand(
                workspaceId,
                componentType.name().toLowerCase(java.util.Locale.ROOT) + ":" + sourceEventId,
                componentType,
                componentName,
                severity,
                title,
                description,
                Instant.now()));
    }

    private Map<String, Object> details(Object... values) {
        java.util.LinkedHashMap<String, Object> details = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < values.length; i += 2) {
            if (values[i] != null && values[i + 1] != null) {
                details.put(values[i].toString(), values[i + 1]);
            }
        }
        return details;
    }

    private String sourceEventId(String topic, UUID primaryId, UUID fallbackId) {
        UUID id = primaryId == null ? fallbackId : primaryId;
        return topic + ":" + (id == null ? UUID.nameUUIDFromBytes(topic.getBytes(java.nio.charset.StandardCharsets.UTF_8)) : id);
    }

    private String providerName(UUID providerId) {
        return providerId == null ? "ai-provider" : "ai-provider:" + providerId;
    }
}
