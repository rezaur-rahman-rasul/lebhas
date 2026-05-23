import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';

import { Permission } from '@app/features/auth/models/user.models';
import { WorkspaceSummaryCardComponent } from '@app/features/admin/workspace/components/workspace-summary-card/workspace-summary-card';
import {
  WORKSPACE_LANGUAGE_LABELS,
  WORKSPACE_STATUS_LABELS,
} from '@app/features/admin/workspace/models/workspace.models';
import { WorkspaceStore } from '@app/features/admin/workspace/state/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';

@Component({
  selector: 'app-crew-home',
  standalone: true,
  imports: [
    BadgeComponent,
    CardComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    WorkspaceSummaryCardComponent,
  ],
  templateUrl: './crew-home.html',
  styleUrl: './crew-home.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CrewHomeComponent {
  protected readonly store = inject(WorkspaceStore);
  protected readonly workspace = this.store.currentWorkspace;
  protected readonly brandProfile = this.store.brandProfile;
  protected readonly skeletonCards = Array.from({ length: 3 }, (_, index) => index);
  protected readonly permissions = computed(
    () => this.workspace()?.currentUserPermissions ?? [],
  );
  protected readonly title = computed(() => this.workspace()?.name || 'Assigned workspace');
  protected readonly description = computed(() => {
    const workspace = this.workspace();
    if (!workspace) {
      return 'Review workspace context and brand guidance in read-only mode.';
    }

    return `${workspace.name} is available in read-only mode with your current crew permissions.`;
  });
  protected readonly permissionCountLabel = computed(() => `${this.permissions().length} enabled`);
  protected readonly brandProfileLabel = computed(() =>
    this.store.isBrandProfileComplete() ? 'Ready' : `${this.store.brandProfileCompletion()}% ready`,
  );
  protected readonly statusLabel = computed(() => {
    const status = this.workspace()?.status;
    return status ? WORKSPACE_STATUS_LABELS[status] : 'Unknown';
  });
  protected readonly languageLabel = computed(() => {
    const language = this.workspace()?.language;
    return language ? WORKSPACE_LANGUAGE_LABELS[language] : 'Language pending';
  });

  constructor() {
    void this.store.loadCrewReadonlyContext();
  }

  protected permissionLabel(permission: Permission): string {
    return permission
      .toLowerCase()
      .split('_')
      .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }

  protected statusTone(): 'brand' | 'red' | 'neutral' {
    const status = this.workspace()?.status;
    return status === 'ACTIVE' ? 'brand' : status === 'SUSPENDED' ? 'red' : 'neutral';
  }
}
