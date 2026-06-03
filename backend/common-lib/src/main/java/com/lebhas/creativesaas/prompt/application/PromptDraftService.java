package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.prompt.application.dto.PromptDraftCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptDraftView;
import com.lebhas.creativesaas.prompt.application.dto.PromptValidationCommand;
import com.lebhas.creativesaas.prompt.domain.PromptDraftEntity;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptDraftRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PromptDraftService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final PromptReadinessService promptReadinessService;
    private final PromptDraftRepository promptDraftRepository;
    private final DomainEventPublisher domainEventPublisher;

    public PromptDraftService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            PromptReadinessService promptReadinessService,
            PromptDraftRepository promptDraftRepository,
            DomainEventPublisher domainEventPublisher
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.promptReadinessService = promptReadinessService;
        this.promptDraftRepository = promptDraftRepository;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public PromptDraftView create(PromptDraftCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.PROMPT_INTELLIGENCE_USE);
        promptReadinessService.assertReady(new PromptValidationCommand(
                command.workspaceId(),
                command.projectId(),
                command.promptText(),
                command.language(),
                List.of(),
                false,
                false,
                false));
        PromptDraftEntity entity = promptDraftRepository.save(PromptDraftEntity.create(
                command.workspaceId(),
                command.projectId(),
                access.currentUser().userId(),
                command.title(),
                command.promptText(),
                command.language(),
                command.platform(),
                command.campaignObjective(),
                command.templateId()));
        publish(KafkaTopicConstants.PROMPT_DRAFT_CREATED, entity, access.currentUser().userId());
        return toView(entity);
    }

    @Transactional
    public PromptDraftView update(UUID workspaceId, UUID projectId, UUID draftId, PromptDraftCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.PROMPT_INTELLIGENCE_USE);
        PromptDraftEntity entity = promptDraftRepository.findByIdAndWorkspaceIdAndDeletedFalse(draftId, workspaceId)
                .filter(draft -> projectId.equals(draft.getProjectId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_DRAFT_NOT_FOUND));
        promptReadinessService.assertReady(new PromptValidationCommand(
                workspaceId,
                projectId,
                command.promptText(),
                command.language(),
                List.of(),
                false,
                false,
                false));
        entity.update(
                command.title(),
                command.promptText(),
                command.language(),
                command.platform(),
                command.campaignObjective(),
                command.templateId());
        promptDraftRepository.save(entity);
        publish(KafkaTopicConstants.PROMPT_DRAFT_UPDATED, entity, access.currentUser().userId());
        return toView(entity);
    }

    @Transactional(readOnly = true)
    public List<PromptDraftView> list(UUID workspaceId, UUID projectId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.PROMPT_INTELLIGENCE_USE);
        promptReadinessService.assertReady(new PromptValidationCommand(
                workspaceId,
                projectId,
                null,
                null,
                List.of(),
                false,
                false,
                false));
        return promptDraftRepository.findAllByWorkspaceIdAndProjectIdAndDeletedFalseOrderByUpdatedAtDesc(workspaceId, projectId)
                .stream()
                .map(this::toView)
                .toList();
    }

    PromptDraftView toView(PromptDraftEntity entity) {
        return new PromptDraftView(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getProjectId(),
                entity.getCreatedByUserId(),
                entity.getTitle(),
                entity.getPromptText(),
                entity.getLanguage(),
                entity.getPlatform(),
                entity.getCampaignObjective(),
                entity.getTemplateId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private void publish(String topic, PromptDraftEntity entity, UUID actorUserId) {
        domainEventPublisher.publish(topic, new BaseDomainEvent(
                topic,
                entity.getWorkspaceId(),
                entity.getId(),
                Instant.now(),
                Map.of(
                        "draftId", entity.getId(),
                        "projectId", entity.getProjectId(),
                        "actorUserId", actorUserId)));
    }
}
