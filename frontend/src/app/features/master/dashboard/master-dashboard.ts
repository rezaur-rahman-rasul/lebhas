import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { SectionHeaderComponent } from '@app/shared/components/section-header/section-header';
import { MonitoringAlert, SystemHealthStatus } from '../../monitoring/models/monitoring.models';
import { MonitoringStore } from '../../monitoring/state/monitoring.store';
import { UsageBillingStore } from '../../usage-billing/state/usage-billing.store';

interface MasterStat {
  readonly label: string;
  readonly value: string;
  readonly trend: string;
  readonly icon: string;
  readonly tone: 'brand' | 'blue' | 'emerald' | 'red' | 'neutral';
}

interface MasterAction {
  readonly label: string;
  readonly route: string;
  readonly icon: string;
}

interface ReadinessItem {
  readonly label: string;
  readonly status: string;
  readonly tone: 'brand' | 'blue' | 'red' | 'neutral';
  readonly helper: string;
}

@Component({
  selector: 'app-master-dashboard',
  standalone: true,
  imports: [
    RouterLink,
    BadgeComponent,
    CardComponent,
    EmptyStateComponent,
    IconComponent,
    SectionHeaderComponent,
  ],
  templateUrl: './master-dashboard.html',
  styleUrl: './master-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MasterDashboardPage {
  private readonly auth = inject(CurrentUserStore);
  private readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly usage = inject(UsageBillingStore);
  protected readonly monitoring = inject(MonitoringStore);

  protected readonly role = this.auth.currentRole;
  protected readonly displayName = this.auth.displayName;
  protected readonly skeletonRows = [0, 1, 2, 3, 4, 5] as const;

  protected readonly canViewMasterUsage = this.permissions.canViewMasterUsage;
  protected readonly canViewMasterMonitoring = this.permissions.canViewMasterMonitoring;

  protected readonly workspaceUsage = this.usage.masterWorkspaceUsage;
  protected readonly aiCosts = this.usage.masterAiCosts;
  protected readonly planUtilization = this.usage.planUtilization;
  protected readonly alerts = this.monitoring.alerts;
  protected readonly systemHealth = this.monitoring.systemHealth;
  protected readonly providerSummary = this.monitoring.aiProviderSummary;
  protected readonly paymentSummary = this.monitoring.paymentSummary;
  protected readonly workspaceSummary = this.monitoring.workspaceSummary;

  protected readonly loading = computed(() => this.usage.loading() || this.monitoring.loading());
  protected readonly error = computed(() => this.monitoring.error() || this.usage.error());
  protected readonly hasAnyData = computed(
    () =>
      this.workspaceUsage().length > 0 ||
      this.aiCosts().length > 0 ||
      this.planUtilization().length > 0 ||
      this.alerts().length > 0 ||
      this.systemHealth().length > 0 ||
      Boolean(this.providerSummary() || this.paymentSummary() || this.workspaceSummary()),
  );

  protected readonly stats = computed<readonly MasterStat[]>(() => {
    const totalWorkspaces =
      this.workspaceSummary()?.totalWorkspaces ?? this.workspace.workspaces().length ?? this.workspaceUsage().length;
    const activePlans = this.planUtilization().length;
    const aiGenerations = this.aiCosts().reduce(
      (total, item) => total + (item.totalGeneratedVersions ?? 0),
      0,
    );
    const usedCredits = this.planUtilization().reduce(
      (total, item) => total + (item.totalUsedCredits ?? 0),
      0,
    );
    const alertCount = this.monitoring.unresolvedAlerts().length;

    return [
      {
        label: 'Accessible Workspaces',
        value: this.formatNumber(totalWorkspaces),
        trend: `${this.workspace.workspaces().length} selectable support contexts`,
        icon: 'building-2',
        tone: 'brand',
      },
      {
        label: 'Admin Users',
        value: '--',
        trend: 'User directory endpoint not available yet',
        icon: 'users',
        tone: 'neutral',
      },
      {
        label: 'Active Plans',
        value: this.formatNumber(activePlans),
        trend: activePlans > 0 ? 'Plans reporting usage' : 'No plan utilization yet',
        icon: 'package-check',
        tone: 'blue',
      },
      {
        label: 'AI Generations',
        value: this.formatNumber(aiGenerations),
        trend: this.aiCosts()[0]?.usageMonth ?? 'Across reported months',
        icon: 'sparkles',
        tone: 'emerald',
      },
      {
        label: 'Credit Revenue/Usage',
        value: this.formatNumber(usedCredits),
        trend: 'Credits consumed by active plans',
        icon: 'wallet-cards',
        tone: 'brand',
      },
      {
        label: 'System Alerts',
        value: this.formatNumber(alertCount),
        trend: alertCount > 0 ? 'Needs review' : 'No unresolved alerts',
        icon: 'triangle-alert',
        tone: alertCount > 0 ? 'red' : 'emerald',
      },
    ];
  });

  protected readonly quickActions: readonly MasterAction[] = [
    { label: 'Manage Workspaces', route: '/master/usage/workspaces', icon: 'building-2' },
    { label: 'Manage Pricing', route: '/payments/subscription', icon: 'package-check' },
    { label: 'Manage AI Tools', route: '/ai-monitoring', icon: 'wand-sparkles' },
    { label: 'View System Health', route: '/master/monitoring/system-health', icon: 'gauge' },
    { label: 'View Audit Logs', route: '/audit-logs', icon: 'shield-check' },
  ];

  protected readonly topWorkspaces = computed(() =>
    [...this.workspaceUsage()]
      .sort((left, right) => this.workspaceCredits(right) - this.workspaceCredits(left))
      .slice(0, 5),
  );

  protected readonly recentActivity = computed(() => {
    const alertActivity = this.alerts().slice(0, 4).map((alert) => ({
      title: alert.title,
      description: alert.workspaceName ?? alert.relatedProviderName ?? this.alertLabel(alert),
      time: alert.createdAt,
      icon: alert.resolved ? 'check-circle-2' : 'triangle-alert',
      tone: alert.resolved ? 'brand' : this.alertTone(alert),
    }));
    const healthActivity = this.systemHealth().slice(0, 3).map((event) => ({
      title: `${event.serviceName} ${this.healthLabel(event.status)}`,
      description: event.eventType,
      time: event.createdAt,
      icon: event.status === SystemHealthStatus.Healthy ? 'check-circle-2' : 'activity',
      tone: this.healthTone(event.status),
    }));

    return [...alertActivity, ...healthActivity]
      .sort((left, right) => Date.parse(right.time) - Date.parse(left.time))
      .slice(0, 6);
  });

  protected readonly pendingIssues = computed(() =>
    [
      ...this.monitoring.criticalAlerts().map((alert) => ({
        title: alert.title,
        description: alert.description,
        badge: this.alertLabel(alert),
        tone: 'red' as const,
      })),
      ...this.monitoring.downServices().map((event) => ({
        title: event.serviceName,
        description: event.eventType,
        badge: 'Down',
        tone: 'red' as const,
      })),
      ...this.monitoring.degradedServices().map((event) => ({
        title: event.serviceName,
        description: event.eventType,
        badge: 'Degraded',
        tone: 'blue' as const,
      })),
    ].slice(0, 5),
  );

  protected readonly readinessItems = computed<readonly ReadinessItem[]>(() => {
    const hasHealth = this.systemHealth().length > 0;
    const downCount = this.monitoring.downServices().length;
    const criticalCount = this.monitoring.criticalAlerts().length;
    const providerSummary = this.providerSummary();
    const paymentSummary = this.paymentSummary();

    return [
      {
        label: 'System health',
        status: downCount > 0 ? 'Needs attention' : hasHealth ? 'Ready' : 'Waiting',
        tone: downCount > 0 ? 'red' : hasHealth ? 'brand' : 'neutral',
        helper: hasHealth ? `${this.monitoring.healthyServices().length} healthy services` : 'No health events yet',
      },
      {
        label: 'AI providers',
        status: (providerSummary?.failedProviders ?? 0) > 0 ? 'Needs attention' : providerSummary ? 'Ready' : 'Waiting',
        tone: (providerSummary?.failedProviders ?? 0) > 0 ? 'red' : providerSummary ? 'brand' : 'neutral',
        helper: `${providerSummary?.healthyProviders ?? 0} healthy / ${providerSummary?.totalProviders ?? 0} total`,
      },
      {
        label: 'Payments',
        status: (paymentSummary?.failedPayments ?? 0) > 0 ? 'Review' : paymentSummary ? 'Ready' : 'Waiting',
        tone: (paymentSummary?.failedPayments ?? 0) > 0 ? 'red' : paymentSummary ? 'brand' : 'neutral',
        helper: `${paymentSummary?.pendingPayments ?? 0} pending transactions`,
      },
      {
        label: 'Open issues',
        status: criticalCount > 0 ? 'Blocked' : this.alerts().length > 0 ? 'Review' : 'Clear',
        tone: criticalCount > 0 ? 'red' : this.alerts().length > 0 ? 'blue' : 'brand',
        helper: `${this.monitoring.unresolvedAlerts().length} unresolved alerts`,
      },
    ];
  });

  constructor() {
    effect(() => {
      if (this.role() !== 'MASTER') {
        return;
      }

      if (this.canViewMasterUsage()) {
        void this.usage.loadMasterUsageOverview();
      }

      if (this.canViewMasterMonitoring()) {
        void this.monitoring.loadMonitoringDashboard();
      }
    });
  }

  protected refresh(): void {
    if (this.canViewMasterUsage()) {
      void this.usage.loadMasterUsageOverview();
    }

    if (this.canViewMasterMonitoring()) {
      void this.monitoring.loadMonitoringDashboard();
    }
  }

  protected workspaceCredits(workspace: { readonly currentMonthUsage: { readonly usedCredits?: number } | null }): number {
    return workspace.currentMonthUsage?.usedCredits ?? 0;
  }

  protected workspaceGenerations(workspace: {
    readonly currentMonthUsage: { readonly totalGeneratedVersions?: number } | null;
    readonly latestSnapshot: { readonly generatedVersions?: number } | null;
  }): number {
    return workspace.currentMonthUsage?.totalGeneratedVersions ?? workspace.latestSnapshot?.generatedVersions ?? 0;
  }

  protected healthLabel(status: string): string {
    return status.toLowerCase().replaceAll('_', ' ');
  }

  protected healthTone(status: string): 'brand' | 'blue' | 'red' | 'neutral' {
    switch (status) {
      case SystemHealthStatus.Healthy:
        return 'brand';
      case SystemHealthStatus.Degraded:
        return 'blue';
      case SystemHealthStatus.Down:
        return 'red';
      default:
        return 'neutral';
    }
  }

  protected alertTone(alert: MonitoringAlert): 'brand' | 'blue' | 'red' | 'neutral' {
    switch (alert.severity) {
      case 'CRITICAL':
      case 'ERROR':
        return 'red';
      case 'WARNING':
        return 'blue';
      case 'INFO':
        return 'brand';
      default:
        return 'neutral';
    }
  }

  protected alertLabel(alert: MonitoringAlert): string {
    return String(alert.severity).toLowerCase().replaceAll('_', ' ');
  }

  protected formatNumber(value: number | null | undefined): string {
    return typeof value === 'number' ? new Intl.NumberFormat('en-US').format(value) : '--';
  }

  protected formatCurrency(value: number | null | undefined): string {
    return typeof value === 'number'
      ? new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 2 }).format(value)
      : '--';
  }
}
