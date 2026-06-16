import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { startWith } from 'rxjs';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  AiFailureLog,
  AiLayerAnalytics,
  AiProviderHealth,
  AiProviderMetric,
  ProviderHealthStatus,
} from '../../ai-monitoring/models/ai-monitoring.models';
import { AiMonitoringStore } from '../../ai-monitoring/state/ai-monitoring.store';
import {
  MasterProviderEnvironment,
  MasterProviderType,
  MasterProviderView,
  ProviderConnectionTestResult,
} from './master-provider.models';
import { MasterProviderService } from './master-provider.service';

type MasterAiOpsMode = 'provider-settings' | 'provider-health' | 'layer-analytics' | 'ai-cost-usage' | 'ai-failures';
type BadgeTone = 'neutral' | 'brand' | 'blue' | 'red';

interface CredentialFormBaseline {
  readonly environment: MasterProviderEnvironment;
  readonly webhookUrl: string;
  readonly availableCreditBalance: number | null;
  readonly active: boolean;
}

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
  private readonly routeData = toSignal(this.route.data, { initialValue: this.route.snapshot.data });
  private readonly masterProviderService = inject(MasterProviderService);
  private readonly notifications = inject(NotificationStateService);

  protected readonly permissions = inject(PermissionStore);
  protected readonly aiStore = inject(AiMonitoringStore);

  protected readonly selectedProviderId = signal<string | null>(null);
  protected readonly providerSettingsLoading = signal(false);
  protected readonly providerSettingsError = signal<string | null>(null);
  protected readonly providerActionLoading = signal<'create' | 'save' | 'test' | 'revoke' | null>(null);
  protected readonly providers = signal<readonly MasterProviderView[]>([]);
  protected readonly addProviderOpen = signal(false);
  protected readonly providerPanelMessage = signal<string | null>(null);
  protected readonly credentialError = signal<string | null>(null);
  protected readonly testResult = signal<ProviderConnectionTestResult | null>(null);
  private readonly credentialBaseline = signal<CredentialFormBaseline | null>(null);

  protected readonly credentialForm = this.formBuilder.group({
    providerId: [''],
    providerCode: [''],
    environment: ['SANDBOX' as MasterProviderEnvironment, Validators.required],
    secret: [''],
    webhookUrl: [''],
    availableCreditBalance: [null as number | null, [Validators.min(0)]],
    active: [true],
  });
  private readonly credentialFormValue = toSignal(
    this.credentialForm.valueChanges.pipe(startWith(this.credentialForm.getRawValue())),
    { initialValue: this.credentialForm.getRawValue() },
  );

  protected readonly mode = computed<MasterAiOpsMode>(
    () => (this.routeData()['mode'] as MasterAiOpsMode | undefined) ?? 'provider-health',
  );
  protected readonly title = computed(() => {
    switch (this.mode()) {
      case 'provider-settings':
        return 'Provider Management';
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
        return 'Manage provider status, categories, and credential updates without exposing saved secrets.';
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
        return !this.permissions.canManagePaymentProviders() && !this.permissions.canViewAiMonitoring();
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

  protected readonly selectedProvider = computed<MasterProviderView | null>(
    () => this.providers().find((provider) => provider.id === this.selectedProviderId()) ?? this.providers()[0] ?? null,
  );
  protected readonly canTestCredential = computed(() => {
    const provider = this.selectedProvider();
    return Boolean(provider && this.isCredentialConfigured(provider) && !this.hasNewSecret());
  });
  protected readonly canSaveCredential = computed(() => {
    const provider = this.selectedProvider();
    this.credentialFormValue();
    if (!provider || this.credentialForm.invalid) {
      return false;
    }

    const hasNewSecret = this.hasNewSecret();
    const hasMetadataChange = this.hasCredentialMetadataChange();
    if (!this.isCredentialConfigured(provider)) {
      return hasNewSecret;
    }
    return hasNewSecret || hasMetadataChange;
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
  protected readonly hasAiCostData = computed(() =>
    this.providerMetrics().length > 0 || this.workspaceUsage().length > 0 || this.layerAnalytics().length > 0,
  );
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

  protected readonly providerForm = this.formBuilder.group({
    providerType: ['AI' as MasterProviderType, Validators.required],
    providerCode: ['', Validators.required],
    displayName: ['', Validators.required],
    description: [''],
    supportsSandbox: [true],
    supportsLive: [true],
    defaultEnvironment: ['SANDBOX' as MasterProviderEnvironment, Validators.required],
    active: [true],
  });

  protected readonly filterForm = this.formBuilder.group({
    dateRange: ['30d'],
    provider: [''],
    layer: [''],
    workspace: [''],
  });

  protected readonly navItems = [
    { label: 'Provider Management', route: '/master/provider-management' },
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

    effect(() => {
      const provider = this.selectedProvider();
      if (!provider) {
        return;
      }
      this.credentialForm.patchValue(
        {
          providerId: provider.id,
          providerCode: provider.providerCode,
          environment: provider.activeEnvironment,
          secret: '',
          webhookUrl: provider.webhookUrl ?? '',
          availableCreditBalance: this.providerAvailableCreditBalance(provider),
          active: provider.active,
        },
        { emitEvent: false },
      );
      this.credentialBaseline.set({
        environment: provider.activeEnvironment,
        webhookUrl: provider.webhookUrl ?? '',
        availableCreditBalance: this.providerAvailableCreditBalance(provider),
        active: provider.active,
      });
      this.credentialForm.markAsPristine();
      this.credentialError.set(null);
      this.providerPanelMessage.set(null);
      this.testResult.set(null);
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
  }

  protected openAddProvider(): void {
    this.providerForm.reset({
      providerType: 'AI',
      providerCode: '',
      displayName: '',
      description: '',
      supportsSandbox: true,
      supportsLive: true,
      defaultEnvironment: 'SANDBOX',
      active: true,
    });
    this.addProviderOpen.set(true);
  }

  protected closeAddProvider(): void {
    if (!this.providerActionLoading()) {
      this.addProviderOpen.set(false);
    }
  }

  protected async createProvider(): Promise<void> {
    if (this.providerForm.invalid || this.providerActionLoading()) {
      this.providerForm.markAllAsTouched();
      return;
    }

    this.providerActionLoading.set('create');
    try {
      const value = this.providerForm.getRawValue();
      const provider = await this.masterProviderService.createProvider({
        providerCode: value.providerCode,
        displayName: value.displayName,
        providerType: value.providerType,
        description: value.description || null,
        supportsSandbox: value.supportsSandbox,
        supportsLive: value.supportsLive,
        defaultEnvironment: value.defaultEnvironment,
        active: value.active,
      });
      this.notifications.success('Provider created', `${provider.displayName} is ready for credentials.`, 'provider-created');
      this.addProviderOpen.set(false);
      await this.loadProviderSettings(provider.id);
    } catch (error) {
      this.notifications.error('Provider could not be created', friendlyError(error), 'provider-create-failed');
    } finally {
      this.providerActionLoading.set(null);
    }
  }

  protected async saveCredential(): Promise<void> {
    const provider = this.selectedProvider();
    if (!provider || this.credentialForm.invalid || this.providerActionLoading() || !this.canSaveCredential()) {
      this.credentialForm.markAllAsTouched();
      return;
    }

    if (!this.isCredentialConfigured(provider) && !this.credentialForm.controls.secret.value.trim()) {
      this.credentialError.set('Paste a secret before saving the first credential for this provider.');
      return;
    }

    const value = this.credentialForm.getRawValue();
    this.providerActionLoading.set('save');
    this.credentialError.set(null);
    try {
      await this.masterProviderService.saveCredential(provider.id, {
        environment: value.environment,
        secret: value.secret.trim() || null,
        webhookUrl: value.webhookUrl.trim() || null,
        availableCreditBalance: normalizeOptionalNumber(value.availableCreditBalance),
        active: value.active,
      });
      this.notifications.success('Credential saved', 'Saved secrets are encrypted and hidden.', 'provider-credential-saved');
      this.providerPanelMessage.set('Next step: configure Provider Routing for this provider.');
      this.credentialForm.patchValue({ secret: '' }, { emitEvent: false });
      await this.loadProviderSettings(provider.id);
    } catch (error) {
      const message = friendlyError(error);
      this.credentialError.set(message);
      this.notifications.error('Credential could not be saved', message, 'provider-credential-save-failed');
    } finally {
      this.providerActionLoading.set(null);
    }
  }

  protected async testConnection(): Promise<void> {
    const provider = this.selectedProvider();
    if (!provider || !this.canTestCredential() || this.providerActionLoading()) {
      return;
    }

    const value = this.credentialForm.getRawValue();
    this.providerActionLoading.set('test');
    this.credentialError.set(null);
    try {
      const result = await this.masterProviderService.testConnection(provider.id, {
        environment: value.environment,
        secret: null,
      });
      this.testResult.set(result);
      const title = result.testStatus === 'SUCCESS' ? 'Connection test passed' : 'Connection test completed';
      this.notifications.info(title, result.message, 'provider-test-result');
      await this.loadProviderSettings(provider.id);
    } catch (error) {
      const message = friendlyError(error);
      this.credentialError.set(message);
      this.notifications.error('Connection test failed', message, 'provider-test-failed');
    } finally {
      this.providerActionLoading.set(null);
    }
  }

  protected async revokeCredential(): Promise<void> {
    const provider = this.selectedProvider();
    if (!provider || !this.isCredentialConfigured(provider) || this.providerActionLoading()) {
      return;
    }

    this.providerActionLoading.set('revoke');
    try {
      await this.masterProviderService.revokeCredential(provider.id, this.credentialForm.controls.environment.value);
      this.notifications.success('Credential revoked', 'The saved secret is no longer active.', 'provider-credential-revoked');
      await this.loadProviderSettings(provider.id);
    } catch (error) {
      const message = friendlyError(error);
      this.credentialError.set(message);
      this.notifications.error('Credential could not be revoked', message, 'provider-credential-revoke-failed');
    } finally {
      this.providerActionLoading.set(null);
    }
  }

  protected async toggleProvider(provider: MasterProviderView): Promise<void> {
    if (this.providerActionLoading()) {
      return;
    }

    this.providerActionLoading.set('save');
    try {
      const nextStatus = provider.active ? 'INACTIVE' : 'ACTIVE';
      await this.masterProviderService.updateProviderStatus(provider.id, nextStatus);
      this.notifications.success(
        nextStatus === 'ACTIVE' ? 'Provider enabled' : 'Provider disabled',
        `${provider.displayName} status was updated.`,
        'provider-status-updated',
      );
      await this.loadProviderSettings(provider.id);
    } catch (error) {
      this.notifications.error('Provider status could not be updated', friendlyError(error), 'provider-status-failed');
    } finally {
      this.providerActionLoading.set(null);
    }
  }

  protected providerMode(provider: MasterProviderView): string {
    if (provider.supportsLive && provider.supportsSandbox) {
      return 'Sandbox + Live';
    }
    if (provider.supportsLive) {
      return 'Live';
    }
    if (provider.supportsSandbox) {
      return 'Sandbox';
    }
    return 'Mode not enabled';
  }

  protected providerSubtitle(provider: MasterProviderView): string {
    const capability = provider.providerCode === 'OPENAI' ? 'Text/Image' : provider.category || provider.providerType;
    return `${provider.providerType} - ${capability} - ${this.providerMode(provider)}`;
  }

  protected providerHealthLabel(provider: MasterProviderView): string {
    return this.normalizeHealthStatus(this.providerHealth().find((health) => health.providerId === provider.id)?.status ?? provider.lastTestStatus);
  }

  protected providerCredentialStatus(provider: MasterProviderView): string {
    return this.isCredentialConfigured(provider) ? 'Configured' : 'Not configured';
  }

  protected providerTone(provider: MasterProviderView): BadgeTone {
    return provider.active ? 'brand' : 'neutral';
  }

  protected credentialTone(provider: MasterProviderView): BadgeTone {
    return this.isCredentialConfigured(provider) ? 'brand' : 'neutral';
  }

  protected testTone(status: string | null | undefined): BadgeTone {
    if (status === 'SUCCESS' || status === 'HEALTHY') {
      return 'brand';
    }
    if (status === 'FAILED' || status === 'DOWN') {
      return 'red';
    }
    return 'neutral';
  }

  protected secretLabel(provider: MasterProviderView): string {
    const name = provider.displayName || provider.providerCode;
    const baseLabel = provider.providerCode === 'OPENAI' ? 'OpenAI API Key' : `${name} API Key / Secret`;
    return this.isCredentialConfigured(provider) ? `Rotate ${baseLabel}` : baseLabel;
  }

  protected secretPlaceholder(provider: MasterProviderView | null): string {
    if (!provider) {
      return 'Paste new API key to configure or rotate credential';
    }
    if (!this.isCredentialConfigured(provider) && provider.providerCode === 'OPENAI') {
      return 'Paste OpenAI API key';
    }
    if (this.isCredentialConfigured(provider)) {
      return 'Paste new key to rotate';
    }
    return 'Paste new API key to configure or rotate credential';
  }

  protected providerCredentialHelper(provider: MasterProviderView): string {
    return 'Saved credentials are never shown again. Paste a new key only when configuring or rotating credentials.';
  }

  protected providerGuidance(provider: MasterProviderView): string | null {
    const configured = this.isCredentialConfigured(provider);
    if (provider.active && !configured) {
      return 'Provider is enabled but credential is missing. Add an API key before routing can use this provider.';
    }
    if (configured && !provider.active) {
      return 'Credential saved, but provider is disabled.';
    }
    if (configured && provider.active && this.normalizeHealthStatus(provider.lastTestStatus) === 'Not tested') {
      return 'Credential saved. Run Test connection to verify provider health.';
    }
    return null;
  }

  protected isCredentialConfigured(provider: MasterProviderView): boolean {
    return provider.credentialConfigured ?? ['CONFIGURED', 'INVALID', 'EXPIRED'].includes(provider.credentialStatus);
  }

  protected credentialUpdatedAt(provider: MasterProviderView): string | null {
    return provider.credentialUpdatedAt ?? provider.updatedAt ?? null;
  }

  protected providerAvailableCreditBalance(provider: MasterProviderView | null): number | null {
    return normalizeOptionalNumber(provider?.availableCreditBalance);
  }

  protected providerAvailableCreditLabel(provider: MasterProviderView): string {
    const balance = this.providerAvailableCreditBalance(provider);
    return balance === null
      ? 'Not set'
      : balance.toLocaleString(undefined, {
          currency: 'USD',
          maximumFractionDigits: 2,
          minimumFractionDigits: 2,
          style: 'currency',
        });
  }

  protected normalizeHealthStatus(status: ProviderHealthStatus | string | null | undefined): string {
    switch (status) {
      case ProviderHealthStatus.Healthy:
      case 'SUCCESS':
      case 'HEALTHY':
        return 'Healthy';
      case ProviderHealthStatus.Degraded:
      case ProviderHealthStatus.Cooldown:
      case 'NOT_IMPLEMENTED':
      case 'DEGRADED':
        return 'Degraded';
      case ProviderHealthStatus.Down:
      case 'FAILED':
      case 'DOWN':
        return 'Failed';
      default:
        return 'Not tested';
    }
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
        await this.loadProviderSettings();
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

  private async loadProviderSettings(selectProviderId?: string): Promise<void> {
    this.providerSettingsLoading.set(true);
    this.providerSettingsError.set(null);
    try {
      const providers = await this.masterProviderService.listProviders();
      this.providers.set(providers);
      const nextSelection =
        providers.find((provider) => provider.id === selectProviderId)?.id ??
        providers.find((provider) => provider.id === this.selectedProviderId())?.id ??
        providers[0]?.id ??
        null;
      this.selectedProviderId.set(nextSelection);
    } catch (error) {
      this.providerSettingsError.set(friendlyError(error));
    } finally {
      this.providerSettingsLoading.set(false);
    }
  }

  private hasNewSecret(): boolean {
    this.credentialFormValue();
    return this.credentialForm.controls.secret.value.trim().length > 0;
  }

  private hasCredentialMetadataChange(): boolean {
    this.credentialFormValue();
    const baseline = this.credentialBaseline();
    if (!baseline) {
      return false;
    }
    const value = this.credentialForm.getRawValue();
    return (
      value.environment !== baseline.environment ||
      value.webhookUrl.trim() !== baseline.webhookUrl.trim() ||
      normalizeOptionalNumber(value.availableCreditBalance) !== baseline.availableCreditBalance ||
      value.active !== baseline.active
    );
  }
}

function normalizeOptionalNumber(value: unknown): number | null {
  if (value === null || value === undefined || value === '') {
    return null;
  }
  const normalized = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(normalized) ? normalized : null;
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

function titleCase(value: string): string {
  return value
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function friendlyError(error: unknown): string {
  if (typeof error === 'object' && error && 'message' in error) {
    const httpError = error as {
      readonly error?: {
        readonly message?: unknown;
        readonly errors?: readonly { readonly message?: unknown; readonly field?: unknown }[];
      };
      readonly message?: unknown;
    };
    const apiMessage = httpError.error?.message;
    if (typeof apiMessage === 'string' && apiMessage.trim()) {
      return apiMessage;
    }
    const firstError = httpError.error?.errors?.[0];
    if (firstError?.message) {
      return String(firstError.message);
    }
    if (typeof httpError.message === 'string' && httpError.message.trim()) {
      return httpError.message;
    }
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return 'Request failed. Please retry.';
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
