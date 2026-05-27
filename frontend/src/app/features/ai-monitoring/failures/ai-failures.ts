import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import {
  AiAnalyticsFilterBarComponent,
  AiAnalyticsFilterOption,
} from '../components/ai-analytics-filter-bar/ai-analytics-filter-bar';
import { AiMonitoringEmptyStateComponent } from '../components/ai-monitoring-empty-state/ai-monitoring-empty-state';
import { AiMonitoringLoadingStateComponent } from '../components/ai-monitoring-loading-state/ai-monitoring-loading-state';
import { FailureReasonCardComponent } from '../components/failure-reason-card/failure-reason-card';
import { AiFailureLog, AiFailureType } from '../models/ai-monitoring.models';
import { AiMonitoringStore } from '../state/ai-monitoring.store';

@Component({
  selector: 'app-ai-failures-page',
  standalone: true,
  imports: [
    DecimalPipe,
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    AiAnalyticsFilterBarComponent,
    AiMonitoringEmptyStateComponent,
    AiMonitoringLoadingStateComponent,
    FailureReasonCardComponent,
  ],
  templateUrl: './ai-failures.html',
  styleUrl: './ai-failures.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiFailuresPage implements OnInit {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(AiMonitoringStore);

  protected readonly isMaster = computed(() => this.workspace.currentRole() === 'MASTER');
  protected readonly providerOptions = computed<readonly AiAnalyticsFilterOption[]>(() =>
    uniqueOptions(
      this.store.aiFailures().map((failure) => ({
        id: failure.providerId,
        label: failure.providerName,
      })),
    ),
  );
  protected readonly layerOptions = computed<readonly AiAnalyticsFilterOption[]>(() =>
    uniqueOptions(
      this.store.aiFailures().map((failure) => ({
        id: failure.layerId,
        label: failure.layerName,
      })),
    ),
  );
  protected readonly filteredFailures = computed(() => {
    const selectedProvider = this.store.selectedProvider();
    const selectedLayer = this.store.selectedLayer();

    return this.store
      .aiFailures()
      .filter((failure) => !selectedProvider || failure.providerId === selectedProvider)
      .filter((failure) => !selectedLayer || failure.layerId === selectedLayer);
  });
  protected readonly urgentFailures = computed(() =>
    [...this.filteredFailures()].sort((a, b) => {
      const severity = failureSeverity(b) - failureSeverity(a);
      return severity || Date.parse(b.createdAt) - Date.parse(a.createdAt);
    }),
  );
  protected readonly fallbackTriggeredCount = computed(
    () => this.filteredFailures().filter((failure) => failure.fallbackTriggered).length,
  );
  protected readonly retryAttempts = computed(() =>
    this.filteredFailures().reduce((total, failure) => total + failure.retryAttempt, 0),
  );
  protected readonly providerGroups = computed(() =>
    groupFailures(this.filteredFailures(), (failure) => failure.providerName),
  );
  protected readonly layerGroups = computed(() =>
    groupFailures(this.filteredFailures(), (failure) => failure.layerName),
  );

  ngOnInit(): void {
    if (this.permissions.canViewAiFailures()) {
      void this.loadFailures();
    }
  }

  protected refresh(): void {
    if (this.permissions.canViewAiFailures()) {
      void this.loadFailures();
    }
  }

  protected friendlyFailureType(type: AiFailureType | string): string {
    switch (type) {
      case AiFailureType.Timeout:
        return 'This tool took too long to respond.';
      case AiFailureType.RateLimit:
        return 'This tool is temporarily busy.';
      case AiFailureType.ProviderDown:
        return 'This tool is currently unavailable.';
      case AiFailureType.InvalidResponse:
        return 'This tool returned an unusable result.';
      case AiFailureType.QualityFailure:
        return 'Output quality was not good enough.';
      case AiFailureType.CostLimitExceeded:
        return 'This request exceeded the allowed AI cost.';
      case AiFailureType.Unknown:
      default:
        return 'Something went wrong while generating this creative.';
    }
  }

  protected fallbackMessage(failure: AiFailureLog): string {
    return failure.fallbackTriggered ? 'Backup tool was used.' : 'No backup tool was used.';
  }

  private async loadFailures(): Promise<void> {
    if (this.isMaster()) {
      await this.store.loadMasterFailures();
      return;
    }

    const workspaceId = this.workspace.activeWorkspaceId();
    if (workspaceId) {
      await this.store.loadWorkspaceGenerationAnalytics(workspaceId);
    }
  }
}

interface FailureGroup {
  readonly name: string;
  readonly count: number;
}

function uniqueOptions(options: readonly AiAnalyticsFilterOption[]): readonly AiAnalyticsFilterOption[] {
  const seen = new Set<string>();
  return options.filter((option) => {
    if (seen.has(option.id)) {
      return false;
    }

    seen.add(option.id);
    return true;
  });
}

function groupFailures(
  failures: readonly AiFailureLog[],
  selector: (failure: AiFailureLog) => string,
): readonly FailureGroup[] {
  const groups = new Map<string, number>();

  for (const failure of failures) {
    const key = selector(failure);
    groups.set(key, (groups.get(key) ?? 0) + 1);
  }

  return [...groups.entries()]
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5);
}

function failureSeverity(failure: AiFailureLog): number {
  switch (failure.failureType) {
    case AiFailureType.ProviderDown:
    case AiFailureType.CostLimitExceeded:
      return 4;
    case AiFailureType.RateLimit:
    case AiFailureType.Timeout:
      return 3;
    case AiFailureType.InvalidResponse:
    case AiFailureType.QualityFailure:
      return 2;
    case AiFailureType.Unknown:
    default:
      return 1;
  }
}
