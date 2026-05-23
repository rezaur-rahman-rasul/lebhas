package com.lebhas.creativesaas.project.application;

import com.lebhas.creativesaas.brand.application.BrandService;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.project.application.dto.ProjectView;
import com.lebhas.creativesaas.project.domain.ProjectEntity;
import com.lebhas.creativesaas.project.infrastructure.persistence.ProjectRepository;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceRepository workspaceRepository;
    private final BrandService brandService;
    private final ProjectRepository projectRepository;
    private final ProjectViewMapper projectViewMapper;

    public ProjectService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            WorkspaceRepository workspaceRepository,
            BrandService brandService,
            ProjectRepository projectRepository,
            ProjectViewMapper projectViewMapper
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.workspaceRepository = workspaceRepository;
        this.brandService = brandService;
        this.projectRepository = projectRepository;
        this.projectViewMapper = projectViewMapper;
    }

    @Transactional(readOnly = true)
    public List<ProjectView> listProjects(UUID workspaceId) {
        workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        return projectRepository.findAllByWorkspaceIdAndDeletedFalseOrderByCreatedAtDesc(workspaceId).stream()
                .map(projectViewMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectEntity requireProject(UUID workspaceId, UUID projectId) {
        return projectRepository.findByIdAndWorkspaceIdAndDeletedFalse(projectId, workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    @Transactional
    public ProjectEntity ensureDefaultProject(UUID workspaceId) {
        return projectRepository.findFirstByWorkspaceIdAndDeletedFalseOrderByCreatedAtAsc(workspaceId)
                .orElseGet(() -> {
                    WorkspaceEntity workspace = workspaceRepository.findByIdAndDeletedFalse(workspaceId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
                    BrandEntity brand = brandService.ensurePrimaryBrand(workspace);
                    return ensureDefaultProject(workspace, brand);
                });
    }

    @Transactional
    public ProjectEntity ensureDefaultProject(WorkspaceEntity workspace, BrandEntity brand) {
        return projectRepository.findFirstByWorkspaceIdAndDeletedFalseOrderByCreatedAtAsc(workspace.getId())
                .orElseGet(() -> projectRepository.save(ProjectEntity.create(
                        workspace.getId(),
                        brand.getId(),
                        defaultProjectName(brand.getName()),
                        "Primary project for " + brand.getName(),
                        null,
                        null)));
    }

    @Transactional
    public UUID requireDefaultProjectId(UUID workspaceId) {
        return ensureDefaultProject(workspaceId).getId();
    }

    private String defaultProjectName(String brandName) {
        return (brandName == null || brandName.isBlank() ? "Default" : brandName.trim()) + " Default Project";
    }
}
