package com.lebhas.creativesaas.approval.application;

import com.lebhas.creativesaas.approval.application.dto.ApprovalHistoryEntryView;
import com.lebhas.creativesaas.approval.domain.ApprovalAction;
import com.lebhas.creativesaas.approval.domain.ApprovalHistory;
import com.lebhas.creativesaas.approval.infrastructure.persistence.ApprovalHistoryRepository;
import com.lebhas.creativesaas.approval.validation.ApprovalWorkflowValidationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ApprovalHistoryService {

    private final ApprovalHistoryRepository approvalHistoryRepository;
    private final ApprovalWorkflowValidationService approvalWorkflowValidationService;
    private final ApprovalMapper approvalMapper;

    public ApprovalHistoryService(
            ApprovalHistoryRepository approvalHistoryRepository,
            ApprovalWorkflowValidationService approvalWorkflowValidationService,
            ApprovalMapper approvalMapper
    ) {
        this.approvalHistoryRepository = approvalHistoryRepository;
        this.approvalWorkflowValidationService = approvalWorkflowValidationService;
        this.approvalMapper = approvalMapper;
    }

    @Transactional
    public ApprovalHistory record(UUID approvalWorkflowId, UUID actionBy, ApprovalAction actionType, String comments) {
        return approvalHistoryRepository.save(ApprovalHistory.record(approvalWorkflowId, actionBy, actionType, comments));
    }

    @Transactional(readOnly = true)
    public List<ApprovalHistoryEntryView> listHistory(UUID workspaceId, UUID approvalWorkflowId) {
        approvalWorkflowValidationService.requireApprovalWorkflowBelongsToWorkspace(workspaceId, approvalWorkflowId);
        return approvalHistoryRepository.findAllByApprovalWorkflowIdOrderByCreatedAtAsc(approvalWorkflowId)
                .stream()
                .map(approvalMapper::toView)
                .toList();
    }
}
