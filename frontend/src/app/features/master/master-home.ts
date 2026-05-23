import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { WorkspaceSummaryCardComponent } from '@app/features/admin/workspace/components/workspace-summary-card/workspace-summary-card';
import {
  WORKSPACE_LANGUAGE_LABELS,
  WORKSPACE_STATUS_LABELS,
} from '@app/features/admin/workspace/models/workspace.models';
import { WorkspaceStore } from '@app/features/admin/workspace/state/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';

@Component({
  selector: 'app-master-home',
  standalone: true,
  imports: [
    DatePipe,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    WorkspaceSummaryCardComponent,
  ],
  templateUrl: './master-home.html',
  styleUrl: './master-home.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MasterHomeComponent {
  protected readonly store = inject(WorkspaceStore);
  private readonly auth = inject(CurrentUserStore);
  private readonly router = inject(Router);
  protected readonly workspaces = this.store.accessibleWorkspaces;
  protected readonly activeWorkspaceId = this.auth.activeWorkspaceId;
  protected readonly skeletonCards = Array.from({ length: 3 }, (_, index) => index);
  protected readonly totalWorkspacesLabel = computed(() => String(this.workspaces().length));
  protected readonly activeWorkspacesLabel = computed(
    () => String(this.workspaces().filter((workspace) => workspace.status === 'ACTIVE').length),
  );
  protected readonly attentionWorkspacesLabel = computed(
    () =>
      String(
        this.workspaces().filter(
          (workspace) => workspace.status === 'SUSPENDED' || workspace.status === 'ARCHIVED',
        ).length,
      ),
  );

  constructor() {
    void this.store.loadAccessibleWorkspaces();
  }

  protected languageLabel(language: keyof typeof WORKSPACE_LANGUAGE_LABELS): string {
    return WORKSPACE_LANGUAGE_LABELS[language];
  }

  protected statusLabel(status: keyof typeof WORKSPACE_STATUS_LABELS): string {
    return WORKSPACE_STATUS_LABELS[status];
  }

  protected statusTone(status: 'ACTIVE' | 'SUSPENDED' | 'ARCHIVED'): 'brand' | 'red' | 'neutral' {
    return status === 'ACTIVE' ? 'brand' : status === 'SUSPENDED' ? 'red' : 'neutral';
  }

  protected async openWorkspace(workspaceId: string, route = '/admin'): Promise<void> {
    this.store.selectWorkspace(workspaceId);
    await this.router.navigateByUrl(route);
  }
}
