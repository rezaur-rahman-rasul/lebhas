package com.lebhas.creativesaas.workspace.application;

import com.lebhas.creativesaas.brand.application.BrandService;
import com.lebhas.creativesaas.brand.domain.BrandEntity;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.credit.application.CreditWalletService;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipEntity;
import com.lebhas.creativesaas.identity.domain.WorkspaceMembershipStatus;
import com.lebhas.creativesaas.identity.infrastructure.persistence.WorkspaceMembershipRepository;
import com.lebhas.creativesaas.project.application.ProjectService;
import com.lebhas.creativesaas.workspace.domain.BrandProfileEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceEntity;
import com.lebhas.creativesaas.workspace.domain.WorkspaceLanguage;
import com.lebhas.creativesaas.workspace.domain.WorkspaceSettingsEntity;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.BrandProfileRepository;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceSettingsRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Component
public class WorkspaceProvisioningService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceSettingsRepository workspaceSettingsRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final WorkspaceMembershipRepository workspaceMembershipRepository;
    private final WorkspaceSlugService workspaceSlugService;
    private final BrandService brandService;
    private final ProjectService projectService;
    private final CreditWalletService creditWalletService;
    private final Clock clock;

    public WorkspaceProvisioningService(
            WorkspaceRepository workspaceRepository,
            WorkspaceSettingsRepository workspaceSettingsRepository,
            BrandProfileRepository brandProfileRepository,
            WorkspaceMembershipRepository workspaceMembershipRepository,
            WorkspaceSlugService workspaceSlugService,
            BrandService brandService,
            ProjectService projectService,
            CreditWalletService creditWalletService,
            Clock clock
    ) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceSettingsRepository = workspaceSettingsRepository;
        this.brandProfileRepository = brandProfileRepository;
        this.workspaceMembershipRepository = workspaceMembershipRepository;
        this.workspaceSlugService = workspaceSlugService;
        this.brandService = brandService;
        this.projectService = projectService;
        this.creditWalletService = creditWalletService;
        this.clock = clock;
    }

    @Transactional
    public ProvisionedWorkspace provisionOwnedWorkspace(UUID ownerUserId, WorkspaceSeed seed) {
        String resolvedSlug = workspaceSlugService.resolveCreateSlug(seed.slug(), seed.name());
        WorkspaceEntity workspace = workspaceRepository.save(WorkspaceEntity.create(
                seed.name(),
                resolvedSlug,
                seed.logoUrl(),
                seed.description(),
                seed.industry(),
                seed.timezone(),
                seed.language(),
                seed.currency(),
                seed.country(),
                ownerUserId));
        WorkspaceSettingsEntity settings = workspaceSettingsRepository.save(
                WorkspaceSettingsEntity.create(workspace.getId(), seed.language(), seed.timezone()));
        BrandProfileEntity brandProfile = brandProfileRepository.save(
                BrandProfileEntity.create(workspace.getId(), seed.name(), seed.industry()));
        WorkspaceMembershipEntity membership = workspaceMembershipRepository.save(WorkspaceMembershipEntity.create(
                workspace.getId(),
                ownerUserId,
                Role.ADMIN,
                WorkspaceMembershipStatus.ACTIVE,
                java.util.Set.of(),
                clock.instant(),
                null));
        BrandEntity brand = brandService.ensurePrimaryBrand(workspace);
        projectService.ensureDefaultProject(workspace, brand);
        creditWalletService.initializeWallet(workspace.getId());
        return new ProvisionedWorkspace(workspace, settings, brandProfile, membership);
    }

    @Transactional
    public WorkspaceSettingsEntity ensureSettings(WorkspaceEntity workspace) {
        return workspaceSettingsRepository.findById(workspace.getId())
                .orElseGet(() -> workspaceSettingsRepository.save(
                        WorkspaceSettingsEntity.create(workspace.getId(), workspace.getLanguage(), workspace.getTimezone())));
    }

    @Transactional
    public BrandProfileEntity ensureBrandProfile(WorkspaceEntity workspace) {
        return brandProfileRepository.findByWorkspaceIdAndDeletedFalse(workspace.getId())
                .orElseGet(() -> brandProfileRepository.save(
                        BrandProfileEntity.create(workspace.getId(), workspace.getName(), workspace.getIndustry())));
    }

    public record WorkspaceSeed(
            String name,
            String slug,
            String logoUrl,
            String description,
            String industry,
            String timezone,
            WorkspaceLanguage language,
            String currency,
            String country
    ) {
    }

    public record ProvisionedWorkspace(
            WorkspaceEntity workspace,
            WorkspaceSettingsEntity settings,
            BrandProfileEntity brandProfile,
            WorkspaceMembershipEntity membership
    ) {
    }
}
