import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { PaymentEmptyStateComponent } from '../components/payment-empty-state/payment-empty-state';
import { PaymentLoadingStateComponent } from '../components/payment-loading-state/payment-loading-state';
import { PaymentTransactionCardComponent } from '../components/payment-transaction-card/payment-transaction-card';
import { PaymentPurpose, PaymentStatus, PaymentTransaction } from '../models/payment.models';
import { PaymentStore } from '../state/payment.store';

@Component({
  selector: 'app-payment-transactions-page',
  standalone: true,
  imports: [
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    PaymentEmptyStateComponent,
    PaymentLoadingStateComponent,
    PaymentTransactionCardComponent,
  ],
  templateUrl: './payment-transactions.html',
  styleUrl: './payment-transactions.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentTransactionsPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(PaymentStore);
  private readonly route = inject(ActivatedRoute);

  protected readonly selectedStatus = signal<string | null>(null);
  protected readonly selectedPurpose = signal<string | null>(null);
  protected readonly fromDate = signal<string | null>(null);
  protected readonly toDate = signal<string | null>(null);

  protected readonly isMasterRoute = this.route.snapshot.data['scope'] === 'master';
  protected readonly accessDenied = computed(() => !this.permissions.canViewPayments());
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly statusOptions = Object.values(PaymentStatus);
  protected readonly purposeOptions = Object.values(PaymentPurpose);
  protected readonly transactions = this.store.paymentTransactions;
  protected readonly filteredTransactions = computed(() =>
    this.transactions().filter((transaction) => this.matchesFilters(transaction)),
  );
  protected readonly hasTransactions = computed(() => this.filteredTransactions().length > 0);
  protected readonly summaryCards = computed(() => [
    {
      key: 'total',
      label: 'Total payments',
      value: this.filteredTransactions().length,
      description: 'Shown after filters are applied.',
    },
    {
      key: 'successful',
      label: 'Successful payments',
      value: this.store.successfulPayments().length,
      description: 'Backend-confirmed successful payments.',
    },
    {
      key: 'pending-failed',
      label: 'Pending or failed',
      value: this.store.pendingPayments().length + this.store.failedPayments().length,
      description: 'Payments waiting for confirmation or requiring attention.',
    },
  ]);
  protected readonly totalAmount = computed(() =>
    this.filteredTransactions().reduce((total, transaction) => total + transaction.amount, 0),
  );
  protected readonly currencyLabel = computed(
    () => this.filteredTransactions()[0]?.currency ?? this.transactions()[0]?.currency ?? '',
  );

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      void this.workspace.initialize();

      const workspaceId = this.workspaceId();
      if (!workspaceId || this.isMasterRoute) {
        return;
      }

      void this.store.loadWorkspacePayments(workspaceId);
    });
  }

  protected refresh(): void {
    if (this.accessDenied()) {
      return;
    }

    this.store.clearError();
    const workspaceId = this.workspaceId();
    if (workspaceId && !this.isMasterRoute) {
      void this.store.loadWorkspacePayments(workspaceId);
    }
  }

  protected clearFilters(): void {
    this.selectedStatus.set(null);
    this.selectedPurpose.set(null);
    this.fromDate.set(null);
    this.toDate.set(null);
  }

  protected updateStatus(event: Event): void {
    this.selectedStatus.set(selectValue(event));
  }

  protected updatePurpose(event: Event): void {
    this.selectedPurpose.set(selectValue(event));
  }

  protected updateFromDate(event: Event): void {
    this.fromDate.set(selectValue(event));
  }

  protected updateToDate(event: Event): void {
    this.toDate.set(selectValue(event));
  }

  private matchesFilters(transaction: PaymentTransaction): boolean {
    if (this.selectedStatus() && transaction.status !== this.selectedStatus()) {
      return false;
    }

    if (this.selectedPurpose() && transaction.paymentPurpose !== this.selectedPurpose()) {
      return false;
    }

    const createdAt = Date.parse(transaction.initiatedAt ?? transaction.createdAt);
    const from = this.fromDate() ? Date.parse(`${this.fromDate()}T00:00:00`) : null;
    const to = this.toDate() ? Date.parse(`${this.toDate()}T23:59:59`) : null;

    if (from && createdAt < from) {
      return false;
    }

    return !(to && createdAt > to);
  }
}

function selectValue(event: Event): string | null {
  const value = (event.target as HTMLSelectElement | HTMLInputElement).value;
  return value || null;
}
