package com.lebhas.creativesaas.generation.application;

import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.generation.application.dto.CreativeGenerationListCriteria;
import com.lebhas.creativesaas.generation.application.dto.CreativeGenerationRequestView;
import com.lebhas.creativesaas.generation.application.dto.CreativeOutputView;
import com.lebhas.creativesaas.generation.application.dto.SubmitCreativeGenerationCommand;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationRequestEntity;
import com.lebhas.creativesaas.generation.domain.CreativeGenerationStatus;
import com.lebhas.creativesaas.generation.domain.GenerationJobEntity;
import com.lebhas.creativesaas.generation.domain.GenerationJobType;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativeGenerationRequestRepository;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativeGenerationSpecifications;
import com.lebhas.creativesaas.generation.infrastructure.persistence.CreativeOutputRepository;
import com.lebhas.creativesaas.generation.infrastructure.persistence.GenerationJobRepository;
import com.lebhas.creativesaas.generation.provider.CreativeGenerationRouter;
import com.lebhas.creativesaas.generation.queue.GenerationJobEventPublisher;
import com.lebhas.creativesaas.generation.queue.GenerationJobQueuedEvent;
import com.lebhas.creativesaas.generation.rate.CreativeGenerationThrottleService;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CreativeGenerationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final CreativeGenerationContextAssembler contextAssembler;
    private final CreativeGenerationValidator validator;
    private final CreativeGenerationThrottleService throttleService;
    private final CreativeGenerationCreditService creditService;
    private final CreativeGenerationProperties properties;
    private final CreativeGenerationRouter generationRouter;
    private final CreativeGenerationRequestRepository requestRepository;
    private final GenerationJobRepository jobRepository;
    private final CreativeOutputRepository outputRepository;
    private final CreativeGenerationViewMapper viewMapper;
    private final GenerationJobEventPublisher jobEventPublisher;
    private final CreativeGenerationActivityLogger activityLogger;

    public CreativeGenerationService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            CreativeGenerationContextAssembler contextAssembler,
            CreativeGenerationValidator validator,
            CreativeGenerationThrottleService throttleService,
            CreativeGenerationCreditService creditService,
            CreativeGenerationProperties properties,
            CreativeGenerationRouter generationRouter,
            CreativeGenerationRequestRepository requestRepository,
            GenerationJobRepository jobRepository,
            CreativeOutputRepository outputRepository,
            CreativeGenerationViewMapper viewMapper,
            GenerationJobEventPublisher jobEventPublisher,
            CreativeGenerationActivityLogger activityLogger
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.contextAssembler = contextAssembler;
        this.validator = validator;
        this.throttleService = throttleService;
        this.creditService = creditService;
        this.properties = properties;
        this.generationRouter = generationRouter;
        this.requestRepository = requestRepository;
        this.jobRepository = jobRepository;
        this.outputRepository = outputRepository;
        this.viewMapper = viewMapper;
        this.jobEventPublisher = jobEventPublisher;
        this.activityLogger = activityLogger;
    }

    @Transactional
    public CreativeGenerationRequestView submit(SubmitCreativeGenerationCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(command.workspaceId(), Permission.CREATIVE_GENERATE);
        throttleService.assertAllowed(command.workspaceId(), access.currentUser().userId(), command.clientIp());
        CreativeGenerationContext context = contextAssembler.assemble(command, access);
        validator.validate(context, command.workspaceId(), access.currentUser().userId());
        creditService.validateSufficientCredits(command.workspaceId(), context);

        CreativeGenerationRequestEntity request = CreativeGenerationRequestEntity.queue(
                command.workspaceId(),
                context.projectId(),
                access.currentUser().userId(),
                context.promptHistoryId(),
                context.sourcePrompt(),
                context.enhancedPrompt(),
                context.platform(),
                context.campaignObjective(),
                context.creativeType(),
                context.outputFormat(),
                context.language(),
                context.brandContextSnapshot(),
                context.assetContextSnapshot(),
                context.generationConfigJson(),
                generationRouter.plannedProviderName(context.creativeType()),
                generationRouter.plannedModelName(context.creativeType()));
        requestRepository.saveAndFlush(request);
        creditService.reserveCredits(request);

        GenerationJobEntity job = GenerationJobEntity.queue(
                request.getWorkspaceId(),
                request.getId(),
                context.creativeType().isVideo() ? GenerationJobType.VIDEO_GENERATION : GenerationJobType.IMAGE_GENERATION,
                properties.getQueue().getName(),
                properties.getMaxAttempts());
        jobRepository.save(job);
        activityLogger.logRequestCreated(
                request.getWorkspaceId(),
                request.getId(),
                access.currentUser().userId(),
                request.getCreativeType(),
                context.providerPrompt().length(),
                context.assetCount());
        activityLogger.logJobQueued(request.getWorkspaceId(), request.getId(), job.getId(), job.getQueueName());
        jobEventPublisher.publish(new GenerationJobQueuedEvent(job.getId(), request.getWorkspaceId(), request.getId(), job.getQueueName()));
        return viewMapper.toRequestView(request);
    }

    @Transactional(readOnly = true)
    public PagedResult<CreativeGenerationRequestView> list(CreativeGenerationListCriteria criteria) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(criteria.workspaceId(), Permission.CREATIVE_GENERATE);
        UUID userFilter = visibleUserFilter(access, criteria.userId());
        CreativeGenerationListCriteria scopedCriteria = new CreativeGenerationListCriteria(
                criteria.workspaceId(),
                userFilter,
                criteria.status(),
                criteria.creativeType(),
                criteria.platform(),
                criteria.page(),
                criteria.size());
        return PagedResult.from(requestRepository.findAll(
                        CreativeGenerationSpecifications.forList(scopedCriteria),
                        PageRequest.of(
                                Math.max(scopedCriteria.page(), 0),
                                Math.min(scopedCriteria.size() <= 0 ? DEFAULT_PAGE_SIZE : scopedCriteria.size(), MAX_PAGE_SIZE),
                                Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(viewMapper::toRequestView));
    }

    @Transactional(readOnly = true)
    public CreativeGenerationRequestView get(UUID workspaceId, UUID requestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_GENERATE);
        return viewMapper.toRequestView(requireVisibleRequest(workspaceId, requestId, access));
    }

    @Transactional(readOnly = true)
    public List<CreativeOutputView> outputs(UUID workspaceId, UUID requestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_GENERATE);
        requireVisibleRequest(workspaceId, requestId, access);
        return outputRepository.findByWorkspaceIdAndRequestIdAndDeletedFalseOrderByCreatedAtAsc(workspaceId, requestId)
                .stream()
                .map(viewMapper::toOutputView)
                .toList();
    }

    @Transactional
    public CreativeGenerationRequestView retry(UUID workspaceId, UUID requestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_GENERATE);
        CreativeGenerationRequestEntity request = requireVisibleRequest(workspaceId, requestId, access);
        if (request.getStatus() != CreativeGenerationStatus.FAILED && request.getStatus() != CreativeGenerationStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.GENERATION_STATE_CONFLICT, "Only failed or cancelled generation requests can be retried");
        }
        request.markQueuedForRetry("Retry requested");
        creditService.reserveCredits(request);
        GenerationJobEntity job = GenerationJobEntity.queue(
                request.getWorkspaceId(),
                request.getId(),
                request.getCreativeType().isVideo() ? GenerationJobType.VIDEO_GENERATION : GenerationJobType.IMAGE_GENERATION,
                properties.getQueue().getName(),
                properties.getMaxAttempts());
        requestRepository.save(request);
        jobRepository.save(job);
        activityLogger.logRetryRequested(workspaceId, requestId, access.currentUser().userId(), job.getId());
        activityLogger.logJobQueued(workspaceId, requestId, job.getId(), job.getQueueName());
        jobEventPublisher.publish(new GenerationJobQueuedEvent(job.getId(), workspaceId, requestId, job.getQueueName()));
        return viewMapper.toRequestView(request);
    }

    @Transactional
    public CreativeGenerationRequestView cancel(UUID workspaceId, UUID requestId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService
                .requirePermission(workspaceId, Permission.CREATIVE_GENERATE);
        CreativeGenerationRequestEntity request = requireVisibleRequest(workspaceId, requestId, access);
        if (request.getStatus() == CreativeGenerationStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.GENERATION_STATE_CONFLICT, "Completed generation requests cannot be cancelled");
        }
        if (request.getStatus() != CreativeGenerationStatus.CANCELLED) {
            request.markCancelled("Cancel requested");
            jobRepository.findFirstByRequestIdAndWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(requestId, workspaceId)
                    .filter(job -> !job.isTerminal())
                    .ifPresent(job -> {
                        job.markCancelled("Cancel requested");
                        jobRepository.save(job);
                    });
            creditService.releaseReservedCredits(request);
            requestRepository.save(request);
            activityLogger.logCancelRequested(workspaceId, requestId, access.currentUser().userId());
        }
        return viewMapper.toRequestView(request);
    }

    private UUID visibleUserFilter(WorkspaceAuthorizationService.WorkspaceAccess access, UUID requestedUserFilter) {
        boolean privileged = access.effectiveRole().isMaster() || access.effectiveRole() == Role.ADMIN;
        if (privileged) {
            return requestedUserFilter;
        }
        return access.currentUser().userId();
    }

    private CreativeGenerationRequestEntity requireVisibleRequest(
            UUID workspaceId,
            UUID requestId,
            WorkspaceAuthorizationService.WorkspaceAccess access
    ) {
        CreativeGenerationRequestEntity request = requestRepository.findByIdAndWorkspaceIdAndDeletedFalse(requestId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GENERATION_REQUEST_NOT_FOUND));
        boolean privileged = access.effectiveRole().isMaster() || access.effectiveRole() == Role.ADMIN;
        if (!privileged && !request.getUserId().equals(access.currentUser().userId())) {
            activityLogger.logAuthorizationFailure(workspaceId, access.currentUser().userId(), "generation_request_owner_required");
            throw new BusinessException(ErrorCode.WORKSPACE_ACCESS_DENIED);
        }
        return request;
    }
}
