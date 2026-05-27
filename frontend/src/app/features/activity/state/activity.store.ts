import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { ActivityActionResult, ActivityFeed, ActivityType, DateRangeFilter } from '../models/activity.models';
import { ActivityApiService } from '../services/activity-api.service';

@Injectable({ providedIn: 'root' })
export class ActivityStore {
  private readonly api = inject(ActivityApiService);
  private readonly permissions = inject(PermissionStore);

  private readonly activitiesSignal = signal<readonly ActivityFeed[]>([]);
  private readonly selectedActivityTypeSignal = signal<ActivityType | string | null>(null);
  private readonly selectedActorSignal = signal<string | null>(null);
  private readonly selectedDateRangeSignal = signal<DateRangeFilter | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly activities = this.activitiesSignal.asReadonly();
  readonly selectedActivityType = this.selectedActivityTypeSignal.asReadonly();
  readonly selectedActor = this.selectedActorSignal.asReadonly();
  readonly selectedDateRange = this.selectedDateRangeSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly recentActivities = computed(() => this.activitiesSignal().slice(0, 10));

  async loadActivityFeed(workspaceId: string): Promise<ActivityActionResult> {
    if (!this.permissions.canViewActivityFeed()) {
      return this.restricted();
    }

    return this.run(async () => {
      const dateRange = this.selectedDateRangeSignal();
      this.activitiesSignal.set(
        await this.api.getActivityFeed(workspaceId, {
          activityType: this.selectedActivityTypeSignal(),
          actorUserId: this.selectedActorSignal(),
          from: dateRange?.from,
          to: dateRange?.to,
        }),
      );
    });
  }

  async loadActivityDetail(workspaceId: string, activityId: string): Promise<ActivityFeed | null> {
    if (!this.permissions.canViewActivityFeed()) {
      this.restricted();
      return null;
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      return await this.api.getActivityDetail(workspaceId, activityId);
    } catch (error) {
      this.errorSignal.set(friendlyError(error));
      return null;
    } finally {
      this.loadingSignal.set(false);
    }
  }

  setSelectedActivityType(type: ActivityType | string | null): void {
    this.selectedActivityTypeSignal.set(type);
  }

  setSelectedActor(actor: string | null): void {
    this.selectedActorSignal.set(actor);
  }

  setSelectedDateRange(range: DateRangeFilter | null): void {
    this.selectedDateRangeSignal.set(range);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private async run(action: () => Promise<void>): Promise<ActivityActionResult> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      await action();
      return { ok: true };
    } catch (error) {
      const message = friendlyError(error);
      this.errorSignal.set(message);
      return { ok: false, message };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private restricted(): ActivityActionResult {
    const message = 'You do not have access to the activity feed.';
    this.errorSignal.set(message);
    return { ok: false, message };
  }
}

function friendlyError(error: unknown): string {
  return normalizeHttpError(error).message || 'We could not load activity. Please try again.';
}
