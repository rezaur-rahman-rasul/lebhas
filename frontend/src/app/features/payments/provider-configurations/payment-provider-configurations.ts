import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { AppDrawerComponent } from '@app/shared/components/app-drawer/app-drawer';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { PaymentEmptyStateComponent } from '../components/payment-empty-state/payment-empty-state';
import { PaymentLoadingStateComponent } from '../components/payment-loading-state/payment-loading-state';
import { ProviderConfigurationFormComponent } from '../components/provider-configuration-form/provider-configuration-form';
import {
  PaymentProviderConfiguration,
  PaymentProviderConfigurationPayload,
} from '../models/payment.models';
import { PaymentStore } from '../state/payment.store';

type ConfigurationDrawerMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-payment-provider-configurations-page',
  standalone: true,
  imports: [
    DatePipe,
    ButtonComponent,
    CardComponent,
    AppDrawerComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    PaymentEmptyStateComponent,
    PaymentLoadingStateComponent,
    ProviderConfigurationFormComponent,
  ],
  templateUrl: './payment-provider-configurations.html',
  styleUrl: './payment-provider-configurations.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentProviderConfigurationsPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(PaymentStore);

  protected readonly drawerMode = signal<ConfigurationDrawerMode>(null);
  protected readonly selectedConfiguration = signal<PaymentProviderConfiguration | null>(null);

  protected readonly accessDenied = computed(() => !this.permissions.canManagePaymentProviders());
  protected readonly configurations = this.store.providerConfigurations;
  protected readonly hasConfigurations = computed(() => this.configurations().length > 0);
  protected readonly drawerOpen = computed(() => this.drawerMode() !== null);
  protected readonly drawerTitle = computed(() =>
    this.drawerMode() === 'edit' ? 'Edit provider configuration' : 'Create provider configuration',
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
    this.selectedConfiguration.set(null);
    this.drawerMode.set('create');
  }

  protected openEdit(configuration: PaymentProviderConfiguration): void {
    this.selectedConfiguration.set(configuration);
    this.drawerMode.set('edit');
  }

  protected closeDrawer(): void {
    this.drawerMode.set(null);
    this.selectedConfiguration.set(null);
  }

  protected async saveConfiguration(payload: PaymentProviderConfigurationPayload): Promise<void> {
    const configuration = this.selectedConfiguration();
    const result = configuration
      ? await this.store.updateProviderConfiguration(configuration.id, payload)
      : await this.store.createProviderConfiguration(payload);

    if (result.ok) {
      this.closeDrawer();
    }
  }

  protected async toggleActive(configuration: PaymentProviderConfiguration): Promise<void> {
    await this.store.updateProviderConfiguration(configuration.id, {
      providerId: configuration.providerId,
      environmentType: configuration.environmentType,
      apiBaseUrl: configuration.apiBaseUrl,
      merchantId: configuration.merchantId,
      successUrl: configuration.successUrl,
      failureUrl: configuration.failureUrl,
      cancelUrl: configuration.cancelUrl,
      isActive: !configuration.isActive,
    });
  }
}
