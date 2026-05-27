import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ActivityEmptyStateComponent } from '../components/activity-empty-state/activity-empty-state';
import { ActivityLoadingStateComponent } from '../components/activity-loading-state/activity-loading-state';
import { TimelineItemComponent } from '../components/timeline-item/timeline-item';
import { ActivityFeed } from '../models/activity.models';
import { ActivityStore } from '../state/activity.store';

interface TimelineGroup {
  readonly date: string;
  readonly activities: readonly ActivityFeed[];
}

@Component({
  selector: 'app-workspace-timeline',
  standalone: true,
  imports: [
    RouterLink,
    ButtonComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    ActivityEmptyStateComponent,
    ActivityLoadingStateComponent,
    TimelineItemComponent,
  ],
  templateUrl: './workspace-timeline.html',
  styleUrl: './workspace-timeline.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceTimelinePage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(ActivityStore);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly accessDenied = computed(() => !this.permissions.canViewActivityFeed());
  protected readonly hasActivities = computed(() => this.store.recentActivities().length > 0);
  protected readonly groupedActivities = computed(() => groupByDate(this.store.recentActivities()));

  constructor() {
    effect(() => {
      const workspaceId = this.activeWorkspaceId();
      if (!workspaceId || this.accessDenied()) {
        return;
      }

      void this.store.loadActivityFeed(workspaceId);
    });
  }

  protected refresh(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || this.accessDenied()) {
      return;
    }

    void this.store.loadActivityFeed(workspaceId);
  }

  protected clearError(): void {
    this.store.clearError();
  }
}

function groupByDate(activities: readonly ActivityFeed[]): readonly TimelineGroup[] {
  const groups = new Map<string, ActivityFeed[]>();
  for (const activity of activities) {
    const date = activity.createdAt.split('T')[0] || activity.createdAt;
    groups.set(date, [...(groups.get(date) ?? []), activity]);
  }

  return Array.from(groups.entries()).map(([date, grouped]) => ({ date, activities: grouped }));
}
