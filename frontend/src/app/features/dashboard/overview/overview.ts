import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { SectionHeaderComponent } from '@app/shared/components/section-header/section-header';
import { BrandStore } from '../../brands/brand.store';
import { ProductServiceStore } from '../../product-services/product-service.store';
import { ProjectStore } from '../../projects/project.store';
import { DashboardStore } from '../dashboard.store';

type QuickActionKey = 'CREATE_BRAND' | 'CREATE_PRODUCT_SERVICE' | 'CREATE_PROJECT' | 'UPLOAD_ASSET' | 'CREATE_PROMPT';

interface QuickAction {
  readonly key: QuickActionKey | 'REVIEW_DASHBOARD' | 'REVIEW_APPROVALS' | 'OPEN_PROJECTS' | 'REVIEW_WORKSPACE';
  readonly title: string;
  readonly description: string;
  readonly route: string;
  readonly icon: string;
  readonly enabled: boolean;
  readonly disabledReason: string | null;
  readonly badgeLabel: string | null;
}

const QUICK_ACTION_ICONS: Record<QuickActionKey, string> = {
  CREATE_BRAND: 'shield-check',
  CREATE_PRODUCT_SERVICE: 'package-open',
  CREATE_PROJECT: 'folder-kanban',
  UPLOAD_ASSET: 'upload-cloud',
  CREATE_PROMPT: 'wand-sparkles',
};

const QUICK_ACTION_FALLBACK_ICON = 'circle-help';

@Component({
  selector: 'app-dashboard-overview',
  standalone: true,
  imports: [
    RouterLink,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    IconComponent,
    SectionHeaderComponent,
  ],
  templateUrl: './overview.html',
  styleUrl: './overview.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardOverviewComponent {
  private readonly auth = inject(CurrentUserStore);
  private readonly permissions = inject(PermissionStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly workspace = inject(WorkspaceStore);
  private readonly brands = inject(BrandStore);
  private readonly products = inject(ProductServiceStore);
  private readonly projects = inject(ProjectStore);
  protected readonly dashboard = inject(DashboardStore);

  protected readonly role = this.auth.currentRole;
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly workspaceLabel = this.workspace.workspaceLabel;
  protected readonly activePlanLabel = this.workspace.activePlanLabel;
  protected readonly subscriptionStatusLabel = this.workspace.subscriptionStatusLabel;
  protected readonly remainingCreditsLabel = this.workspace.remainingCreditsLabel;
  protected readonly brandCount = computed(() => this.permissions.canViewBrands() ? this.brands.total() : 0);
  protected readonly productServiceCount = computed(() =>
    this.permissions.canViewProducts() ? this.products.total() : 0,
  );
  protected readonly totalProjectCount = computed(() =>
    this.permissions.canViewProjects() ? this.projects.total() : 0,
  );
  protected readonly validProjects = computed(() => {
    const productIds = new Set(this.products.items().map((product) => product.id));

    return this.projects.items().filter((project) => {
      const linkedProductService = (project as unknown as { readonly linkedProductService?: boolean }).linkedProductService;
      const productServiceName = (project as unknown as { readonly productServiceName?: string | null }).productServiceName;

      if (linkedProductService === true) {
        return true;
      }

      if (productServiceName && productServiceName.trim().toLowerCase() !== 'unknown product') {
        return true;
      }

      return Boolean(project.productServiceId && productIds.has(project.productServiceId));
    });
  });
  protected readonly validProjectCount = computed(() => this.productServiceCount() > 0 ? this.validProjects().length : 0);
  protected readonly hasBrand = computed(() => this.brandCount() > 0);
  protected readonly hasProductService = computed(() => this.productServiceCount() > 0);
  protected readonly hasValidProject = computed(() => this.validProjectCount() > 0);
  protected readonly hasProjectLinkWarning = computed(
    () => this.totalProjectCount() > 0 && !this.hasValidProject(),
  );
  protected readonly firstValidProject = computed(() => this.validProjects()[0] ?? null);
  protected readonly skeletonItems = [0, 1, 2, 3, 4] as const;
  protected readonly hierarchySkeletonItems = [0, 1, 2, 3] as const;
  protected readonly showWorkspaceError = computed(
    () => this.role() !== 'MASTER' && !this.workspace.loading() && !this.workspaceId(),
  );
  protected readonly showSkeleton = computed(() => {
    if (this.auth.authLoading() || this.workspace.loading()) {
      return true;
    }

    if (this.dashboard.friendlyError() || this.showWorkspaceError()) {
      return false;
    }

    return Boolean(this.workspaceId()) && !this.dashboard.ready();
  });
  protected readonly showDashboardError = computed(
    () => !this.showSkeleton() && Boolean(this.dashboard.friendlyError()),
  );

  protected readonly description = computed(() => {
    const role = this.role();
    const workspaceLabel = this.workspaceLabel();

    if (role === 'MASTER') {
      return 'Monitor tenant context, role-aware access, and shell readiness before deeper workspace operations arrive.';
    }

    if (role === 'CREW') {
      return `${workspaceLabel} is ready for crew-facing campaign execution and related creative follow-up.`;
    }

    return `${workspaceLabel} is ready for brand, product, and campaign operations with the workspace hierarchy in place.`;
  });

  protected readonly metrics = computed(() => {
    if (this.role() === 'MASTER') {
      return [
        { label: 'Accessible workspaces', value: String(this.workspace.workspaces().length), icon: 'building-2' },
        { label: 'Admin surfaces', value: '3', icon: 'shield-check' },
        { label: 'Support routes', value: '2', icon: 'life-buoy' },
        { label: 'System overview', value: '1', icon: 'layout-dashboard' },
      ];
    }

    return [
      { label: 'Brands', value: String(this.brandCount()), icon: 'badge-check' },
      { label: 'Products / Services', value: String(this.productServiceCount()), icon: 'package-open' },
      { label: 'Projects / Campaigns', value: String(this.totalProjectCount()), icon: 'folder-kanban' },
      { label: 'Credits / limits', value: this.remainingCreditsLabel(), icon: 'gauge' },
      ];
  });

  protected readonly quickActions = computed<readonly QuickAction[]>(() => {
    if (this.role() === 'MASTER') {
      return [
        { key: 'REVIEW_DASHBOARD', title: 'Review dashboard', description: 'Check tenant readiness and active workspace access.', route: '/dashboard', icon: 'layout-dashboard', enabled: true, disabledReason: null, badgeLabel: null },
        { key: 'REVIEW_APPROVALS', title: 'Review approvals', description: 'Inspect approval workflow readiness inside the active workspace.', route: '/approvals', icon: 'shield-check', enabled: true, disabledReason: null, badgeLabel: null },
      ];
    }

    if (this.role() === 'CREW') {
      return [
        { key: 'OPEN_PROJECTS', title: 'Open projects', description: 'Move into assigned project and campaign context.', route: '/projects', icon: 'folder-kanban', enabled: true, disabledReason: null, badgeLabel: null },
        { key: 'REVIEW_WORKSPACE', title: 'Review workspace summary', description: 'Confirm the current workspace before taking action.', route: '/dashboard', icon: 'layout-dashboard', enabled: true, disabledReason: null, badgeLabel: null },
      ];
    }

    const project = this.firstValidProject();
    const hasBrands = this.hasBrand();
    const hasProducts = this.hasProductService();
    const hasValidProject = this.hasValidProject();
    const projectAssetsRoute = project ? `/projects/${project.id}/assets` : '/projects';
    const projectPromptsRoute = project ? `/projects/${project.id}/prompts` : '/projects';
    const canCreateBrand = this.permissions.canManageBrands();
    const canCreateProduct = this.permissions.canManageProducts();
    const canCreateProject = this.permissions.canCreateProjects();
    const canUploadAsset = this.permissions.has('ASSET_UPLOAD', { requireActiveSubscription: true });
    const canCreatePrompt = this.permissions.canUsePromptBuilder();

    return [
      {
        key: 'CREATE_BRAND',
        title: 'Create Brand',
        description: canCreateBrand ? 'Start the brand layer for products and campaigns.' : 'You do not have permission to create brands.',
        route: '/brands',
        icon: this.getQuickActionIcon('CREATE_BRAND'),
        enabled: canCreateBrand,
        disabledReason: canCreateBrand ? null : 'You do not have permission to perform this action.',
        badgeLabel: canCreateBrand ? null : 'Permission required',
      },
      {
        key: 'CREATE_PRODUCT_SERVICE',
        title: 'Create Product/Service',
        description: hasBrands && canCreateProduct ? 'Attach a product or service to a brand.' : 'Create a brand first.',
        route: '/product-services',
        icon: this.getQuickActionIcon('CREATE_PRODUCT_SERVICE'),
        enabled: hasBrands && canCreateProduct,
        disabledReason: !canCreateProduct ? 'You do not have permission to perform this action.' : hasBrands ? null : 'Create a brand first.',
        badgeLabel: !canCreateProduct ? 'Permission required' : hasBrands ? null : 'Brand required',
      },
      {
        key: 'CREATE_PROJECT',
        title: 'Create Project',
        description: hasProducts && canCreateProject ? 'Prepare a campaign container for assets and prompts.' : 'Create a product or service first.',
        route: '/projects',
        icon: this.getQuickActionIcon('CREATE_PROJECT'),
        enabled: hasProducts && canCreateProject,
        disabledReason: !canCreateProject ? 'You do not have permission to perform this action.' : hasProducts ? null : 'Create a product or service first.',
        badgeLabel: !canCreateProject ? 'Permission required' : hasProducts ? null : 'Product required',
      },
      {
        key: 'UPLOAD_ASSET',
        title: 'Upload Asset',
        description: hasProducts && hasValidProject && canUploadAsset ? 'Add source media to the first project.' : 'Create a linked project first.',
        route: projectAssetsRoute,
        icon: this.getQuickActionIcon('UPLOAD_ASSET'),
        enabled: hasProducts && hasValidProject && canUploadAsset,
        disabledReason: this.downstreamDisabledReason(canUploadAsset, hasProducts, hasValidProject),
        badgeLabel: !canUploadAsset ? 'Permission required' : 'Project required',
      },
      {
        key: 'CREATE_PROMPT',
        title: 'Create Prompt',
        description: hasProducts && hasValidProject && canCreatePrompt ? 'Open prompt building for the first project.' : 'Create a linked project first.',
        route: projectPromptsRoute,
        icon: this.getQuickActionIcon('CREATE_PROMPT'),
        enabled: hasProducts && hasValidProject && canCreatePrompt,
        disabledReason: this.downstreamDisabledReason(canCreatePrompt, hasProducts, hasValidProject),
        badgeLabel: !canCreatePrompt ? 'Permission required' : 'Project required',
      },
    ];
  });

  protected readonly hierarchy = computed(() => [
    { label: 'Workspace', value: this.workspaceLabel(), route: '/dashboard', icon: 'building-2' },
    { label: 'Brand', value: `${this.brandCount()} ready`, route: '/brands', icon: 'badge-check' },
    { label: 'Product/Service', value: `${this.productServiceCount()} linked`, route: '/product-services', icon: 'package-open' },
    {
      label: 'Project/Campaign',
      value: this.hasProjectLinkWarning()
        ? `${this.totalProjectCount()} active · product link missing`
        : `${this.totalProjectCount()} active`,
      route: '/projects',
      icon: 'folder-kanban',
    },
  ]);

  protected readonly subscriptionSummaries = computed(() => [
    { label: 'Package', value: this.activePlanLabel(), icon: 'credit-card' },
    { label: 'Status', value: this.subscriptionStatusLabel(), icon: 'shield-check' },
    { label: 'Credits / limits', value: this.remainingCreditsLabel(), icon: 'gauge' },
  ]);

  constructor() {
    effect(() => {
      const workspaceId = this.workspaceId();
      if (!workspaceId) {
        return;
      }

      void this.dashboard.load(workspaceId);

      if (this.role() === 'ADMIN') {
        if (this.permissions.canViewBrands()) {
          void this.brands.load(workspaceId);
        }

        if (this.permissions.canViewProducts()) {
          void this.products.load(workspaceId);
        }

        if (this.permissions.canViewProjects()) {
          void this.projects.load(workspaceId);
        }
      }
    });
  }

  protected retry(): void {
    void this.dashboard.retry();
  }

  protected showDisabledActionToast(action: QuickAction): void {
    if (!action.disabledReason) {
      return;
    }

    this.notifications.info(action.disabledReason, undefined, `dashboard-action-${action.key}-${action.disabledReason}`);
  }

  protected getQuickActionIcon(actionType: QuickActionKey | string): string {
    return QUICK_ACTION_ICONS[actionType as QuickActionKey] ?? QUICK_ACTION_FALLBACK_ICON;
  }

  private downstreamDisabledReason(canUseFeature: boolean, hasProducts: boolean, hasValidProject: boolean): string | null {
    if (!canUseFeature) {
      return 'You do not have permission to perform this action.';
    }

    if (!hasProducts) {
      return 'Create a product or service first.';
    }

    return hasValidProject ? null : 'Create a linked project first.';
  }
}
