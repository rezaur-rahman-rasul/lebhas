package com.lebhas.creativesaas.workspace.application;

import com.lebhas.creativesaas.brand.application.BrandService;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.workspace.application.dto.BrandProfileView;
import com.lebhas.creativesaas.workspace.application.dto.UpdateBrandProfileCommand;
import com.lebhas.creativesaas.workspace.domain.BrandProfileEntity;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.BrandProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceBrandProfileService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceProvisioningService workspaceProvisioningService;
    private final BrandProfileRepository brandProfileRepository;
    private final WorkspaceViewMapper workspaceViewMapper;
    private final WorkspaceActivityLogger workspaceActivityLogger;
    private final BrandService brandService;

    public WorkspaceBrandProfileService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            WorkspaceProvisioningService workspaceProvisioningService,
            BrandProfileRepository brandProfileRepository,
            WorkspaceViewMapper workspaceViewMapper,
            WorkspaceActivityLogger workspaceActivityLogger,
            BrandService brandService
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.workspaceProvisioningService = workspaceProvisioningService;
        this.brandProfileRepository = brandProfileRepository;
        this.workspaceViewMapper = workspaceViewMapper;
        this.workspaceActivityLogger = workspaceActivityLogger;
        this.brandService = brandService;
    }

    @Transactional
    public BrandProfileView getBrandProfile(java.util.UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requirePermission(workspaceId, Permission.WORKSPACE_VIEW);
        BrandProfileEntity profile = brandProfileRepository.findByWorkspaceIdAndDeletedFalse(access.workspace().getId())
                .orElseGet(() -> workspaceProvisioningService.ensureBrandProfile(access.workspace()));
        return workspaceViewMapper.toBrandProfileView(profile);
    }

    @Transactional
    public BrandProfileView updateBrandProfile(UpdateBrandProfileCommand command) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceOwnerOrMaster(command.workspaceId());
        BrandProfileEntity profile = brandProfileRepository.findByWorkspaceIdAndDeletedFalse(access.workspace().getId())
                .orElseGet(() -> workspaceProvisioningService.ensureBrandProfile(access.workspace()));
        profile.update(
                command.brandName(),
                command.businessType(),
                command.industry(),
                command.targetAudience(),
                command.brandVoice(),
                command.preferredCta(),
                command.primaryColor(),
                command.secondaryColor(),
                command.website(),
                command.facebookUrl(),
                command.instagramUrl(),
                command.linkedinUrl(),
                command.tiktokUrl(),
                command.description());
        brandProfileRepository.save(profile);
        brandService.syncPrimaryBrand(
                access.workspace(),
                command.brandName(),
                command.businessType(),
                command.industry(),
                command.targetAudience(),
                command.brandVoice(),
                command.preferredCta(),
                command.primaryColor(),
                command.secondaryColor(),
                command.website(),
                command.facebookUrl(),
                command.instagramUrl(),
                command.linkedinUrl(),
                command.tiktokUrl());
        workspaceActivityLogger.logBrandProfileUpdated(access.workspace().getId(), access.currentUser().userId());
        return workspaceViewMapper.toBrandProfileView(profile);
    }
}
