import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { UsageBillingStore } from '@app/features/usage-billing/state/usage-billing.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { IconComponent } from '@app/shared/components/icon/icon';
import { BillingModalService } from '../services/billing-modal.service';
import { Invoice, PaymentStatus, PaymentTransaction } from '../models/payment.models';
import { PaymentStore } from '../state/payment.store';

interface BillingStat {
  readonly label: string;
  readonly value: string;
  readonly helper: string;
  readonly icon: string;
  readonly tone: 'brand' | 'success' | 'warning' | 'neutral';
}

@Component({
  selector: 'app-payment-dashboard-page',
  standalone: true,
  imports: [
    DatePipe,
    RouterLink,
    ButtonComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    IconComponent,
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
  protected readonly billingModal = inject(BillingModalService);

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
  protected readonly currentUsage = computed(
    () => this.usageBilling.currentMonthUsage() ?? this.usageBilling.usageSummary(),
  );
  protected readonly recentTransactions = computed<readonly PaymentTransaction[]>(() =>
    [...this.store.paymentTransactions()]
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
      .slice(0, 5),
  );
  protected readonly recentInvoices = computed<readonly Invoice[]>(() =>
    [...this.store.invoices()]
      .sort((a, b) => Date.parse(b.createdAt) - Date.parse(a.createdAt))
      .slice(0, 4),
  );
  protected readonly billingStats = computed<readonly BillingStat[]>(() => {
    const subscription = this.subscription();
    const usage = this.currentUsage();
    const credits = this.currentCreditBalance();

    return [
      {
        label: 'Current Plan',
        value: subscription?.planName ?? this.workspace.activePlanLabel(),
        helper: subscription?.status ? this.titleCase(subscription.status) : 'Status unavailable',
        icon: 'crown',
        tone: 'brand',
      },
      {
        label: 'Available Credits',
        value: credits === null ? 'Unavailable' : credits.toLocaleString(),
        helper: 'Ready for creative generation',
        icon: 'coins',
        tone: 'success',
      },
      {
        label: 'Monthly Usage',
        value: usage ? usage.usedCredits.toLocaleString() : 'Unavailable',
        helper: 'Credits used this month',
        icon: 'activity',
        tone: 'neutral',
      },
      {
        label: 'Next Renewal',
        value: subscription?.currentPeriodEnd ?? subscription?.trialEndsAt ?? 'Not set',
        helper: 'Subscription renewal date',
        icon: 'calendar-clock',
        tone: 'warning',
      },
      {
        label: 'Payment Status',
        value: this.paymentStatusSummary(),
        helper: `${this.store.pendingPayments().length} pending payment(s)`,
        icon: 'receipt-text',
        tone: 'neutral',
      },
    ];
  });

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
      if (this.permissions.canPurchaseCredits()) {
        void this.store.loadCreditPackages();
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

  protected openBillingModal(): void {
    this.billingModal.show();
  }

  protected money(amount: number, currency: string): string {
    return `${currency} ${amount.toLocaleString(undefined, { maximumFractionDigits: 2 })}`;
  }

  protected transactionTitle(transaction: PaymentTransaction): string {
    return this.titleCase(transaction.paymentPurpose.replaceAll('_', ' '));
  }

  protected statusTone(status: string): 'brand' | 'blue' | 'red' | 'neutral' {
    switch (status) {
      case PaymentStatus.Success:
        return 'brand';
      case PaymentStatus.Pending:
      case PaymentStatus.Initiated:
        return 'blue';
      case PaymentStatus.Failed:
      case PaymentStatus.Cancelled:
      case PaymentStatus.Expired:
        return 'red';
      default:
        return 'neutral';
    }
  }

  protected titleCase(value: string): string {
    return value
      .toLowerCase()
      .split(/[\s_]+/)
      .map((part) => part.slice(0, 1).toUpperCase() + part.slice(1))
      .join(' ');
  }

  private paymentStatusSummary(): string {
    if (this.store.failedPayments().length) {
      return 'Needs attention';
    }
    if (this.store.pendingPayments().length) {
      return 'Pending';
    }
    if (this.store.successfulPayments().length) {
      return 'Healthy';
    }
    return 'No records';
  }
}
