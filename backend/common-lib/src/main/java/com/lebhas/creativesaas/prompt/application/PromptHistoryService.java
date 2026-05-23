package com.lebhas.creativesaas.prompt.application;

import com.lebhas.creativesaas.campaign.application.ProjectCampaignService;
import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.project.application.ProjectService;
import com.lebhas.creativesaas.prompt.application.dto.PromptHistoryFilter;
import com.lebhas.creativesaas.prompt.application.dto.PromptHistoryView;
import com.lebhas.creativesaas.prompt.domain.CampaignObjective;
import com.lebhas.creativesaas.prompt.domain.PromptHistoryEntity;
import com.lebhas.creativesaas.prompt.domain.PromptLanguage;
import com.lebhas.creativesaas.prompt.domain.PromptPlatform;
import com.lebhas.creativesaas.prompt.domain.SuggestionType;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptHistoryRepository;
import com.lebhas.creativesaas.prompt.infrastructure.persistence.PromptHistorySpecifications;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PromptHistoryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final ProjectCampaignService projectCampaignService;
    private final ProjectService projectService;
    private final PromptHistoryRepository promptHistoryRepository;
    private final PromptViewMapper promptViewMapper;
    private final PromptActivityLogger promptActivityLogger;

    public PromptHistoryService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            ProjectCampaignService projectCampaignService,
            ProjectService projectService,
            PromptHistoryRepository promptHistoryRepository,
            PromptViewMapper promptViewMapper,
            PromptActivityLogger promptActivityLogger
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.projectCampaignService = projectCampaignService;
        this.projectService = projectService;
        this.promptHistoryRepository = promptHistoryRepository;
        this.promptViewMapper = promptViewMapper;
        this.promptActivityLogger = promptActivityLogger;
    }

    @Transactional(readOnly = true)
    public PagedResult<PromptHistoryView> listHistory(PromptHistoryFilter filter) {
        requireHistoryViewAccess(filter.workspaceId(), "history_list");
        if (filter.projectId() != null) {
            projectCampaignService.requireProjectCampaign(filter.workspaceId(), filter.projectId());
        }
        return PagedResult.from(promptHistoryRepository.findAll(
                        PromptHistorySpecifications.forList(filter),
                        PageRequest.of(
                                Math.max(filter.page(), 0),
                                Math.min(filter.size() <= 0 ? DEFAULT_PAGE_SIZE : filter.size(), MAX_PAGE_SIZE),
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(promptViewMapper::toHistoryView));
    }

    @Transactional(readOnly = true)
    public PromptHistoryView getHistory(UUID workspaceId, UUID projectId, UUID historyId) {
        requireHistoryViewAccess(workspaceId, "history_get");
        if (projectId != null) {
            projectCampaignService.requireProjectCampaign(workspaceId, projectId);
        }
        PromptHistoryEntity entity = promptHistoryRepository.findByIdAndWorkspaceIdAndDeletedFalse(historyId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROMPT_HISTORY_NOT_FOUND));
        if (projectId != null && !belongsToProject(entity, projectId)) {
            throw new BusinessException(ErrorCode.PROMPT_HISTORY_NOT_FOUND);
        }
        return promptViewMapper.toHistoryView(entity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PromptHistoryEntity recordSuccess(
            UUID workspaceId,
            UUID projectId,
            UUID creativeRequestId,
            UUID userId,
            String sourcePrompt,
            String outputPayload,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType,
            String aiProvider,
            String aiModel,
            Integer tokenUsage
    ) {
        UUID resolvedProjectId = resolveProjectId(workspaceId, projectId);
        return promptHistoryRepository.save(PromptHistoryEntity.success(
                workspaceId,
                resolvedProjectId,
                resolvedProjectId,
                creativeRequestId,
                userId,
                sourcePrompt,
                outputPayload,
                language,
                platform,
                campaignObjective,
                businessType,
                brandContextSnapshot,
                suggestionType,
                aiProvider,
                aiModel,
                tokenUsage));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PromptHistoryEntity recordFailure(
            UUID workspaceId,
            UUID projectId,
            UUID creativeRequestId,
            UUID userId,
            String sourcePrompt,
            PromptLanguage language,
            PromptPlatform platform,
            CampaignObjective campaignObjective,
            String businessType,
            String brandContextSnapshot,
            SuggestionType suggestionType,
            String aiProvider,
            String aiModel
    ) {
        UUID resolvedProjectId = resolveProjectId(workspaceId, projectId);
        return promptHistoryRepository.save(PromptHistoryEntity.failure(
                workspaceId,
                resolvedProjectId,
                resolvedProjectId,
                creativeRequestId,
                userId,
                sourcePrompt,
                language,
                platform,
                campaignObjective,
                businessType,
                brandContextSnapshot,
                suggestionType,
                aiProvider,
                aiModel));
    }

    private UUID resolveProjectId(UUID workspaceId, UUID projectId) {
        if (projectId != null) {
            return projectCampaignService.requireProjectCampaign(workspaceId, projectId).getId();
        }
        return projectService.requireDefaultProjectId(workspaceId);
    }

    private boolean belongsToProject(PromptHistoryEntity entity, UUID projectId) {
        return projectId.equals(entity.getProjectCampaignId())
                || (entity.getProjectCampaignId() == null && projectId.equals(entity.getProjectId()));
    }

    private void requireHistoryViewAccess(UUID workspaceId, String operation) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        if (access.effectiveRole().isMaster()
                || access.permissions().contains(Permission.PROMPT_HISTORY_VIEW)
                || access.permissions().contains(Permission.PROMPT_TEMPLATE_MANAGE)) {
            return;
        }
        promptActivityLogger.logAuthorizationFailure(operation, workspaceId, access.currentUser().userId(), "missing_prompt_history_permission");
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }
}
