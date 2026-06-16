import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { UsageBillingStore } from '@app/features/usage-billing/state/usage-billing.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { CreditPurchaseCardComponent } from '../components/credit-purchase-card/credit-purchase-card';
import { PaymentEmptyStateComponent } from '../components/payment-empty-state/payment-empty-state';
import { PaymentLoadingStateComponent } from '../components/payment-loading-state/payment-loading-state';
import { PaymentStatusBadgeComponent } from '../components/payment-status-badge/payment-status-badge';
import { CreditPackage, CreditPurchasePayload, PaymentProvider } from '../models/payment.models';
import { PaymentApiService } from '../services/payment-api.service';
import { PaymentStore } from '../state/payment.store';

@Component({
  selector: 'app-credit-purchase-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    CreditPurchaseCardComponent,
    PaymentEmptyStateComponent,
    PaymentLoadingStateComponent,
    PaymentStatusBadgeComponent,
  ],
  templateUrl: './credit-purchase.html',
  styleUrl: './credit-purchase.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditPurchasePage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly usageBilling = inject(UsageBillingStore);
  protected readonly store = inject(PaymentStore);
  private readonly paymentApi = inject(PaymentApiService);

  protected readonly paymentGateways = signal<readonly PaymentProvider[]>([]);
  protected readonly selectedGatewayCode = signal<string | null>(null);
  protected readonly accessDenied = computed(() => !this.permissions.canPurchaseCredits());
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly creditPackages = this.store.activeCreditPackages;
  protected readonly hasCreditPackages = computed(() => this.creditPackages().length > 0);
  protected readonly selectedCreditPackage = this.store.selectedCreditPackage;
  protected readonly selectedSummary = this.store.selectedCreditPurchaseSummary;
  protected readonly paymentSession = this.store.activePaymentSession;
  protected readonly currentCreditBalance = computed(() => {
    const usageCredits = this.usageBilling.availableCredits();
    if (typeof usageCredits === 'number') {
      return usageCredits;
    }

    const workspaceCredits = this.workspace.usage()?.creditsRemaining;
    return typeof workspaceCredits === 'number' ? workspaceCredits : null;
  });
  protected readonly canStartPayment = computed(
    () => Boolean(this.workspaceId() && this.selectedCreditPackage() && this.selectedGatewayCode()) && !this.store.loading(),
  );

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      void this.workspace.initialize();
      void this.store.loadCreditPackages();
    });

    effect(() => {
      const workspaceId = this.workspaceId();
      if (!workspaceId || this.accessDenied()) {
        return;
      }

      void this.loadPaymentGateways(workspaceId);
    });

    effect(() => {
      const workspaceId = this.workspaceId();
      if (!workspaceId || !this.permissions.canViewUsageBilling()) {
        return;
      }

      void this.usageBilling.loadDashboard(workspaceId);
    });
  }

  protected selectCreditPackage(creditPackage: CreditPackage): void {
    this.store.clearActivePaymentSession();
    this.store.setSelectedCreditPackage(creditPackage);
  }

  protected refresh(): void {
    if (this.accessDenied()) {
      return;
    }

    this.store.clearError();
    void this.store.loadCreditPackages();

    const workspaceId = this.workspaceId();
    if (workspaceId && this.permissions.canViewUsageBilling()) {
      void this.usageBilling.loadDashboard(workspaceId);
    }
  }

  protected async buyCredits(): Promise<void> {
    const workspaceId = this.workspaceId();
    const creditPackage = this.selectedCreditPackage();

    if (!workspaceId || !creditPackage) {
      return;
    }

    const payload: CreditPurchasePayload = {
      creditPackageId: creditPackage.id,
      preferredProviderCode: this.selectedGatewayCode(),
      returnUrl: typeof window === 'undefined' ? null : window.location.href,
    };

    await this.store.purchaseCredits(workspaceId, payload);
  }

  protected selectGateway(gatewayCode: string): void {
    this.store.clearActivePaymentSession();
    this.selectedGatewayCode.set(gatewayCode);
  }

  protected continueToPayment(): void {
    const redirectUrl = this.paymentSession()?.paymentRedirectUrl;
    if (!redirectUrl) {
      return;
    }

    window.location.assign(redirectUrl);
  }

  private async loadPaymentGateways(workspaceId: string): Promise<void> {
    const gateways = await this.paymentApi.getWorkspacePaymentGateways(workspaceId).catch(() => []);
    this.paymentGateways.set(gateways);

    if (!this.selectedGatewayCode() && gateways.length > 0) {
      this.selectedGatewayCode.set(gateways[0].code);
    }
  }
}
