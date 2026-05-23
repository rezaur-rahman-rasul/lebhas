package com.lebhas.creativesaas.creativerequest.application;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.creativerequest.application.dto.CreativeRequestView;
import com.lebhas.creativesaas.creativerequest.infrastructure.persistence.CreativeRequestRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CreativeRequestFoundationService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final CreativeRequestRepository creativeRequestRepository;
    private final CreativeRequestViewMapper creativeRequestViewMapper;

    public CreativeRequestFoundationService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            CreativeRequestRepository creativeRequestRepository,
            CreativeRequestViewMapper creativeRequestViewMapper
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.creativeRequestRepository = creativeRequestRepository;
        this.creativeRequestViewMapper = creativeRequestViewMapper;
    }

    @Transactional(readOnly = true)
    public List<CreativeRequestView> listCreativeRequests(UUID workspaceId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return creativeRequestRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId)
                .stream()
                .map(creativeRequestViewMapper::toView)
                .toList();
    }
}
