import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { FeatureLimit } from '@app/core/workspace/workspace.models';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { SectionHeaderComponent } from '@app/shared/components/section-header/section-header';
import { AssetStore } from '../../admin/assets/state/asset.store';
import {
  CreativeGenerationRequest,
  creativeGenerationStatusLabel,
  creativeGenerationStatusTone,
  creativeTypeLabel,
} from '../../admin/creative-generation/models/creative-generation.models';
import { CreativeGenerationStore } from '../../admin/creative-generation/state/creative-generation.store';
import { approvalStatusLabel } from '../../approvals/approval.models';
import { ApprovalStore } from '../../approvals/approval.store';
import { BrandStore } from '../../brands/brand.store';
import { ProductServiceStore } from '../../product-services/product-service.store';
import { ProjectStore } from '../../projects/project.store';
import { DashboardStore } from '../dashboard.store';

type QuickActionKey =
  | 'CREATE_BRAND'
  | 'CREATE_PRODUCT_SERVICE'
  | 'CREATE_PROJECT'
  | 'UPLOAD_ASSET'
  | 'CREATE_PROMPT'
  | 'GENERATE_CREATIVE';

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
  GENERATE_CREATIVE: 'sparkles',
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
  private readonly assets = inject(AssetStore);
  private readonly approvals = inject(ApprovalStore);
  protected readonly generations = inject(CreativeGenerationStore);
  protected readonly dashboard = inject(DashboardStore);

  protected readonly role = this.auth.currentRole;
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly workspaceLabel = this.workspace.workspaceLabel;
  protected readonly skeletonItems = [0, 1, 2, 3, 4, 5] as const;

  protected readonly brandCount = computed(() => (this.permissions.canViewBrands() ? this.brands.total() : 0));
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
  protected readonly validProjectCount = computed(() => (this.productServiceCount() > 0 ? this.validProjects().length : 0));
  protected readonly hasBrand = computed(() => this.brandCount() > 0);
  protected readonly hasProductService = computed(() => this.productServiceCount() > 0);
  protected readonly hasValidProject = computed(() => this.validProjectCount() > 0);
  protected readonly firstValidProject = computed(() => this.validProjects()[0] ?? null);
  protected readonly assetCount = computed(() => this.assets.assets().length);
  protected readonly hasAsset = computed(() => this.assetCount() > 0);
  protected readonly generatedCreativeCount = computed(() => {
    const total = this.generations.generationPagination().totalItems;
    return total > 0 ? total : this.generations.generationRequests().length;
  });
  protected readonly pendingApprovalCount = computed(() =>
    this.approvals
      .approvals()
      .filter((approval) => {
        const label = approvalStatusLabel(approval.status);
        return label === 'Queued' || label === 'In review' || label === 'Changes requested';
      }).length,
  );
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
    if (this.role() === 'MASTER') {
      return 'Monitor tenant context and workspace access before deeper operations.';
    }

    return 'Overview of your workspace performance and recent activity.';
  });

  protected readonly planLabel = computed(() => {
    const label = this.workspace.activePlanLabel();
    return label === 'Package details unavailable' ? '--' : label;
  });
  protected readonly creditsRemaining = computed(() => {
    const credits = this.workspace.usage()?.creditsRemaining;
    return typeof credits === 'number' ? credits : null;
  });
  protected readonly creditLimit = computed(() => this.resolveLimit('credits', 'credit', 'monthlyCredits', 'monthly_credits'));
  protected readonly creditsUsed = computed(() => {
    const limit = this.creditLimit();
    if (typeof limit?.used === 'number') {
      return limit.used;
    }

    if (typeof limit?.limit === 'number' && typeof this.creditsRemaining() === 'number') {
      return Math.max(0, limit.limit - this.creditsRemaining()!);
    }

    return null;
  });
  protected readonly creditsTotal = computed(() => this.creditLimit()?.limit ?? null);
  protected readonly creditUsagePercent = computed(() => {
    const used = this.creditsUsed();
    const total = this.creditsTotal();
    if (typeof used !== 'number' || typeof total !== 'number' || total <= 0) {
      return 0;
    }

    return Math.min(100, Math.max(0, Math.round((used / total) * 100)));
  });
  protected readonly storageLabel = computed(() => {
    const storage = this.workspace.usage()?.storageRemainingBytes;
    return typeof storage === 'number' ? this.formatBytes(storage) : '--';
  });
  protected readonly aiGenerationsLabel = computed(() => {
    const limit = this.workspace.featurePolicy()?.generatedVersionLimit;
    return typeof limit === 'number' ? String(limit) : String(this.generatedCreativeCount());
  });

  protected readonly topStats = computed(() => {
    if (this.role() === 'MASTER') {
      return [
        { label: 'Total Brands', value: '--', trend: 'Select a workspace', icon: 'badge-check' },
        { label: 'Products / Services', value: '--', trend: 'Workspace scoped', icon: 'package-open' },
        { label: 'Projects / Campaigns', value: '--', trend: 'Workspace scoped', icon: 'folder-kanban' },
        { label: 'Generated Creatives', value: '--', trend: 'Workspace scoped', icon: 'sparkles' },
        { label: 'Pending Approvals', value: '--', trend: 'Workspace scoped', icon: 'shield-check' },
        { label: 'Monthly Credit Usage', value: '--', trend: 'Workspace scoped', icon: 'gauge' },
      ];
    }

    return [
      { label: 'Total Brands', value: String(this.brandCount()), trend: this.hasBrand() ? 'Ready for catalog setup' : 'Create a brand first', icon: 'badge-check' },
      { label: 'Products / Services', value: String(this.productServiceCount()), trend: this.hasProductService() ? 'Linked to brands' : 'Add your first product', icon: 'package-open' },
      { label: 'Projects / Campaigns', value: String(this.totalProjectCount()), trend: this.hasValidProject() ? 'Campaign structure ready' : 'Create a campaign container', icon: 'folder-kanban' },
      { label: 'Generated Creatives', value: String(this.generatedCreativeCount()), trend: this.generatedCreativeCount() > 0 ? 'Recent versions available' : 'Generate your first creative', icon: 'sparkles' },
      { label: 'Pending Approvals', value: String(this.pendingApprovalCount()), trend: this.pendingApprovalCount() > 0 ? 'Needs review' : 'No open approvals', icon: 'shield-check' },
      { label: 'Monthly Credit Usage', value: this.creditUsageValue(), trend: this.creditUsageTrend(), icon: 'gauge' },
    ];
  });

  protected readonly workspaceHealthItems = computed(() => [
    { label: 'Current Package', value: this.planLabel(), icon: 'gem' },
    { label: 'Credits Limit', value: this.creditsTotal() === null ? '--' : String(this.creditsTotal()), icon: 'wallet-cards' },
    { label: 'Team Members', value: this.teamMemberLabel(), icon: 'users' },
    { label: 'Storage Used', value: this.storageUsedLabel(), icon: 'database' },
    { label: 'AI Generations', value: this.aiGenerationsLabel(), icon: 'sparkles' },
  ]);

  protected readonly recentActivities = computed(() => {
    const activities = [
      ...this.generations.generationRequests().slice(0, 2).map((request) => ({
        title: 'Creative generated',
        description: `${creativeTypeLabel(request.creativeType)} for ${request.platform ?? 'workspace campaign'}`,
        icon: 'sparkles',
        time: request.updatedAt,
      })),
      ...this.brands.items().slice(0, 1).map((brand) => ({
        title: 'Brand created',
        description: brand.name,
        icon: 'badge-check',
        time: brand.updatedAt,
      })),
      ...this.approvals.approvals().slice(0, 2).map((approval) => ({
        title: approvalStatusLabel(approval.status),
        description: approval.title ?? 'Creative approval',
        icon: 'shield-check',
        time: approval.updatedAt,
      })),
      ...this.assets.assets().slice(0, 2).map((asset) => ({
        title: 'Asset uploaded',
        description: asset.originalFileName,
        icon: 'upload-cloud',
        time: asset.updatedAt,
      })),
    ];

    return activities
      .sort((left, right) => Date.parse(right.time) - Date.parse(left.time))
      .slice(0, 5);
  });

  protected readonly recentCreatives = computed(() => this.generations.generationRequests().slice(0, 4));

  protected readonly primaryAction = computed(() => {
    if (!this.hasBrand()) {
      return { label: 'Create Brand', route: '/brands', icon: 'badge-check' };
    }

    if (!this.hasProductService()) {
      return { label: 'Create Product', route: '/product-services', icon: 'package-open' };
    }

    if (!this.hasValidProject()) {
      return { label: 'Create Project', route: '/projects', icon: 'folder-kanban' };
    }

    if (!this.hasAsset()) {
      const project = this.firstValidProject();
      return { label: 'Upload Asset', route: project ? `/projects/${project.id}/assets` : '/assets', icon: 'upload-cloud' };
    }

    return { label: 'Generate Creative', route: '/creative-generator', icon: 'sparkles' };
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
    const hasAsset = this.hasAsset();
    const projectAssetsRoute = project ? `/projects/${project.id}/assets` : '/projects';
    const projectPromptsRoute = project ? `/projects/${project.id}/prompts` : '/projects';
    const canCreateBrand = this.permissions.canManageBrands();
    const canCreateProduct = this.permissions.canManageProducts();
    const canCreateProject = this.permissions.canCreateProjects();
    const canUploadAsset = this.permissions.has('ASSET_UPLOAD', { requireActiveSubscription: true });
    const canCreatePrompt = this.permissions.canUsePromptBuilder();
    const canGenerateCreative = this.auth.permissions().includes('CREATIVE_GENERATE');

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
        title: 'Create Product / Service',
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
      {
        key: 'GENERATE_CREATIVE',
        title: 'Generate Creative',
        description: hasProducts && hasValidProject && hasAsset && canGenerateCreative ? 'Create campaign-ready visuals.' : 'Add project assets first.',
        route: '/creative-generator',
        icon: this.getQuickActionIcon('GENERATE_CREATIVE'),
        enabled: hasProducts && hasValidProject && hasAsset && canGenerateCreative,
        disabledReason: !canGenerateCreative
          ? 'You do not have permission to generate creatives.'
          : !hasAsset
            ? 'Upload an asset first.'
            : this.downstreamDisabledReason(true, hasProducts, hasValidProject),
        badgeLabel: !canGenerateCreative ? 'Permission required' : hasAsset ? null : 'Asset required',
      },
    ];
  });

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

        if (this.auth.permissions().includes('ASSET_VIEW')) {
          void this.assets.loadLibraryContext();
        }

        if (this.auth.permissions().includes('CREATIVE_GENERATE')) {
          void this.generations.loadGenerationRequests();
        }

        if (this.role() === 'ADMIN') {
          void this.approvals.load();
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

  protected formatActivityTime(value: string): string {
    const timestamp = Date.parse(value);
    if (!Number.isFinite(timestamp)) {
      return '--';
    }

    const diffMinutes = Math.max(0, Math.round((Date.now() - timestamp) / 60000));
    if (diffMinutes < 1) {
      return 'Just now';
    }

    if (diffMinutes < 60) {
      return `${diffMinutes}m ago`;
    }

    const diffHours = Math.round(diffMinutes / 60);
    if (diffHours < 24) {
      return `${diffHours}h ago`;
    }

    return `${Math.round(diffHours / 24)}d ago`;
  }

  protected creativeStatusLabel(request: CreativeGenerationRequest): string {
    return creativeGenerationStatusLabel(request.status);
  }

  protected creativeStatusTone(request: CreativeGenerationRequest): 'brand' | 'blue' | 'red' | 'neutral' {
    return creativeGenerationStatusTone(request.status);
  }

  protected creativeTitle(request: CreativeGenerationRequest): string {
    return request.sourcePrompt?.trim() || creativeTypeLabel(request.creativeType);
  }

  protected creativeMeta(request: CreativeGenerationRequest): string {
    return [request.platform, request.outputFormat].filter(Boolean).join(' / ') || 'Workspace creative';
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

  private resolveLimit(...keys: readonly string[]): FeatureLimit | null {
    const limits = this.workspace.usage()?.limits ?? this.workspace.featurePolicy()?.limits ?? {};
    for (const key of keys) {
      const limit = limits[key];
      if (limit) {
        return limit;
      }
    }

    return null;
  }

  private creditUsageValue(): string {
    const used = this.creditsUsed();
    const total = this.creditsTotal();
    if (typeof used === 'number' && typeof total === 'number') {
      return `${used}/${total}`;
    }

    return this.creditsRemaining() === null ? '--' : String(this.creditsRemaining());
  }

  private creditUsageTrend(): string {
    const remaining = this.creditsRemaining();
    return typeof remaining === 'number' ? `${remaining} credits remaining` : 'Credits: --';
  }

  private teamMemberLabel(): string {
    return this.auth.currentUser() ? '1+' : '--';
  }

  private storageUsedLabel(): string {
    const limit = this.resolveLimit('assets.storage', 'asset.storage', 'storage');
    if (typeof limit?.used === 'number') {
      return this.formatBytes(limit.used);
    }

    return this.storageLabel();
  }

  private formatBytes(value: number): string {
    if (value <= 0) {
      return '0 MB';
    }

    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const index = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1);
    const amount = value / Math.pow(1024, index);
    return `${amount >= 10 ? amount.toFixed(0) : amount.toFixed(1)} ${units[index]}`;
  }
}
