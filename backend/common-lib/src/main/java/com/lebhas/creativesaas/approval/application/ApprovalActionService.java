package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.application.dto.ApprovalActionCommand;
import com.lebhas.creativesaas.approval.application.dto.ApprovalWorkflowView;
import com.lebhas.creativesaas.approval.domain.ApprovalAction;
import com.lebhas.creativesaas.approval.domain.ApprovalWorkflow;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalWorkflowRepository;
import com.lebhas.creativesaas.approval.validation.ApprovalPermissionValidationService;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.messaging.kafka.KafkaTopicConstants;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApprovalActionService {

    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApprovalHistoryService approvalHistoryService;
    private final ApprovalWorkflowRepository approvalWorkflowRepository;
    private final ApprovalPermissionValidationService approvalPermissionValidationService;
    private final ApprovalMapper approvalMapper;

    public ApprovalActionService(
            ApprovalWorkflowService approvalWorkflowService,
            ApprovalHistoryService approvalHistoryService,
            ApprovalWorkflowRepository approvalWorkflowRepository,
            ApprovalPermissionValidationService approvalPermissionValidationService,
            ApprovalMapper approvalMapper
    ) {
        this.approvalWorkflowService = approvalWorkflowService;
        this.approvalHistoryService = approvalHistoryService;
        this.approvalWorkflowRepository = approvalWorkflowRepository;
        this.approvalPermissionValidationService = approvalPermissionValidationService;
        this.approvalMapper = approvalMapper;
    }

    @Transactional
    public ApprovalWorkflowView approve(ApprovalActionCommand command) {
        return apply(command, ApprovalAction.APPROVE);
    }

    @Transactional
    public ApprovalWorkflowView reject(ApprovalActionCommand command) {
        return apply(command, ApprovalAction.REJECT);
    }

    @Transactional
    public ApprovalWorkflowView requestRevision(ApprovalActionCommand command) {
        return apply(command, ApprovalAction.REQUEST_REVISION);
    }

    private ApprovalWorkflowView apply(ApprovalActionCommand command, ApprovalAction action) {
        WorkspaceAuthorizationService.WorkspaceAccess access =
                approvalPermissionValidationService.requireApprovalVisibility(command.workspaceId());
        ApprovalWorkflow workflow = approvalWorkflowService.requireWorkflow(command.workspaceId(), command.approvalWorkflowId());
        approvalPermissionValidationService.requireApprovalAction(access, workflow);
        approvalWorkflowService.invalidate(workflow);

        if (action == ApprovalAction.APPROVE) {
            workflow.markApproved();
        } else if (action == ApprovalAction.REJECT) {
            workflow.markRejected();
        } else {
            workflow.requestRevision();
        }

        ApprovalWorkflow saved = approvalWorkflowRepository.save(workflow);
        approvalHistoryService.record(saved.getId(), access.currentUser().userId(), action, command.comments());
        approvalWorkflowService.synchronizeGeneratedVersion(saved, command.comments());
        approvalWorkflowService.cache(saved);
        approvalWorkflowService.publish(topicFor(action), saved, access.currentUser().userId(), command.comments());
        return approvalMapper.toView(saved);
    }

    private String topicFor(ApprovalAction action) {
        return switch (action) {
            case APPROVE -> KafkaTopicConstants.APPROVAL_APPROVED;
            case REJECT -> KafkaTopicConstants.APPROVAL_REJECTED;
            case REQUEST_REVISION -> KafkaTopicConstants.APPROVAL_REVISION_REQUESTED;
        };
    }
}
