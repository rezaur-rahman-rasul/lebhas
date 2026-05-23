import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  WORKSPACE_LANGUAGE_LABELS,
  WORKSPACE_STATUS_LABELS,
  WORKSPACE_VISIBILITY_LABELS,
} from '../../models/workspace.models';
import { WorkspaceStore } from '../../state/workspace.store';
import { WorkspaceSummaryCardComponent } from '../../components/workspace-summary-card/workspace-summary-card';

@Component({
  selector: 'app-workspace-dashboard-page',
  standalone: true,
  imports: [
    RouterLink,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    IconComponent,
    PageHeaderComponent,
    WorkspaceSummaryCardComponent,
  ],
  templateUrl: './workspace-dashboard-page.html',
  styleUrl: './workspace-dashboard-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceDashboardPageComponent {
  protected readonly store = inject(WorkspaceStore);
  private readonly auth = inject(CurrentUserStore);
  protected readonly workspace = this.store.currentWorkspace;
  protected readonly settings = this.store.workspaceSettings;
  protected readonly skeletonCards = Array.from({ length: 4 }, (_, index) => index);

  protected readonly title = computed(() => this.workspace()?.name || 'Workspace dashboard');
  protected readonly description = computed(() => {
    const workspace = this.workspace();
    if (!workspace) {
      return 'Review workspace identity, brand readiness, and crew access at a glance.';
    }

    return `${workspace.name} is prepared for Day 3 workspace administration, brand setup, and crew operations.`;
  });
  protected readonly crewCountLabel = computed(() => String(this.store.crewMembers().length));
  protected readonly defaultLanguageLabel = computed(() => {
    const language = this.settings()?.defaultLanguage ?? this.workspace()?.language;
    return language ? WORKSPACE_LANGUAGE_LABELS[language] : 'Not set yet';
  });
  protected readonly brandCompletionLabel = computed(() => {
    const completion = this.store.brandProfileCompletion();
    return completion === 100 ? 'Complete' : `${completion}% complete`;
  });
  protected readonly statusLabel = computed(() => {
    const status = this.workspace()?.status;
    return status ? WORKSPACE_STATUS_LABELS[status] : 'Unknown';
  });
  protected readonly visibilityLabel = computed(() => {
    const visibility = this.settings()?.workspaceVisibility;
    return visibility ? WORKSPACE_VISIBILITY_LABELS[visibility] : 'Private';
  });
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'ADMIN');
  protected readonly roleTone = computed(() =>
    this.auth.currentRole() === 'MASTER'
      ? 'red'
      : this.auth.currentRole() === 'CREW'
        ? 'blue'
        : 'brand',
  );

  constructor() {
    void this.store.loadWorkspaceDashboardContext();
  }

  protected reload(): void {
    void this.store.loadWorkspaceDashboardContext();
  }

  protected statusTone(): 'brand' | 'blue' | 'red' | 'neutral' {
    const status = this.workspace()?.status;
    return status === 'ACTIVE' ? 'brand' : status === 'SUSPENDED' ? 'red' : 'neutral';
  }
}
