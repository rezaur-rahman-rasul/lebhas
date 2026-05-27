import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { UsageEmptyStateComponent } from '../components/usage-empty-state/usage-empty-state';
import { UsageLoadingStateComponent } from '../components/usage-loading-state/usage-loading-state';
import { UsageProgressBarComponent } from '../components/usage-progress-bar/usage-progress-bar';
import { UsageSummaryCardComponent } from '../components/usage-summary-card/usage-summary-card';
import { MasterWorkspaceUsage, PlanUtilization } from '../models/usage-billing.models';
import { UsageBillingStore } from '../state/usage-billing.store';

type MasterUsageSection = 'overview' | 'workspaces' | 'ai-costs' | 'plan-utilization';

interface SummaryTile {
  readonly label: string;
  readonly value: number;
  readonly format: 'number' | 'currency' | 'bytes';
  readonly icon: string;
  readonly helperText: string;
  readonly tone: 'neutral' | 'brand' | 'warning' | 'danger';
}

@Component({
  selector: 'app-master-usage-overview',
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
    UsageEmptyStateComponent,
    UsageLoadingStateComponent,
    UsageProgressBarComponent,
    UsageSummaryCardComponent,
  ],
  templateUrl: './master-usage-overview.html',
  styleUrl: './master-usage-overview.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MasterUsageOverviewPage {
  private readonly route = inject(ActivatedRoute);

  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(UsageBillingStore);

  protected readonly section = computed<MasterUsageSection>(
    () => (this.route.snapshot.data['section'] as MasterUsageSection | undefined) ?? 'overview',
  );
  protected readonly accessDenied = computed(() => !this.permissions.canViewMasterUsage());
  protected readonly planAccessDenied = computed(
    () => this.section() === 'plan-utilization' && !this.permissions.canViewPlanUtilization(),
  );
  protected readonly hasData = computed(
    () =>
      this.store.masterWorkspaceUsage().length > 0 ||
      this.store.masterAiCosts().length > 0 ||
      this.store.topCostWorkspaces().length > 0 ||
      this.store.planUtilization().length > 0,
  );

  protected readonly sectionTitle = computed(() => {
    switch (this.section()) {
      case 'workspaces':
        return 'Workspace usage';
      case 'ai-costs':
        return 'AI cost usage';
      case 'plan-utilization':
        return 'Plan utilization';
      default:
        return 'Master usage overview';
    }
  });

  protected readonly sectionDescription = computed(() => {
    switch (this.section()) {
      case 'workspaces':
        return 'Compare workspace credit, creative, storage, sharing, and cost usage from backend records.';
      case 'ai-costs':
        return 'Review AI cost usage by month without exposing provider secrets or credentials.';
      case 'plan-utilization':
        return 'Understand how backend-defined packages are being used across workspaces.';
      default:
        return 'Monitor workspace usage, credit consumption, AI cost, package utilization, and high-cost warnings.';
    }
  });

  protected readonly totalWorkspaces = computed(() => this.store.masterWorkspaceUsage().length);
  protected readonly totalUsedCredits = computed(() =>
    this.store.masterWorkspaceUsage().reduce(
      (total, workspace) =>
        total +
        (workspace.currentMonthUsage?.usedCredits ?? workspace.latestSnapshot?.usedCredits ?? 0),
      0,
    ),
  );
  protected readonly totalAiCost = computed(() => {
    const workspaceTotal = this.store.masterWorkspaceUsage().reduce(
      (total, workspace) =>
        total +
        (workspace.currentMonthUsage?.totalAiCostUsd ?? workspace.latestSnapshot?.aiCostUsd ?? 0),
      0,
    );

    return workspaceTotal > 0
      ? workspaceTotal
      : this.store.masterAiCosts().reduce((total, item) => total + item.totalAiCostUsd, 0);
  });
  protected readonly totalPublicShares = computed(() =>
    this.store.masterWorkspaceUsage().reduce(
      (total, workspace) =>
        total +
        (workspace.currentMonthUsage?.totalPublicShares ?? workspace.latestSnapshot?.publicShares ?? 0),
      0,
    ),
  );
  protected readonly totalStorageBytes = computed(() =>
    this.store.masterWorkspaceUsage().reduce(
      (total, workspace) =>
        total +
        (workspace.currentMonthUsage?.totalStorageBytes ?? workspace.latestSnapshot?.storageBytes ?? 0),
      0,
    ),
  );

  protected readonly summaryTiles = computed<readonly SummaryTile[]>(() => [
    {
      label: 'Total workspaces',
      value: this.totalWorkspaces(),
      format: 'number',
      icon: 'building-2',
      helperText: 'Workspaces returned by backend usage APIs.',
      tone: 'brand',
    },
    {
      label: 'Total used credits',
      value: this.totalUsedCredits(),
      format: 'number',
      icon: 'wallet-cards',
      helperText: 'Credits used across workspace usage records.',
      tone: 'brand',
    },
    {
      label: 'Total AI cost',
      value: this.totalAiCost(),
      format: 'currency',
      icon: 'chart-no-axes-combined',
      helperText: 'Estimated AI cost reported by backend.',
      tone: this.totalAiCost() > 0 ? 'warning' : 'neutral',
    },
    {
      label: 'Public share usage',
      value: this.totalPublicShares(),
      format: 'number',
      icon: 'share-2',
      helperText: 'Public share usage across workspaces.',
      tone: 'neutral',
    },
    {
      label: 'Storage usage',
      value: this.totalStorageBytes(),
      format: 'bytes',
      icon: 'hard-drive',
      helperText: 'Storage used across workspaces.',
      tone: 'neutral',
    },
  ]);

  protected readonly highestUsageWorkspaces = computed(() =>
    [...this.store.masterWorkspaceUsage()]
      .sort((a, b) => workspaceCredits(b) - workspaceCredits(a))
      .slice(0, 8),
  );

  protected readonly topCostWorkspaces = computed(() => this.store.topCostWorkspaces().slice(0, 8));
  protected readonly aiCostRows = computed(() =>
    [...this.store.masterAiCosts()].sort(
      (a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt),
    ),
  );
  protected readonly planRows = computed(() =>
    [...this.store.planUtilization()].sort((a, b) => b.totalUsedCredits - a.totalUsedCredits),
  );
  protected readonly usageAnomalyHint = computed(() =>
    this.store.topCostWorkspaces().some((workspace) => workspace.highCostWarning)
      ? 'AI cost increased unusually for one or more workspaces.'
      : null,
  );

  protected readonly navItems = [
    { label: 'Overview', route: '/master/usage', section: 'overview' },
    { label: 'Workspaces', route: '/master/usage/workspaces', section: 'workspaces' },
    { label: 'AI costs', route: '/master/usage/ai-costs', section: 'ai-costs' },
    { label: 'Plan utilization', route: '/master/usage/plan-utilization', section: 'plan-utilization' },
  ] as const;

  constructor() {
    effect(() => {
      if (this.permissions.canViewMasterUsage()) {
        void this.store.loadMasterUsageOverview();
      }
    });
  }

  protected refresh(): void {
    if (this.permissions.canViewMasterUsage()) {
      void this.store.loadMasterUsageOverview();
    }
  }

  protected workspaceCreditTotal(workspace: MasterWorkspaceUsage): number {
    return workspaceCredits(workspace);
  }

  protected workspaceAiCost(workspace: MasterWorkspaceUsage): number {
    return workspace.currentMonthUsage?.totalAiCostUsd ?? workspace.latestSnapshot?.aiCostUsd ?? 0;
  }

  protected workspaceStorage(workspace: MasterWorkspaceUsage): number {
    return workspace.currentMonthUsage?.totalStorageBytes ?? workspace.latestSnapshot?.storageBytes ?? 0;
  }

  protected workspaceShares(workspace: MasterWorkspaceUsage): number {
    return workspace.currentMonthUsage?.totalPublicShares ?? workspace.latestSnapshot?.publicShares ?? 0;
  }

  protected formatBytes(value: number): string {
    if (value <= 0) {
      return '0 B';
    }

    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const unitIndex = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1);
    const size = value / 1024 ** unitIndex;
    return `${size.toFixed(size >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
  }

  protected percentValue(value: number | null): number | null {
    return typeof value === 'number' ? value : null;
  }

  protected planCreditLimit(plan: PlanUtilization): number | null {
    return typeof plan.averageCreditUtilizationPercent === 'number' && plan.averageCreditUtilizationPercent > 0
      ? plan.totalUsedCredits / (plan.averageCreditUtilizationPercent / 100)
      : null;
  }
}

function workspaceCredits(workspace: MasterWorkspaceUsage): number {
  return workspace.currentMonthUsage?.usedCredits ?? workspace.latestSnapshot?.usedCredits ?? 0;
}
