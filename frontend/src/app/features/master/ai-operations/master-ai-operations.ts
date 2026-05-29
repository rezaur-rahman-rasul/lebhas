import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, RouterLinkActive, ActivatedRoute } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { IconComponent } from '@app/shared/components/icon/icon';
import { EnvironmentType, PaymentProvider } from '../../payments/models/payment.models';
import { PaymentStore } from '../../payments/state/payment.store';
import {
  AiFailureLog,
  AiLayerAnalytics,
  AiProviderHealth,
  AiProviderMetric,
  ProviderHealthStatus,
} from '../../ai-monitoring/models/ai-monitoring.models';
import { AiMonitoringStore } from '../../ai-monitoring/state/ai-monitoring.store';

type MasterAiOpsMode = 'provider-settings' | 'provider-health' | 'layer-analytics' | 'ai-cost-usage' | 'ai-failures';
type BadgeTone = 'neutral' | 'brand' | 'blue' | 'red';

interface StatTile {
  readonly label: string;
  readonly value: string | number;
  readonly helper: string;
  readonly icon: string;
  readonly tone: BadgeTone;
}

interface CostRow {
  readonly id: string;
  readonly name: string;
  readonly value: number;
  readonly helper: string;
  readonly warning?: boolean;
}

@Component({
  selector: 'app-master-ai-operations',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    RouterLink,
    RouterLinkActive,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    IconComponent,
  ],
  templateUrl: './master-ai-operations.html',
  styleUrl: './master-ai-operations.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MasterAiOperationsPage {
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  protected readonly permissions = inject(PermissionStore);
  protected readonly aiStore = inject(AiMonitoringStore);
  protected readonly paymentStore = inject(PaymentStore);

  protected readonly selectedProviderId = signal<string | null>(null);

  protected readonly mode = computed<MasterAiOpsMode>(
    () => (this.route.snapshot.data['mode'] as MasterAiOpsMode | undefined) ?? 'provider-health',
  );
  protected readonly title = computed(() => {
    switch (this.mode()) {
      case 'provider-settings':
        return 'Provider Settings';
      case 'layer-analytics':
        return 'Layer Analytics';
      case 'ai-cost-usage':
        return 'AI Cost Usage';
      case 'ai-failures':
        return 'AI Failures';
      default:
        return 'Provider Health';
    }
  });
  protected readonly description = computed(() => {
    switch (this.mode()) {
      case 'provider-settings':
        return 'Manage provider status and credential updates without exposing saved secrets.';
      case 'layer-analytics':
        return 'Track layer usage, cost, duration, failure count, quality score, and trend.';
      case 'ai-cost-usage':
        return 'Review AI cost by provider, workspace, tool, quality signal, and warning status.';
      case 'ai-failures':
        return 'Inspect failed AI jobs, provider issues, retry attempts, and fallback usage.';
      default:
        return 'Monitor provider health, latency, success rate, fallback usage, and cost trend.';
    }
  });
  protected readonly accessDenied = computed(() => {
    switch (this.mode()) {
      case 'provider-settings':
        return !this.permissions.canManagePaymentProviders();
      case 'layer-analytics':
        return !this.permissions.canViewLayerAnalytics();
      case 'ai-cost-usage':
        return !this.permissions.canViewAiMonitoring();
      case 'ai-failures':
        return !this.permissions.canViewAiFailures();
      default:
        return !this.permissions.canViewProviderMetrics();
    }
  });

  protected readonly providers = this.paymentStore.paymentProviders;
  protected readonly selectedProvider = computed<PaymentProvider | null>(
    () => this.providers().find((provider) => provider.id === this.selectedProviderId()) ?? this.providers()[0] ?? null,
  );
  protected readonly selectedConfiguration = computed(() => {
    const provider = this.selectedProvider();
    return provider
      ? this.paymentStore.providerConfigurations().find((configuration) => configuration.providerId === provider.id) ?? null
      : null;
  });

  protected readonly providerMetrics = this.aiStore.providerMetrics;
  protected readonly providerHealth = this.aiStore.providerHealth;
  protected readonly layerAnalytics = this.aiStore.layerAnalytics;
  protected readonly workspaceUsage = this.aiStore.workspaceAiUsage;
  protected readonly failures = this.aiStore.aiFailures;
  protected readonly providerFilterOptions = computed(() =>
    uniqueBy(
      [
        ...this.providerMetrics().map((metric) => ({
          id: metric.providerId,
          label: metric.providerName,
        })),
        ...this.providerHealth().map((health) => ({
          id: health.providerId,
          label: health.providerName,
        })),
        ...this.failures().map((failure) => ({
          id: failure.providerId,
          label: failure.providerName,
        })),
      ],
      (option) => option.id,
    ),
  );

  protected readonly providerHealthRows = computed(() =>
    this.providerHealth().map((health) => ({
      health,
      metric: this.metricForProvider(health.providerId),
    })),
  );
  protected readonly providerHealthStats = computed<readonly StatTile[]>(() => {
    const health = this.providerHealth();
    const metrics = this.providerMetrics();
    const avgLatency = average(metrics.map((metric) => metric.avgLatencyMs));
    const totalRequests = metrics.reduce((total, metric) => total + metric.totalRequests, 0);
    const failedRequests = metrics.reduce((total, metric) => total + metric.failedRequests, 0);

    return [
      {
        label: 'Healthy providers',
        value: health.filter((provider) => provider.status === ProviderHealthStatus.Healthy).length,
        helper: 'Providers currently ready for routing.',
        icon: 'heart-pulse',
        tone: 'brand',
      },
      {
        label: 'Degraded providers',
        value: health.filter((provider) => provider.status === ProviderHealthStatus.Degraded).length,
        helper: 'Providers slower or less stable than usual.',
        icon: 'activity',
        tone: 'blue',
      },
      {
        label: 'Failed providers',
        value: health.filter((provider) => provider.status === ProviderHealthStatus.Down).length,
        helper: 'Providers currently unavailable.',
        icon: 'triangle-alert',
        tone: 'red',
      },
      {
        label: 'Avg response time',
        value: avgLatency === null ? '--' : `${Math.round(avgLatency)} ms`,
        helper: 'Average latency from provider metrics.',
        icon: 'timer',
        tone: 'neutral',
      },
      {
        label: 'Failure rate',
        value: totalRequests > 0 ? `${((failedRequests / totalRequests) * 100).toFixed(1)}%` : '--',
        helper: 'Failed provider requests over total requests.',
        icon: 'shield-alert',
        tone: failedRequests > 0 ? 'red' : 'brand',
      },
      {
        label: 'Cost trend',
        value: this.aiStore.totalCost() > 0 ? `$${this.aiStore.totalCost().toFixed(2)}` : '--',
        helper: 'Estimated AI cost from monitoring records.',
        icon: 'chart-no-axes-combined',
        tone: 'blue',
      },
    ];
  });

  protected readonly filteredLayers = computed(() => {
    const providerId = this.filterForm.controls.provider.value;
    const layer = this.filterForm.controls.layer.value.toLowerCase().trim();
    return this.layerAnalytics()
      .filter((item) => !providerId || item.providerId === providerId)
      .filter((item) => !layer || item.layerName.toLowerCase().includes(layer) || item.layerType.toLowerCase().includes(layer));
  });
  protected readonly layerStats = computed<readonly StatTile[]>(() => {
    const layers = this.filteredLayers();
    return [
      {
        label: 'Usage count',
        value: layers.reduce((total, layer) => total + layer.totalExecutions, 0),
        helper: 'Layer executions in this view.',
        icon: 'layers-3',
        tone: 'brand',
      },
      {
        label: 'Avg cost',
        value: formatMoney(average(layers.map((layer) => layer.avgExecutionCostUsd))),
        helper: 'Average cost per layer run.',
        icon: 'coins',
        tone: 'blue',
      },
      {
        label: 'Avg duration',
        value: formatMs(average(layers.map((layer) => layer.avgExecutionTimeMs))),
        helper: 'Average layer run time.',
        icon: 'timer',
        tone: 'neutral',
      },
      {
        label: 'Failure count',
        value: layers.reduce((total, layer) => total + layer.failedExecutions, 0),
        helper: 'Failed layer executions.',
        icon: 'triangle-alert',
        tone: 'red',
      },
    ];
  });

  protected readonly totalAiCost = computed(() => this.aiStore.totalCost());
  protected readonly costByProvider = computed<readonly CostRow[]>(() =>
    this.providerMetrics()
      .map((metric) => ({
        id: metric.providerId,
        name: metric.providerName,
        value: metric.avgCostUsd * metric.totalRequests,
        helper: `${metric.totalRequests} requests`,
      }))
      .sort((a, b) => b.value - a.value),
  );
  protected readonly costByWorkspace = computed<readonly CostRow[]>(() =>
    this.workspaceUsage()
      .map((workspace) => ({
        id: workspace.workspaceId,
        name: workspace.workspaceName,
        value: workspace.totalEstimatedCostUsd,
        helper: `${workspace.totalGenerationRequests} requests`,
        warning: workspace.highCostWarning,
      }))
      .sort((a, b) => b.value - a.value),
  );
  protected readonly costByTool = computed<readonly CostRow[]>(() => {
    const grouped = new Map<string, CostRow>();
    for (const layer of this.layerAnalytics()) {
      const value = layer.avgExecutionCostUsd * layer.totalExecutions;
      const existing = grouped.get(layer.layerName);
      grouped.set(layer.layerName, {
        id: layer.layerId,
        name: layer.layerName,
        value: (existing?.value ?? 0) + value,
        helper: `${layer.layerType} layer`,
      });
    }
    return [...grouped.values()].sort((a, b) => b.value - a.value);
  });
  protected readonly highCostWarnings = computed(() =>
    this.workspaceUsage().filter((workspace) => workspace.highCostWarning),
  );

  protected readonly filteredFailures = computed(() => {
    const providerId = this.filterForm.controls.provider.value;
    const layer = this.filterForm.controls.layer.value.toLowerCase().trim();
    const workspace = this.filterForm.controls.workspace.value.toLowerCase().trim();
    return this.failures()
      .filter((failure) => !providerId || failure.providerId === providerId)
      .filter((failure) => !layer || failure.layerName.toLowerCase().includes(layer))
      .filter((failure) => !workspace || failure.creativeRequestId.toLowerCase().includes(workspace))
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt));
  });

  protected readonly credentialForm = this.formBuilder.group({
    environmentType: [EnvironmentType.Sandbox, Validators.required],
    apiKey: ['', Validators.required],
    webhookUrl: [''],
    isActive: [true],
  });

  protected readonly filterForm = this.formBuilder.group({
    dateRange: ['30d'],
    provider: [''],
    layer: [''],
    workspace: [''],
  });

  protected readonly navItems = [
    { label: 'Provider Settings', route: '/master/provider-settings' },
    { label: 'Provider Health', route: '/master/provider-health' },
    { label: 'Layer Analytics', route: '/master/layer-analytics' },
    { label: 'AI Cost Usage', route: '/master/ai-cost-usage' },
    { label: 'AI Failures', route: '/master/ai-failures' },
  ] as const;

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      void this.loadCurrentMode();
    });

    effect(() => {
      const provider = this.selectedProvider();
      this.selectedProviderId.set(this.selectedProviderId() ?? provider?.id ?? null);
    });
  }

  protected refresh(): void {
    if (!this.accessDenied()) {
      void this.loadCurrentMode();
    }
  }

  protected clearFilters(): void {
    this.filterForm.reset({ dateRange: '30d', provider: '', layer: '', workspace: '' });
    this.aiStore.clearFilters();
  }

  protected selectProvider(providerId: string): void {
    this.selectedProviderId.set(providerId);
    this.credentialForm.reset({
      environmentType: EnvironmentType.Sandbox,
      apiKey: '',
      webhookUrl: '',
      isActive: true,
    });
  }

  protected async saveCredential(): Promise<void> {
    const provider = this.selectedProvider();
    if (!provider || this.credentialForm.invalid) {
      this.credentialForm.markAllAsTouched();
      return;
    }

    const value = this.credentialForm.getRawValue();
    await this.paymentStore.createProviderConfiguration({
      providerId: provider.id,
      environmentType: value.environmentType,
      apiBaseUrl: null,
      merchantId: null,
      successUrl: value.webhookUrl || null,
      failureUrl: null,
      cancelUrl: null,
      isActive: value.isActive,
      secrets: { apiKey: value.apiKey },
    });
    this.credentialForm.patchValue({ apiKey: '' }, { emitEvent: false });
  }

  protected async toggleProvider(provider: PaymentProvider): Promise<void> {
    await this.paymentStore.updatePaymentProvider(provider.id, {
      name: provider.name,
      code: provider.code,
      providerType: provider.providerType,
      isEnabled: !provider.isEnabled,
      sandboxEnabled: provider.sandboxEnabled,
      liveEnabled: provider.liveEnabled,
      priority: provider.priority,
    });
  }

  protected providerMode(provider: PaymentProvider): string {
    if (provider.liveEnabled && provider.sandboxEnabled) {
      return 'Sandbox + Live';
    }
    if (provider.liveEnabled) {
      return 'Live';
    }
    if (provider.sandboxEnabled) {
      return 'Sandbox';
    }
    return 'Mode not enabled';
  }

  protected providerCredentialStatus(provider: PaymentProvider): string {
    return this.paymentStore.providerConfigurations().some((configuration) => configuration.providerId === provider.id)
      ? 'Configured'
      : 'Not configured';
  }

  protected providerTone(provider: PaymentProvider): BadgeTone {
    return provider.isEnabled ? 'brand' : 'neutral';
  }

  protected healthTone(status: ProviderHealthStatus | string): BadgeTone {
    switch (status) {
      case ProviderHealthStatus.Healthy:
        return 'brand';
      case ProviderHealthStatus.Degraded:
      case ProviderHealthStatus.Cooldown:
        return 'blue';
      case ProviderHealthStatus.Down:
        return 'red';
      default:
        return 'neutral';
    }
  }

  protected successRate(metric: AiProviderMetric | null): string {
    if (!metric || metric.totalRequests <= 0) {
      return '--';
    }
    return `${((metric.successfulRequests / metric.totalRequests) * 100).toFixed(1)}%`;
  }

  protected metricForProvider(providerId: string): AiProviderMetric | null {
    return this.providerMetrics().find((metric) => metric.providerId === providerId) ?? null;
  }

  protected failureReason(failure: AiFailureLog): string {
    return failure.failureReason || friendlyFailureType(failure.failureType);
  }

  private async loadCurrentMode(): Promise<void> {
    switch (this.mode()) {
      case 'provider-settings':
        await this.paymentStore.loadPaymentProviders();
        return;
      case 'layer-analytics':
        await this.aiStore.loadLayerAnalytics();
        return;
      case 'ai-cost-usage':
        await this.aiStore.loadMasterDashboardData();
        return;
      case 'ai-failures':
        await this.aiStore.loadMasterFailures();
        return;
      default:
        await Promise.all([this.aiStore.loadProviderHealth(), this.aiStore.loadProviderMetrics()]);
    }
  }
}

function average(values: readonly number[]): number | null {
  const finiteValues = values.filter((value) => Number.isFinite(value));
  return finiteValues.length
    ? finiteValues.reduce((total, value) => total + value, 0) / finiteValues.length
    : null;
}

function formatMoney(value: number | null): string {
  return value === null ? '--' : `$${value.toFixed(4)}`;
}

function formatMs(value: number | null): string {
  return value === null ? '--' : `${Math.round(value)} ms`;
}

function friendlyFailureType(type: string): string {
  return type
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function uniqueBy<T>(items: readonly T[], selector: (item: T) => string): readonly T[] {
  const seen = new Set<string>();
  return items.filter((item) => {
    const key = selector(item);
    if (seen.has(key)) {
      return false;
    }
    seen.add(key);
    return true;
  });
}
