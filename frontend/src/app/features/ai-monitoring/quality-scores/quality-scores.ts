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
import { AiMonitoringEmptyStateComponent } from '../components/ai-monitoring-empty-state/ai-monitoring-empty-state';
import { AiMonitoringLoadingStateComponent } from '../components/ai-monitoring-loading-state/ai-monitoring-loading-state';
import { QualityScoreCardComponent } from '../components/quality-score-card/quality-score-card';
import { AiQualityScore } from '../models/ai-monitoring.models';
import { AiMonitoringStore } from '../state/ai-monitoring.store';

@Component({
  selector: 'app-quality-scores-page',
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
    AiMonitoringEmptyStateComponent,
    AiMonitoringLoadingStateComponent,
    QualityScoreCardComponent,
  ],
  templateUrl: './quality-scores.html',
  styleUrl: './quality-scores.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QualityScoresPage implements OnInit {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(AiMonitoringStore);

  protected readonly isMaster = computed(() => this.workspace.currentRole() === 'MASTER');
  protected readonly visibleScores = computed(() => this.store.qualityScores());
  protected readonly averageOverallScore = computed(() =>
    average(this.visibleScores().map((score) => score.overallScore)),
  );
  protected readonly averageBanglaTypographyScore = computed(() =>
    average(this.visibleScores().map((score) => score.banglaTypographyScore)),
  );
  protected readonly latestScores = computed(() =>
    [...this.visibleScores()].sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt)),
  );

  ngOnInit(): void {
    if (this.permissions.canViewQualityScores()) {
      void this.loadScores();
    }
  }

  protected refresh(): void {
    if (this.permissions.canViewQualityScores()) {
      void this.loadScores();
    }
  }

  protected scoreLabel(score: AiQualityScore): string | null {
    return score.qualityLabel ?? null;
  }

  private async loadScores(): Promise<void> {
    if (this.isMaster()) {
      await this.store.loadMasterQualityScores();
      return;
    }

    const workspaceId = this.workspace.activeWorkspaceId();
    if (workspaceId) {
      await this.store.loadWorkspaceQualityScores(workspaceId);
    }
  }
}

function average(values: readonly number[]): number | null {
  const validValues = values.filter((value) => Number.isFinite(value));
  return validValues.length
    ? validValues.reduce((total, value) => total + value, 0) / validValues.length
    : null;
}
