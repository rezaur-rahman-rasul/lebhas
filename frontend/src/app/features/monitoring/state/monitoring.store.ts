import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { PermissionStore } from '@app/core/permissions/permission.store';
import {
  AiProviderMonitoringSummary,
  MonitoringActionResult,
  MonitoringAlert,
  MonitoringAlertFilters,
  MonitoringSeverity,
  PaymentMonitoringSummary,
  SystemHealthEvent,
  SystemHealthStatus,
  WorkspaceMonitoringSummary,
} from '../models/monitoring.models';
import { MonitoringApiService } from '../services/monitoring-api.service';

@Injectable({ providedIn: 'root' })
export class MonitoringStore {
  private readonly api = inject(MonitoringApiService);
  private readonly permissions = inject(PermissionStore);

  private readonly systemHealthSignal = signal<readonly SystemHealthEvent[]>([]);
  private readonly alertsSignal = signal<readonly MonitoringAlert[]>([]);
  private readonly aiProviderSummarySignal = signal<AiProviderMonitoringSummary | null>(null);
  private readonly paymentSummarySignal = signal<PaymentMonitoringSummary | null>(null);
  private readonly workspaceSummarySignal = signal<WorkspaceMonitoringSummary | null>(null);
  private readonly selectedAlertTypeSignal = signal<string | null>(null);
  private readonly selectedSeveritySignal = signal<MonitoringSeverity | string | null>(null);
  private readonly unresolvedOnlySignal = signal(true);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly systemHealth = this.systemHealthSignal.asReadonly();
  readonly alerts = this.alertsSignal.asReadonly();
  readonly aiProviderSummary = this.aiProviderSummarySignal.asReadonly();
  readonly paymentSummary = this.paymentSummarySignal.asReadonly();
  readonly workspaceSummary = this.workspaceSummarySignal.asReadonly();
  readonly selectedAlertType = this.selectedAlertTypeSignal.asReadonly();
  readonly selectedSeverity = this.selectedSeveritySignal.asReadonly();
  readonly unresolvedOnly = this.unresolvedOnlySignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly criticalAlerts = computed(() =>
    this.alertsSignal().filter((alert) => alert.severity === MonitoringSeverity.Critical),
  );
  readonly unresolvedAlerts = computed(() => this.alertsSignal().filter((alert) => !alert.resolved));
  readonly healthyServices = computed(() =>
    this.systemHealthSignal().filter((event) => event.status === SystemHealthStatus.Healthy),
  );
  readonly degradedServices = computed(() =>
    this.systemHealthSignal().filter((event) => event.status === SystemHealthStatus.Degraded),
  );
  readonly downServices = computed(() =>
    this.systemHealthSignal().filter((event) => event.status === SystemHealthStatus.Down),
  );

  async loadMonitoringDashboard(): Promise<MonitoringActionResult> {
    if (!this.permissions.canViewMasterMonitoring()) {
      return this.restricted();
    }

    return this.run(async () => {
      const [systemHealth, alerts, aiProviderSummary, paymentSummary, workspaceSummary] =
        await Promise.all([
          this.api.getSystemHealth(),
          this.api.getAlerts(this.filters()),
          this.api.getAiProviderMonitoring(),
          this.api.getPaymentMonitoring(),
          this.api.getWorkspaceMonitoring(),
        ]);
      this.systemHealthSignal.set(systemHealth);
      this.alertsSignal.set(alerts);
      this.aiProviderSummarySignal.set(aiProviderSummary);
      this.paymentSummarySignal.set(paymentSummary);
      this.workspaceSummarySignal.set(workspaceSummary);
    });
  }

  async loadSystemHealth(): Promise<MonitoringActionResult> {
    if (!this.permissions.canViewSystemHealth()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.systemHealthSignal.set(await this.api.getSystemHealth());
    });
  }

  async loadMonitoringAlerts(): Promise<MonitoringActionResult> {
    if (!this.permissions.canViewMonitoringAlerts()) {
      return this.restricted();
    }

    return this.run(async () => {
      this.alertsSignal.set(await this.api.getAlerts(this.filters()));
    });
  }

  async loadAiProviderMonitoring(): Promise<MonitoringActionResult> {
    return this.masterOnly(async () => {
      this.aiProviderSummarySignal.set(await this.api.getAiProviderMonitoring());
    });
  }

  async loadPaymentMonitoring(): Promise<MonitoringActionResult> {
    return this.masterOnly(async () => {
      this.paymentSummarySignal.set(await this.api.getPaymentMonitoring());
    });
  }

  async loadWorkspaceMonitoring(): Promise<MonitoringActionResult> {
    return this.masterOnly(async () => {
      this.workspaceSummarySignal.set(await this.api.getWorkspaceMonitoring());
    });
  }

  setMonitoringSeverity(severity: MonitoringSeverity | string | null): void {
    this.selectedSeveritySignal.set(severity);
  }

  setMonitoringAlertType(alertType: string | null): void {
    this.selectedAlertTypeSignal.set(alertType);
  }

  setUnresolvedOnly(value: boolean): void {
    this.unresolvedOnlySignal.set(value);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private masterOnly(action: () => Promise<void>): Promise<MonitoringActionResult> {
    if (!this.permissions.canViewMasterMonitoring()) {
      return Promise.resolve(this.restricted());
    }

    return this.run(action);
  }

  private filters(): MonitoringAlertFilters {
    return {
      alertType: this.selectedAlertTypeSignal(),
      severity: this.selectedSeveritySignal(),
      unresolvedOnly: this.unresolvedOnlySignal(),
    };
  }

  private async run(action: () => Promise<void>): Promise<MonitoringActionResult> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      await action();
      return { ok: true };
    } catch (error) {
      const message =
        normalizeHttpError(error).message || 'We could not load monitoring data. Please try again.';
      this.errorSignal.set(message);
      return { ok: false, message };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private restricted(): MonitoringActionResult {
    const message = 'You do not have access to master monitoring.';
    this.errorSignal.set(message);
    return { ok: false, message };
  }
}
