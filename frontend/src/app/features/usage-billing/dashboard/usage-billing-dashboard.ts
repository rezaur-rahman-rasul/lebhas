import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { CreditBalanceCardComponent } from '../components/credit-balance-card/credit-balance-card';
import { CreditLedgerCardComponent } from '../components/credit-ledger-card/credit-ledger-card';
import { CreditLifecycleTimelineComponent } from '../components/credit-lifecycle-timeline/credit-lifecycle-timeline';
import { PlanUtilizationCardComponent } from '../components/plan-utilization-card/plan-utilization-card';
import { UsageEmptyStateComponent } from '../components/usage-empty-state/usage-empty-state';
import { UsageFilterBarComponent } from '../components/usage-filter-bar/usage-filter-bar';
import { UsageLoadingStateComponent } from '../components/usage-loading-state/usage-loading-state';
import { UsageLogCardComponent } from '../components/usage-log-card/usage-log-card';
import { UsageProgressBarComponent } from '../components/usage-progress-bar/usage-progress-bar';
import { UsageSummaryCardComponent } from '../components/usage-summary-card/usage-summary-card';
import {
  CreditTransactionType,
  UsageLimitSnapshot,
  UsageType,
  WorkspaceUsageSummary,
} from '../models/usage-billing.models';
import { UsageBillingStore } from '../state/usage-billing.store';

interface UsageSummaryTile {
  readonly label: string;
  readonly value: number;
  readonly format: 'number' | 'currency' | 'bytes';
  readonly icon: string;
  readonly helperText: string;
  readonly tone: 'neutral' | 'brand' | 'warning' | 'danger';
}

@Component({
  selector: 'app-usage-billing-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    CreditBalanceCardComponent,
    CreditLedgerCardComponent,
    CreditLifecycleTimelineComponent,
    PlanUtilizationCardComponent,
    UsageEmptyStateComponent,
    UsageFilterBarComponent,
    UsageLoadingStateComponent,
    UsageLogCardComponent,
    UsageProgressBarComponent,
    UsageSummaryCardComponent,
  ],
  templateUrl: './usage-billing-dashboard.html',
  styleUrl: './usage-billing-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsageBillingDashboardPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(UsageBillingStore);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly activePackageName = this.workspace.activePlanLabel;
  protected readonly subscriptionStatus = this.workspace.subscriptionStatusLabel;

  protected readonly currentUsage = computed(
    () => this.store.currentMonthUsage() ?? this.store.usageSummary(),
  );
  protected readonly hasUsageData = computed(() => {
    const usage = this.currentUsage();
    return (
      !!usage &&
      (usage.usedCredits > 0 ||
        usage.reservedCredits > 0 ||
        usage.refundedCredits > 0 ||
        usage.totalCreativeRequests > 0 ||
        usage.totalGeneratedVersions > 0 ||
        usage.totalLayerExecutions > 0 ||
        usage.totalUploads > 0 ||
        usage.totalDownloads > 0 ||
        usage.totalPublicShares > 0 ||
        usage.totalPromptEnhancements > 0 ||
        usage.totalApiCalls > 0)
    );
  });

  protected readonly summaryTiles = computed<readonly UsageSummaryTile[]>(() => {
    const usage = this.currentUsage();
    if (!usage) {
      return [];
    }

    return [
      {
        label: 'Creative requests',
        value: usage.totalCreativeRequests,
        format: 'number',
        icon: 'file-plus-2',
        helperText: 'Requests created in this workspace.',
        tone: 'brand',
      },
      {
        label: 'Generated versions',
        value: usage.totalGeneratedVersions,
        format: 'number',
        icon: 'images',
        helperText: 'Creative outputs generated this month.',
        tone: 'brand',
      },
      {
        label: 'AI steps used',
        value: usage.totalLayerExecutions,
        format: 'number',
        icon: 'workflow',
        helperText: 'AI work steps reported by backend usage.',
        tone: 'neutral',
      },
      {
        label: 'Estimated AI cost',
        value: usage.totalAiCostUsd,
        format: 'currency',
        icon: 'wallet-cards',
        helperText: 'Estimated AI cost from backend usage records.',
        tone: usage.totalAiCostUsd > 0 ? 'warning' : 'neutral',
      },
      {
        label: 'Uploads',
        value: usage.totalUploads,
        format: 'number',
        icon: 'upload-cloud',
        helperText: 'Files uploaded for campaigns and creatives.',
        tone: 'neutral',
      },
      {
        label: 'Storage usage',
        value: usage.totalStorageBytes,
        format: 'bytes',
        icon: 'hard-drive',
        helperText: 'Storage used by uploaded and generated files.',
        tone: this.store.isStorageLimitNear() ? 'warning' : 'neutral',
      },
      {
        label: 'Downloads',
        value: usage.totalDownloads,
        format: 'number',
        icon: 'download',
        helperText: 'Creative and asset downloads.',
        tone: 'neutral',
      },
      {
        label: 'Shared links used',
        value: usage.totalPublicShares,
        format: 'number',
        icon: 'share-2',
        helperText: 'Public share links used for creative review.',
        tone: 'neutral',
      },
      {
        label: 'Prompt enhancements',
        value: usage.totalPromptEnhancements,
        format: 'number',
        icon: 'sparkles',
        helperText: 'Prompts improved by the AI assistant.',
        tone: 'neutral',
      },
      {
        label: 'Generation failures',
        value: usage.totalGenerationFailures,
        format: 'number',
        icon: 'triangle-alert',
        helperText: 'Failed generations that may have refunded credits.',
        tone: usage.totalGenerationFailures > 0 ? 'danger' : 'neutral',
      },
      {
        label: 'API calls',
        value: usage.totalApiCalls,
        format: 'number',
        icon: 'activity',
        helperText: 'Workspace usage activity reported by backend.',
        tone: 'neutral',
      },
    ];
  });

  protected readonly creditLimit = computed(() => this.currentUsage()?.creditLimit ?? null);
  protected readonly storageLimit = computed(() => this.currentUsage()?.storageLimit ?? null);
  protected readonly generatedVersionLimit = computed(
    () => this.currentUsage()?.generatedVersionLimit ?? null,
  );
  protected readonly creditLimitExceeded = computed(() => this.isExceeded(this.creditLimit()));
  protected readonly generatedVersionLimitExceeded = computed(() =>
    this.isExceeded(this.generatedVersionLimit()),
  );
  protected readonly planUtilizationNote = computed(() => {
    const activePlan = this.activePackageName();
    const plan = this.store
      .planUtilization()
      .find((item) => item.pricingPlanName === activePlan);

    return plan
      ? `${plan.workspaceCount} workspace(s) are using this package.`
      : 'Package usage comes from your workspace context and backend usage limits.';
  });

  protected readonly publicShareAvailable = computed(
    () => this.workspace.featurePolicy()?.shareAvailable ?? null,
  );
  protected readonly approvalAvailable = computed(
    () => this.workspace.featurePolicy()?.approvalAvailable ?? null,
  );
  protected readonly videoGenerationAvailable = computed(() => {
    const features = this.workspace.featurePolicy()?.features ?? {};
    const videoFeature = Object.entries(features).find(([key]) =>
      key.toLowerCase().includes('video'),
    );

    return videoFeature?.[1].enabled ?? null;
  });

  protected readonly accessDenied = computed(() => !this.permissions.canViewUsageBilling());
  protected readonly canShowCreditHistory = this.permissions.canViewCreditLedger;
  protected readonly canShowPackageLimits = computed(
    () => this.permissions.canViewUsageBilling() || this.permissions.canViewPlanUtilization(),
  );

  constructor() {
    effect(() => {
      const workspaceId = this.activeWorkspaceId();
      if (!workspaceId || !this.permissions.canViewUsageBilling()) {
        return;
      }

      void this.loadWorkspaceData(workspaceId);
    });
  }

  protected refresh(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || !this.permissions.canViewUsageBilling()) {
      return;
    }

    void this.loadWorkspaceData(workspaceId);
  }

  protected updateMonth(month: string | null): void {
    this.store.setSelectedMonth(month);
    this.refreshActivity();
  }

  protected updateUsageType(type: UsageType | string | null): void {
    this.store.setSelectedUsageType(type);
    this.refreshActivity();
  }

  protected updateTransactionType(type: CreditTransactionType | string | null): void {
    this.store.setSelectedTransactionType(type);
    this.refreshActivity();
  }

  protected clearFilters(): void {
    this.store.setSelectedMonth(null);
    this.store.setSelectedUsageType(null);
    this.store.setSelectedTransactionType(null);
    this.refreshActivity();
  }

  protected isExceeded(limit: UsageLimitSnapshot | null): boolean {
    return !!limit?.limit && limit.used >= limit.limit;
  }

  private async loadWorkspaceData(workspaceId: string): Promise<void> {
    await this.store.loadDashboard(workspaceId);
    await this.refreshActivity();
  }

  private async refreshActivity(): Promise<void> {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId) {
      return;
    }

    const loaders: Promise<unknown>[] = [this.store.loadUsageBillingLogs(workspaceId)];
    if (this.permissions.canViewCreditLedger()) {
      loaders.push(this.store.loadCreditLedger(workspaceId));
    }

    await Promise.all(loaders);
  }
}
