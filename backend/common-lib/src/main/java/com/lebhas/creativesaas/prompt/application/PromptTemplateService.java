package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.BaseDomainEvent;
import com.lebhas.creativesaas.messaging.kafka.DomainEventPublisher;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import com.lebhas.creativesaas.prompt.application.dto.CreatePromptTemplateCommand;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateFilter;
import com.lebhas.creativesaas.prompt.application.dto.PromptTemplateView;
import com.lebhas.creativesaas.prompt.application.dto.UpdatePromptTemplateCommand;
import com.lebhas.creativesaas.prompt.cache.PromptTemplateCacheService;
import com.lebhas.creativesaas.prompt.cache.dto.PromptTemplateCacheEntry;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateEntity;
import com.lebhas.creativesaas.prompt.domain.PromptTemplateStatus;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptTemplateRepository;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptTemplateSpecifications;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PromptTemplateService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final PromptTemplateRepository promptTemplateRepository;
    private final PromptTemplateMapper promptTemplateMapper;
    private final PromptTemplateCacheService promptTemplateCacheService;
    private final PromptActivityLogger promptActivityLogger;
    private final DomainEventPublisher domainEventPublisher;

    public PromptTemplateService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            PromptTemplateRepository promptTemplateRepository,
            PromptTemplateMapper promptTemplateMapper,
            PromptTemplateCacheService promptTemplateCacheService,
            PromptActivityLogger promptActivityLogger,
            DomainEventPublisher domainEventPublisher
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.promptTemplateRepository = promptTemplateRepository;
        this.promptTemplateMapper = promptTemplateMapper;
        this.promptTemplateCacheService = promptTemplateCacheService;
        this.promptActivityLogger = promptActivityLogger;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Transactional
    public PromptTemplateView createTemplate(CreatePromptTemplateCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requireTemplateManageAccess(command.workspaceId(), command.systemDefault(), "template_create");
        PromptTemplateEntity entity = PromptTemplateEntity.create(
                command.workspaceId(),
                command.name(),
                command.category(),
                command.description(),
                command.platform(),
                command.campaignObjective(),
                command.businessType(),
                command.language(),
                command.templateText(),
                command.isPublic(),
                command.systemDefault(),
                command.status());
        promptTemplateRepository.save(entity);
        promptTemplateCacheService.store(entity);
        promptActivityLogger.logTemplateCreated(command.workspaceId(), access.currentUser().userId(), entity.getId(), entity.isSystemDefault());
        publishPromptTemplateCreatedSafely(entity, access.currentUser().userId());
        return promptTemplateMapper.toView(entity);
    }

    @Transactional(readOnly = true)
    public List<PromptTemplateView> listTemplates(PromptTemplateFilter filter) {
        requireTemplateReadAccess(filter.workspaceId(), "template_list");
        return promptTemplateRepository.findAll(
                        PromptTemplateSpecifications.forList(filter),
                        Sort.by(Sort.Order.desc("isDefault"), Sort.Order.desc("updatedAt")))
                .stream()
                .map(promptTemplateMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PromptTemplateView getTemplate(UUID workspaceId, UUID templateId) {
        requireTemplateReadAccess(workspaceId, "template_get");
        PromptTemplateCacheEntry cached = promptTemplateCacheService.getOrLoad(workspaceId, templateId);
        if (cached == null || !isAccessibleInWorkspace(cached, workspaceId)) {
            throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_NOT_FOUND);
        }
        return promptTemplateMapper.toView(cached);
    }

    @Transactional
    public PromptTemplateView updateTemplate(UpdatePromptTemplateCommand command) {
        PromptTemplateEntity entity = requireAccessibleTemplate(command.workspaceId(), command.templateId());
        WorkspaceAuthorizationService.WorkspaceAccess access = requireTemplateManageAccess(command.workspaceId(), entity.isSystemDefault() || command.systemDefault(), "template_update");
        entity.update(
                command.workspaceId(),
                command.name(),
                command.category(),
                command.description(),
                command.platform(),
                command.campaignObjective(),
                command.businessType(),
                command.language(),
                command.templateText(),
                command.isPublic(),
                command.systemDefault(),
                command.status() == null ? PromptTemplateStatus.ACTIVE : command.status());
        promptTemplateRepository.save(entity);
        promptTemplateCacheService.store(entity);
        promptActivityLogger.logTemplateUpdated(command.workspaceId(), access.currentUser().userId(), entity.getId(), entity.isSystemDefault());
        return promptTemplateMapper.toView(entity);
    }

    @Transactional
    public void deleteTemplate(UUID workspaceId, UUID templateId) {
        PromptTemplateEntity entity = requireAccessibleTemplate(workspaceId, templateId);
        WorkspaceAuthorizationService.WorkspaceAccess access = requireTemplateManageAccess(workspaceId, entity.isSystemDefault(), "template_delete");
        entity.markDeleted();
        promptTemplateRepository.save(entity);
        promptTemplateCacheService.invalidate(workspaceId, templateId);
        promptActivityLogger.logTemplateDeleted(workspaceId, access.currentUser().userId(), entity.getId(), entity.isSystemDefault());
    }

    @Transactional(readOnly = true)
    public PromptTemplateEntity requireUsableTemplate(UUID workspaceId, UUID templateId) {
        PromptTemplateEntity entity = requireAccessibleTemplate(workspaceId, templateId);
        if (!entity.isActiveTemplate()) {
            throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_NOT_FOUND);
        }
        return entity;
    }

    private PromptTemplateEntity requireAccessibleTemplate(UUID workspaceId, UUID templateId) {
        PromptTemplateEntity entity = promptTemplateRepository.findByIdAndDeletedFalse(templateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_TEMPLATE_NOT_FOUND));
        if (!entity.isAccessibleInWorkspace(workspaceId)) {
            throw new BusinessException(ErrorCode.PROMPT_TEMPLATE_NOT_FOUND);
        }
        return entity;
    }

    private WorkspaceAuthorizationService.WorkspaceAccess requireTemplateReadAccess(UUID workspaceId, String operation) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        if (access.effectiveRole().isMaster()
                || access.permissions().contains(Permission.PROMPT_TEMPLATE_VIEW)
                || access.permissions().contains(Permission.PROMPT_TEMPLATE_MANAGE)) {
            return access;
        }
        promptActivityLogger.logAuthorizationFailure(operation, workspaceId, access.currentUser().userId(), "missing_prompt_template_permission");
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private WorkspaceAuthorizationService.WorkspaceAccess requireTemplateManageAccess(UUID workspaceId, boolean systemDefault, String operation) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        if (systemDefault) {
            if (access.currentUser().isMaster()) {
                return access;
            }
            promptActivityLogger.logAuthorizationFailure(operation, workspaceId, access.currentUser().userId(), "system_template_master_required");
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (access.effectiveRole().isMaster() || access.permissions().contains(Permission.PROMPT_TEMPLATE_MANAGE)) {
            return access;
        }
        promptActivityLogger.logAuthorizationFailure(operation, workspaceId, access.currentUser().userId(), "missing_prompt_template_manage_permission");
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private boolean isAccessibleInWorkspace(PromptTemplateCacheEntry entry, UUID workspaceId) {
        return entry.systemDefault() || workspaceId.equals(entry.workspaceId());
    }

    private void publishPromptTemplateCreatedSafely(PromptTemplateEntity entity, UUID actorUserId) {
        try {
            domainEventPublisher.publish(
                    KafkaTopicConstants.PROMPT_TEMPLATE_CREATED,
                    new BaseDomainEvent(
                            KafkaTopicConstants.PROMPT_TEMPLATE_CREATED,
                            entity.getWorkspaceId(),
                            entity.getId(),
                            Instant.now(),
                            Map.of(
                                    "templateId", entity.getId(),
                                    "actorUserId", actorUserId,
                                    "name", entity.getName(),
                                    "category", entity.getCategory() == null ? "" : entity.getCategory(),
                                    "isPublic", entity.isPublic(),
                                    "systemDefault", entity.isSystemDefault())));
        } catch (RuntimeException ignored) {
        }
    }
}
