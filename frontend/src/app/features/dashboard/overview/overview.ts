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
      { label: 'Active workspace', value: this.workspaceId() ? '1' : '0', icon: 'building' },
    ];
  });

  protected readonly quickActions = computed(() => {
    if (this.role() === 'MASTER') {
      return [
        { title: 'Review workspaces', description: 'Check tenant readiness and active workspace access.', route: '/workspaces', icon: 'building-2' },
        { title: 'Inspect users', description: 'Review admin and crew visibility across the shell foundation.', route: '/users', icon: 'users' },
      ];
    }

    if (this.role() === 'CREW') {
      return [
        { title: 'Open projects', description: 'Move into assigned project and campaign context.', route: '/projects', icon: 'folder-kanban' },
        { title: 'Review workspace summary', description: 'Confirm the current workspace before taking action.', route: '/dashboard', icon: 'layout-dashboard' },
      ];
    }

    return [
      { title: 'Manage brands', description: 'Shape the brand layer that products inherit from.', route: '/brands', icon: 'badge-check' },
      { title: 'Manage products', description: 'Keep product and service context ready for projects.', route: '/product-services', icon: 'package-open' },
      { title: 'Manage projects', description: 'Define campaign containers before creative generation starts.', route: '/projects', icon: 'folder-kanban' },
    ];
  });

  protected readonly recentActivity = [
    'JWT auth flow and refresh interceptor are attached to protected routes.',
    'Workspace selection persists across reloads and informs tenant-aware requests.',
    'Brand, product, and project state now sit on signals stores.',
    'Role-aware navigation adapts to MASTER, ADMIN, and CREW entry points.',
  ] as const;

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
