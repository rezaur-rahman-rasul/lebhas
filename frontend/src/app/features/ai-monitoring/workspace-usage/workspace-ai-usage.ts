import { CurrencyPipe, DecimalPipe } from '@angular/common';
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
import { AiMonitoringEmptyStateComponent } from '../components/ai-monitoring-empty-state/ai-monitoring-empty-state';
import { AiMonitoringLoadingStateComponent } from '../components/ai-monitoring-loading-state/ai-monitoring-loading-state';
import { AiMonitoringStore } from '../state/ai-monitoring.store';

@Component({
  selector: 'app-workspace-ai-usage-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DecimalPipe,
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    AiMonitoringEmptyStateComponent,
    AiMonitoringLoadingStateComponent,
  ],
  templateUrl: './workspace-ai-usage.html',
  styleUrl: './workspace-ai-usage.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceAiUsagePage implements OnInit {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(AiMonitoringStore);

  protected readonly isMaster = computed(() => this.workspace.currentRole() === 'MASTER');
  protected readonly visibleUsage = computed(() => this.store.workspaceAiUsage());
  protected readonly totalGenerationRequests = computed(() =>
    this.visibleUsage().reduce((total, usage) => total + usage.totalGenerationRequests, 0),
  );
  protected readonly totalGeneratedVersions = computed(() =>
    this.visibleUsage().reduce((total, usage) => total + usage.totalGeneratedVersions, 0),
  );
  protected readonly totalCreditsConsumed = computed(() =>
    this.visibleUsage().reduce((total, usage) => total + usage.totalCreditsConsumed, 0),
  );
  protected readonly totalEstimatedCost = computed(() =>
    this.visibleUsage().reduce((total, usage) => total + usage.totalEstimatedCostUsd, 0),
  );
  protected readonly totalFailures = computed(() =>
    this.visibleUsage().reduce((total, usage) => total + usage.totalFailures, 0),
  );
  protected readonly averageGenerationTime = computed(() =>
    average(this.visibleUsage().map((usage) => usage.avgGenerationTimeMs)),
  );
  protected readonly highCostWorkspaces = computed(() =>
    this.visibleUsage().filter((usage) => usage.highCostWarning),
  );
  protected readonly usageTrendRows = computed(() =>
    [...this.visibleUsage()].sort((a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt)).slice(0, 6),
  );

  ngOnInit(): void {
    if (this.permissions.canViewWorkspaceAiUsage()) {
      void this.loadUsage();
    }
  }

  protected refresh(): void {
    if (this.permissions.canViewWorkspaceAiUsage()) {
      void this.loadUsage();
    }
  }

  private async loadUsage(): Promise<void> {
    if (this.isMaster()) {
      await this.store.loadMasterWorkspaceUsage();
      return;
    }

    const workspaceId = this.workspace.activeWorkspaceId();
    if (workspaceId) {
      await this.store.loadWorkspaceAiUsage(workspaceId);
    }
  }
}

function average(values: readonly number[]): number | null {
  const validValues = values.filter((value) => Number.isFinite(value));
  return validValues.length
    ? validValues.reduce((total, value) => total + value, 0) / validValues.length
    : null;
}
