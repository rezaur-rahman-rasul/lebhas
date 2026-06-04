import { Injectable, computed, inject, signal } from '@angular/core';
import { normalizeHttpError } from '@app/core/api/http-error';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { CreditLedgerItemView, MasterCreditOverviewView, ProviderCreditAdjustmentRequest, ProviderCreditPoolView } from '../models/provider-credit-exchange.models';
import { ProviderCreditApiService } from '../services/provider-credit-api.service';

@Injectable({ providedIn: 'root' })
export class ProviderCreditsStore {
  private readonly api = inject(ProviderCreditApiService);
  private readonly notifications = inject(NotificationStateService);

  private readonly providerCreditPoolsSignal = signal<readonly ProviderCreditPoolView[]>([]);
  private readonly selectedProviderPoolSignal = signal<ProviderCreditPoolView | null>(null);
  private readonly providerLedgerSignal = signal<readonly CreditLedgerItemView[]>([]);
  private readonly masterCreditOverviewSignal = signal<MasterCreditOverviewView | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly adjustingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly providerCreditPools = this.providerCreditPoolsSignal.asReadonly();
  readonly selectedProviderPool = this.selectedProviderPoolSignal.asReadonly();
  readonly providerLedger = this.providerLedgerSignal.asReadonly();
  readonly masterCreditOverview = this.masterCreditOverviewSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly adjusting = this.adjustingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly lowBalanceProviders = computed(() => this.providerCreditPoolsSignal().filter((pool) => pool.availableInternalCredits < pool.lowBalanceThreshold));
  readonly totalAvailableProviderCredits = computed(() => this.providerCreditPoolsSignal().reduce((total, pool) => total + pool.availableInternalCredits, 0));
  readonly totalReservedProviderCredits = computed(() => this.providerCreditPoolsSignal().reduce((total, pool) => total + pool.reservedInternalCredits, 0));

  async loadPool(providerId: string): Promise<void> {
    await this.run(async () => {
      const pool = await this.api.getProviderCreditPool(providerId);
      this.selectedProviderPoolSignal.set(pool);
      this.providerCreditPoolsSignal.update((items) => upsertByProvider(items, pool));
      this.providerLedgerSignal.set(await this.api.getProviderCreditLedger(providerId));
    });
  }

  async loadOverview(): Promise<void> {
    await this.run(async () => {
      const overview = await this.api.getMasterCreditOverview();
      this.masterCreditOverviewSignal.set(overview);
      this.providerCreditPoolsSignal.set(overview.providerPools ?? []);
    });
  }

  async adjustProviderCreditPool(providerId: string, payload: ProviderCreditAdjustmentRequest): Promise<boolean> {
    this.adjustingSignal.set(true);
    this.errorSignal.set(null);
    try {
      const pool = await this.api.adjustProviderCreditPool(providerId, payload);
      this.selectedProviderPoolSignal.set(pool);
      this.providerCreditPoolsSignal.update((items) => upsertByProvider(items, pool));
      this.providerLedgerSignal.set(await this.api.getProviderCreditLedger(providerId));
      await this.loadOverview();
      this.notifications.success('Credits adjusted successfully.', 'Credits adjusted successfully.');
      return true;
    } catch (error) {
      const message = normalizeHttpError(error).message || 'Credit pool could not be updated.';
      this.errorSignal.set(message);
      this.notifications.error('Credit pool could not be updated.', 'Credit pool could not be updated.');
      return false;
    } finally {
      this.adjustingSignal.set(false);
    }
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      await action();
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message || 'Credit balance could not be loaded.');
    } finally {
      this.loadingSignal.set(false);
    }
  }
}

function upsertByProvider(items: readonly ProviderCreditPoolView[], pool: ProviderCreditPoolView): readonly ProviderCreditPoolView[] {
  return items.some((item) => item.providerId === pool.providerId) ? items.map((item) => item.providerId === pool.providerId ? pool : item) : [...items, pool];
}
