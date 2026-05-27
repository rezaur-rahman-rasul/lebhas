import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppDrawerComponent } from '@app/shared/components/app-drawer/app-drawer';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ActivityCardComponent, safeMetadataSummary } from '../components/activity-card/activity-card';
import { ActivityEmptyStateComponent } from '../components/activity-empty-state/activity-empty-state';
import { ActivityFilterBarComponent } from '../components/activity-filter-bar/activity-filter-bar';
import { ActivityLoadingStateComponent } from '../components/activity-loading-state/activity-loading-state';
import { ActivityTypeBadgeComponent } from '../components/activity-type-badge/activity-type-badge';
import { ActivityFeed, ActivityType, DateRangeFilter } from '../models/activity.models';
import { ActivityStore } from '../state/activity.store';

@Component({
  selector: 'app-activity-feed',
  standalone: true,
  imports: [
    RouterLink,
    ButtonComponent,
    AppDrawerComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    ActivityCardComponent,
    ActivityEmptyStateComponent,
    ActivityFilterBarComponent,
    ActivityLoadingStateComponent,
    ActivityTypeBadgeComponent,
  ],
  templateUrl: './activity-feed.html',
  styleUrl: './activity-feed.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityFeedPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(ActivityStore);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly accessDenied = computed(() => !this.permissions.canViewActivityFeed());
  protected readonly hasActivities = computed(() => this.store.activities().length > 0);
  protected readonly selectedActivity = signal<ActivityFeed | null>(null);

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

  protected updateActivityType(type: ActivityType | string | null): void {
    this.store.setSelectedActivityType(type);
    this.refresh();
  }

  protected updateActor(actor: string | null): void {
    this.store.setSelectedActor(actor);
  }

  protected updateDateRange(range: DateRangeFilter | null): void {
    const hasRange = Boolean(range?.from || range?.to);
    this.store.setSelectedDateRange(hasRange ? range : null);
  }

  protected clearFilters(): void {
    this.store.setSelectedActivityType(null);
    this.store.setSelectedActor(null);
    this.store.setSelectedDateRange(null);
    this.refresh();
  }

  protected async openDetail(activity: ActivityFeed): Promise<void> {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId) {
      this.selectedActivity.set(activity);
      return;
    }

    const detail = await this.store.loadActivityDetail(workspaceId, activity.id);
    this.selectedActivity.set(detail ?? activity);
  }

  protected closeDetail(): void {
    this.selectedActivity.set(null);
  }

  protected clearError(): void {
    this.store.clearError();
  }

  protected metadataSummary(activity: ActivityFeed): string {
    return safeMetadataSummary(activity.metadataJson);
  }
}
