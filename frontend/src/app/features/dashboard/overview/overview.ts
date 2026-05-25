import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { SectionHeaderComponent } from '@app/shared/components/section-header/section-header';
import { BrandStore } from '../../brands/brand.store';
import { ProductServiceStore } from '../../product-services/product-service.store';
import { ProjectStore } from '../../projects/project.store';

@Component({
  selector: 'app-dashboard-overview',
  standalone: true,
  imports: [
    RouterLink,
    BadgeComponent,
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
  private readonly workspace = inject(WorkspaceStore);
  private readonly brands = inject(BrandStore);
  private readonly products = inject(ProductServiceStore);
  private readonly projects = inject(ProjectStore);

  protected readonly role = this.auth.currentRole;
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly workspaceLabel = this.workspace.workspaceLabel;
  protected readonly activePlanLabel = this.workspace.activePlanLabel;
  protected readonly subscriptionStatusLabel = this.workspace.subscriptionStatusLabel;
  protected readonly remainingCreditsLabel = this.workspace.remainingCreditsLabel;
  protected readonly firstProject = computed(() => this.projects.items()[0] ?? null);

  protected readonly description = computed(() => {
    const role = this.role();
    const workspaceLabel = this.workspaceLabel();

    if (role === 'MASTER') {
      return 'Monitor tenant context, role-aware access, and shell readiness before deeper workspace operations arrive.';
    }

    if (role === 'CREW') {
      return `${workspaceLabel} is ready for crew-facing campaign execution and related creative follow-up.`;
    }

    return `${workspaceLabel} is ready for brand, product, and campaign operations with the Day 2 hierarchy in place.`;
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
      { label: 'Brands', value: String(this.permissions.canViewBrands() ? this.brands.total() : 0), icon: 'badge-check' },
      { label: 'Products / Services', value: String(this.permissions.canViewProducts() ? this.products.total() : 0), icon: 'package-open' },
      { label: 'Projects / Campaigns', value: String(this.permissions.canViewProjects() ? this.projects.total() : 0), icon: 'folder-kanban' },
        { label: 'Credits / limits', value: this.remainingCreditsLabel(), icon: 'gauge' },
      ];
  });

  protected readonly quickActions = computed(() => {
    if (this.role() === 'MASTER') {
      return [
        { title: 'Review dashboard', description: 'Check tenant readiness and active workspace access.', route: '/dashboard', icon: 'layout-dashboard' },
        { title: 'Review approvals', description: 'Inspect approval workflow readiness inside the active workspace.', route: '/approvals', icon: 'shield-check' },
      ];
    }

    if (this.role() === 'CREW') {
      return [
        { title: 'Open projects', description: 'Move into assigned project and campaign context.', route: '/projects', icon: 'folder-kanban' },
        { title: 'Review workspace summary', description: 'Confirm the current workspace before taking action.', route: '/dashboard', icon: 'layout-dashboard' },
      ];
    }

    const project = this.firstProject();
    const projectAssetsRoute = project ? `/projects/${project.id}/assets` : '/projects';
    const projectPromptsRoute = project ? `/projects/${project.id}/prompts` : '/projects';

    return [
      { title: 'Create Brand', description: 'Start the brand layer for products and campaigns.', route: '/brands', icon: 'badge-check' },
      { title: 'Create Product/Service', description: 'Attach a product or service to a brand.', route: '/product-services', icon: 'package-open' },
      { title: 'Create Project', description: 'Prepare a campaign container for assets and prompts.', route: '/projects', icon: 'folder-kanban' },
      { title: 'Upload Asset', description: project ? 'Add source media to the first project.' : 'Create or select a project first.', route: projectAssetsRoute, icon: 'upload-cloud' },
      { title: 'Create Prompt', description: project ? 'Open prompt building for the first project.' : 'Create or select a project first.', route: projectPromptsRoute, icon: 'sparkles' },
    ];
  });

  protected readonly hierarchy = computed(() => [
    { label: 'Workspace', value: this.workspaceLabel(), route: '/dashboard', icon: 'building-2' },
    { label: 'Brand', value: `${this.brands.total()} ready`, route: '/brands', icon: 'badge-check' },
    { label: 'Product/Service', value: `${this.products.total()} linked`, route: '/product-services', icon: 'package-open' },
    { label: 'Project/Campaign', value: `${this.projects.total()} active`, route: '/projects', icon: 'folder-kanban' },
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

      if (this.permissions.canViewBrands()) {
        void this.brands.load(workspaceId);
      }

      if (this.permissions.canViewProducts()) {
        void this.products.load(workspaceId);
      }

      if (this.permissions.canViewProjects()) {
        void this.projects.load(workspaceId);
      }
    });
  }
}
