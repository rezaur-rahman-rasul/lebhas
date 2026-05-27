import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { UsageBillingStore } from '@app/features/usage-billing/state/usage-billing.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { IconComponent } from '@app/shared/components/icon/icon';
import { InvoiceCardComponent } from '../components/invoice-card/invoice-card';
import { PaymentEmptyStateComponent } from '../components/payment-empty-state/payment-empty-state';
import { PaymentLoadingStateComponent } from '../components/payment-loading-state/payment-loading-state';
import { PaymentStatusBadgeComponent } from '../components/payment-status-badge/payment-status-badge';
import { PaymentTransactionCardComponent } from '../components/payment-transaction-card/payment-transaction-card';
import { Invoice, PaymentTransaction } from '../models/payment.models';
import { PaymentStore } from '../state/payment.store';

interface PaymentQuickAction {
  readonly label: string;
  readonly description: string;
  readonly route: string;
  readonly icon: string;
  readonly visible: boolean;
}

@Component({
  selector: 'app-payment-dashboard-page',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    IconComponent,
    InvoiceCardComponent,
    PaymentEmptyStateComponent,
    PaymentLoadingStateComponent,
    PaymentStatusBadgeComponent,
    PaymentTransactionCardComponent,
  ],
  templateUrl: './payment-dashboard.html',
  styleUrl: './payment-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentDashboardPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly usageBilling = inject(UsageBillingStore);
  protected readonly store = inject(PaymentStore);

  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly hasAccess = computed(
    () =>
      this.permissions.canPurchaseSubscription() ||
      this.permissions.canPurchaseCredits() ||
      this.permissions.canViewPayments() ||
      this.permissions.canViewInvoices(),
  );
  protected readonly subscription = this.workspace.subscription;
  protected readonly currentCreditBalance = computed(() => {
    const usageCredits = this.usageBilling.availableCredits();
    if (typeof usageCredits === 'number') {
      return usageCredits;
    }

    const workspaceCredits = this.workspace.usage()?.creditsRemaining;
    return typeof workspaceCredits === 'number' ? workspaceCredits : null;
  });
  protected readonly recentTransactions = computed<readonly PaymentTransaction[]>(() =>
    [...this.store.paymentTransactions()]
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
      .slice(0, 3),
  );
  protected readonly recentInvoices = computed<readonly Invoice[]>(() =>
    [...this.store.invoices()]
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
      .slice(0, 3),
  );
  protected readonly quickActions = computed<readonly PaymentQuickAction[]>(() =>
    [
      {
        label: 'Manage subscription',
        description: 'Choose a package and continue with backend payment redirect.',
        route: '/payments/subscription',
        icon: 'badge-dollar-sign',
        visible: this.permissions.canPurchaseSubscription(),
      },
      {
        label: 'Buy credits',
        description: 'Purchase backend-configured credit packages.',
        route: '/payments/credits',
        icon: 'wallet-cards',
        visible: this.permissions.canPurchaseCredits(),
      },
      {
        label: 'View payment history',
        description: 'Review payment attempts and provider confirmation status.',
        route: '/payments/transactions',
        icon: 'receipt-text',
        visible: this.permissions.canViewPayments(),
      },
      {
        label: 'View invoices',
        description: 'Review backend-issued invoice records.',
        route: '/payments/invoices',
        icon: 'file-text',
        visible: this.permissions.canViewInvoices(),
      },
    ].filter((action) => action.visible),
  );

  constructor() {
    effect(() => {
      if (!this.hasAccess()) {
        return;
      }

      void this.workspace.initialize();

      const workspaceId = this.workspaceId();
      if (!workspaceId) {
        return;
      }

      if (this.permissions.canViewPayments()) {
        void this.store.loadWorkspacePayments(workspaceId);
      }

      if (this.permissions.canViewInvoices()) {
        void this.store.loadWorkspaceInvoices(workspaceId);
      }

      if (this.permissions.canViewUsageBilling()) {
        void this.usageBilling.loadDashboard(workspaceId);
      }
    });
  }

  protected refresh(): void {
    this.store.clearError();
    this.usageBilling.clearError();

    const workspaceId = this.workspaceId();
    if (!workspaceId) {
      void this.workspace.initialize();
      return;
    }

    if (this.permissions.canViewPayments()) {
      void this.store.loadWorkspacePayments(workspaceId);
    }

    if (this.permissions.canViewInvoices()) {
      void this.store.loadWorkspaceInvoices(workspaceId);
    }

    if (this.permissions.canViewUsageBilling()) {
      void this.usageBilling.loadDashboard(workspaceId);
    }
  }
}
