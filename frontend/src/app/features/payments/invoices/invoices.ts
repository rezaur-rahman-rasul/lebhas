import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { InvoiceCardComponent } from '../components/invoice-card/invoice-card';
import { PaymentEmptyStateComponent } from '../components/payment-empty-state/payment-empty-state';
import { PaymentLoadingStateComponent } from '../components/payment-loading-state/payment-loading-state';
import { Invoice, InvoiceType, PaymentStatus } from '../models/payment.models';
import { PaymentStore } from '../state/payment.store';

@Component({
  selector: 'app-invoices-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    InvoiceCardComponent,
    PaymentEmptyStateComponent,
    PaymentLoadingStateComponent,
  ],
  templateUrl: './invoices.html',
  styleUrl: './invoices.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InvoicesPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(PaymentStore);

  protected readonly selectedStatus = signal<string | null>(null);
  protected readonly selectedType = signal<string | null>(null);
  protected readonly fromDate = signal<string | null>(null);
  protected readonly toDate = signal<string | null>(null);

  protected readonly accessDenied = computed(() => !this.permissions.canViewInvoices());
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly invoices = this.store.invoices;
  protected readonly statusOptions = Object.values(PaymentStatus);
  protected readonly typeOptions = Object.values(InvoiceType);
  protected readonly filteredInvoices = computed(() =>
    this.invoices().filter((invoice) => this.matchesFilters(invoice)),
  );
  protected readonly hasInvoices = computed(() => this.filteredInvoices().length > 0);
  protected readonly totalAmount = computed(() =>
    this.filteredInvoices().reduce((total, invoice) => total + invoice.amount, 0),
  );
  protected readonly currencyLabel = computed(
    () => this.filteredInvoices()[0]?.currency ?? this.invoices()[0]?.currency ?? '',
  );

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      void this.workspace.initialize();

      const workspaceId = this.workspaceId();
      if (!workspaceId) {
        return;
      }

      void this.store.loadWorkspaceInvoices(workspaceId);
    });
  }

  protected refresh(): void {
    if (this.accessDenied()) {
      return;
    }

    this.store.clearError();
    const workspaceId = this.workspaceId();
    if (workspaceId) {
      void this.store.loadWorkspaceInvoices(workspaceId);
    }
  }

  protected clearFilters(): void {
    this.selectedStatus.set(null);
    this.selectedType.set(null);
    this.fromDate.set(null);
    this.toDate.set(null);
  }

  protected updateStatus(event: Event): void {
    this.selectedStatus.set(selectValue(event));
  }

  protected updateType(event: Event): void {
    this.selectedType.set(selectValue(event));
  }

  protected updateFromDate(event: Event): void {
    this.fromDate.set(selectValue(event));
  }

  protected updateToDate(event: Event): void {
    this.toDate.set(selectValue(event));
  }

  private matchesFilters(invoice: Invoice): boolean {
    if (this.selectedStatus() && invoice.status !== this.selectedStatus()) {
      return false;
    }

    if (this.selectedType() && invoice.invoiceType !== this.selectedType()) {
      return false;
    }

    const createdAt = Date.parse(invoice.issuedAt ?? invoice.createdAt);
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
