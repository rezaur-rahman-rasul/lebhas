package com.lebhas.creativesaas.generatedversion.application;

import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.generatedversion.application.dto.GeneratedVersionView;
import com.lebhas.creativesaas.generatedversion.infrastructure.persistence.GeneratedVersionRepository;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GeneratedVersionFoundationService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final GeneratedVersionRepository generatedVersionRepository;
    private final GeneratedVersionViewMapper generatedVersionViewMapper;

    public GeneratedVersionFoundationService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            GeneratedVersionRepository generatedVersionRepository,
            GeneratedVersionViewMapper generatedVersionViewMapper
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.generatedVersionRepository = generatedVersionRepository;
        this.generatedVersionViewMapper = generatedVersionViewMapper;
    }

    @Transactional(readOnly = true)
    public List<GeneratedVersionView> listGeneratedVersions(UUID workspaceId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return generatedVersionRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId)
                .stream()
                .map(generatedVersionViewMapper::toView)
                .toList();
    }
}
