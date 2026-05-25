import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { SectionHeaderComponent } from '@app/shared/components/section-header/section-header';

@Component({
  selector: 'app-dashboard-home',
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
  templateUrl: './dashboard-home.html',
  styleUrl: './dashboard-home.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardHomeComponent {
  private readonly auth = inject(CurrentUserStore);
  private readonly workspace = inject(WorkspaceStore);

  protected readonly description = computed(() => {
    const user = this.auth.currentUser();
    if (!user) {
      return 'Workspace shell is ready.';
    }

    const workspaceLabel = this.workspace.workspaceLabel();
    return `${workspaceLabel} is ready for ${user.role.toLowerCase()} workflows across brands, products, projects, and creative requests.`;
  });

  protected readonly primaryRoute = computed(() => {
    const role = this.auth.currentRole();
    return role === 'MASTER'
      ? '/dashboard'
      : role === 'CREW'
        ? '/projects'
        : '/brands';
  });

  protected readonly metrics = computed(() => {
    const role = this.auth.currentRole();
    if (role === 'MASTER') {
      return [
        { label: 'Connected workspaces', value: String(this.workspace.workspaces().length || 2), icon: 'building-2' },
        { label: 'User scopes', value: '3', icon: 'users' },
        { label: 'Support lanes', value: '1', icon: 'shield-check' },
        { label: 'System views', value: '4', icon: 'layout-dashboard' },
      ];
    }

    if (role === 'CREW') {
      return [
        { label: 'Assigned projects', value: '4', icon: 'folder-kanban' },
        { label: 'Creative requests', value: '12', icon: 'pencil-line' },
        { label: 'Generated versions', value: '27', icon: 'image' },
        { label: 'Export queues', value: '3', icon: 'download' },
      ];
    }

    return [
      { label: 'Brands', value: '3', icon: 'badge-check' },
      { label: 'Products', value: '24', icon: 'grid-2x2' },
      { label: 'Campaigns', value: '6', icon: 'folder-kanban' },
      { label: 'Creative requests', value: '18', icon: 'pencil-line' },
    ];
  });

  protected readonly quickActions = computed(() => {
    const role = this.auth.currentRole();
    if (role === 'MASTER') {
      return [
        { title: 'Review dashboard', description: 'Check tenant boundaries and active workspace context.', route: '/dashboard', icon: 'layout-dashboard' },
        { title: 'Review approvals', description: 'Confirm approval workflow readiness.', route: '/approvals', icon: 'shield-check' },
      ];
    }

    if (role === 'CREW') {
      return [
        { title: 'Open assigned projects', description: 'Move into the crew-facing workload surface.', route: '/projects', icon: 'folder-kanban' },
        { title: 'Review approvals', description: 'Review generated creative approval readiness.', route: '/approvals', icon: 'shield-check' },
      ];
    }

    return [
      { title: 'Open brand foundation', description: 'Prepare brand-level setup inside the active workspace.', route: '/brands', icon: 'badge-check' },
      { title: 'Review product hierarchy', description: 'Shape products and services before request intake.', route: '/product-services', icon: 'grid-2x2' },
    ];
  });

  protected readonly recentActivity = [
    'Workspace shell initialized with role-aware navigation',
    'Dark theme loaded from persistent preference',
    'Auth interceptor chain attached to protected routes',
    'Dashboard foundation prepared for project and creative request modules',
  ] as const;
}
