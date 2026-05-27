import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { AppDrawerComponent } from '@app/shared/components/app-drawer/app-drawer';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { PaymentEmptyStateComponent } from '../components/payment-empty-state/payment-empty-state';
import { PaymentLoadingStateComponent } from '../components/payment-loading-state/payment-loading-state';
import { PaymentProviderCardComponent } from '../components/payment-provider-card/payment-provider-card';
import { PaymentProviderFormComponent } from '../components/payment-provider-form/payment-provider-form';
import { PaymentProvider, PaymentProviderPayload } from '../models/payment.models';
import { PaymentStore } from '../state/payment.store';

type ProviderDrawerMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-payment-providers-page',
  standalone: true,
  imports: [
    ButtonComponent,
    CardComponent,
    AppDrawerComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    PaymentEmptyStateComponent,
    PaymentLoadingStateComponent,
    PaymentProviderCardComponent,
    PaymentProviderFormComponent,
  ],
  templateUrl: './payment-providers.html',
  styleUrl: './payment-providers.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentProvidersPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(PaymentStore);

  protected readonly drawerMode = signal<ProviderDrawerMode>(null);
  protected readonly selectedProvider = signal<PaymentProvider | null>(null);
  protected readonly pendingDisableProvider = signal<PaymentProvider | null>(null);

  protected readonly accessDenied = computed(() => !this.permissions.canManagePaymentProviders());
  protected readonly providers = this.store.paymentProviders;
  protected readonly hasProviders = computed(() => this.providers().length > 0);
  protected readonly drawerOpen = computed(() => this.drawerMode() !== null);
  protected readonly drawerTitle = computed(() =>
    this.drawerMode() === 'edit' ? 'Edit payment provider' : 'Create payment provider',
  );

  constructor() {
    effect(() => {
      if (this.permissions.canManagePaymentProviders()) {
        void this.store.loadPaymentProviders();
      }
    });
  }

  protected refresh(): void {
    if (!this.permissions.canManagePaymentProviders()) {
      return;
    }

    void this.store.loadPaymentProviders();
  }

  protected openCreate(): void {
    this.selectedProvider.set(null);
    this.drawerMode.set('create');
  }

  protected openEdit(provider: PaymentProvider): void {
    this.selectedProvider.set(provider);
    this.drawerMode.set('edit');
  }

  protected closeDrawer(): void {
    this.drawerMode.set(null);
    this.selectedProvider.set(null);
  }

  protected async saveProvider(payload: PaymentProviderPayload): Promise<void> {
    const provider = this.selectedProvider();
    const result = provider
      ? await this.store.updatePaymentProvider(provider.id, payload)
      : await this.store.createPaymentProvider(payload);

    if (result.ok) {
      this.closeDrawer();
    }
  }

  protected requestToggleProvider(provider: PaymentProvider): void {
    if (provider.isEnabled) {
      this.pendingDisableProvider.set(provider);
      return;
    }

    void this.toggleProvider(provider);
  }

  protected async confirmDisable(): Promise<void> {
    const provider = this.pendingDisableProvider();
    if (!provider) {
      return;
    }

    await this.toggleProvider(provider);
    this.pendingDisableProvider.set(null);
  }

  protected cancelDisable(): void {
    this.pendingDisableProvider.set(null);
  }

  private async toggleProvider(provider: PaymentProvider): Promise<void> {
    await this.store.updatePaymentProvider(provider.id, {
      name: provider.name,
      code: provider.code,
      providerType: provider.providerType,
      isEnabled: !provider.isEnabled,
      sandboxEnabled: provider.sandboxEnabled,
      liveEnabled: provider.liveEnabled,
      priority: provider.priority,
    });
  }
}
