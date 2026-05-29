import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { IconComponent } from '@app/shared/components/icon/icon';
import { BillingModalService } from '@app/features/payments/services/billing-modal.service';
import { CreditLedger, CreditTransactionType, WorkspaceUsageSummary } from '../models/usage-billing.models';
import { UsageBillingStore } from '../state/usage-billing.store';

interface CreditStat {
  readonly label: string;
  readonly value: number | null;
  readonly helper: string;
  readonly icon: string;
  readonly tone: 'brand' | 'success' | 'warning' | 'neutral';
}

@Component({
  selector: 'app-usage-billing-dashboard',
  standalone: true,
  imports: [
    DatePipe,
    DecimalPipe,
    ReactiveFormsModule,
    ButtonComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    IconComponent,
  ],
  templateUrl: './usage-billing-dashboard.html',
  styleUrl: './usage-billing-dashboard.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsageBillingDashboardPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(UsageBillingStore);
  protected readonly billingModal = inject(BillingModalService);

  protected readonly activeWorkspaceId = this.workspace.activeWorkspaceId;
  protected readonly filters = new FormGroup({
    type: new FormControl<CreditTransactionType | ''>('', { nonNullable: true }),
    from: new FormControl('', { nonNullable: true }),
    to: new FormControl('', { nonNullable: true }),
    search: new FormControl('', { nonNullable: true }),
  });
  protected readonly transactionTypes = Object.values(CreditTransactionType);
  protected readonly searchTerm = signal('');
  protected readonly selectedType = signal<CreditTransactionType | ''>('');
  protected readonly fromDate = signal('');
  protected readonly toDate = signal('');

  protected readonly accessDenied = computed(() => !this.permissions.canViewUsageBilling());
  protected readonly currentUsage = computed(
    () => this.store.currentMonthUsage() ?? this.store.usageSummary(),
  );
  protected readonly availableCredits = computed(() => {
    const storeCredits = this.store.availableCredits();
    if (typeof storeCredits === 'number') {
      return storeCredits;
    }
    const workspaceCredits = this.workspace.usage()?.creditsRemaining;
    return typeof workspaceCredits === 'number' ? workspaceCredits : null;
  });
  protected readonly usedPercent = computed(() => {
    const usage = this.currentUsage();
    const limit = usage?.creditLimit?.limit ?? null;
    if (!usage || !limit) {
      return 0;
    }
    return Math.min(100, Math.round((usage.usedCredits / limit) * 100));
  });
  protected readonly creditStats = computed<readonly CreditStat[]>(() => {
    const usage = this.currentUsage();
    return [
      { label: 'Available Credits', value: this.availableCredits(), helper: 'Ready to use', icon: 'coins', tone: 'success' },
      { label: 'Reserved Credits', value: usage?.reservedCredits ?? null, helper: 'Held for active jobs', icon: 'lock-keyhole', tone: 'warning' },
      { label: 'Used Credits', value: usage?.usedCredits ?? null, helper: 'Current month spend', icon: 'activity', tone: 'brand' },
      { label: 'Refunded Credits', value: usage?.refundedCredits ?? null, helper: 'Returned after failures', icon: 'rotate-ccw', tone: 'neutral' },
    ];
  });
  protected readonly filteredLedger = computed(() => {
    const type = this.selectedType();
    const search = this.searchTerm().trim().toLowerCase();
    const from = this.fromDate() ? Date.parse(this.fromDate()) : null;
    const to = this.toDate() ? Date.parse(this.toDate()) + 86_399_999 : null;

    return this.store.creditLedger().filter((entry) => {
      const createdAt = Date.parse(entry.createdAt);
      const matchesType = !type || entry.transactionType === type;
      const matchesFrom = from === null || createdAt >= from;
      const matchesTo = to === null || createdAt <= to;
      const haystack = [
        entry.description ?? '',
        entry.referenceType ?? '',
        entry.referenceId ?? '',
        entry.transactionType,
      ].join(' ').toLowerCase();
      return matchesType && matchesFrom && matchesTo && (!search || haystack.includes(search));
    });
  });

  constructor() {
    effect(() => {
      const workspaceId = this.activeWorkspaceId();
      if (!workspaceId || !this.permissions.canViewUsageBilling()) {
        return;
      }

      void this.loadWorkspaceData(workspaceId);
    });

    this.filters.controls.type.valueChanges.subscribe((value) => this.selectedType.set(value));
    this.filters.controls.search.valueChanges.subscribe((value) => this.searchTerm.set(value));
    this.filters.controls.from.valueChanges.subscribe((value) => this.fromDate.set(value));
    this.filters.controls.to.valueChanges.subscribe((value) => this.toDate.set(value));
  }

  protected refresh(): void {
    const workspaceId = this.activeWorkspaceId();
    if (!workspaceId || !this.permissions.canViewUsageBilling()) {
      return;
    }

    void this.loadWorkspaceData(workspaceId);
  }

  protected clearFilters(): void {
    this.filters.reset({ type: '', from: '', to: '', search: '' });
  }

  protected openBillingModal(): void {
    this.billingModal.show();
  }

  protected typeLabel(value: string): string {
    return value.toLowerCase().replaceAll('_', ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  protected ledgerStatus(entry: CreditLedger): string {
    return entry.transactionType === CreditTransactionType.Reserve ? 'Reserved' : 'Posted';
  }

  protected ledgerTone(entry: CreditLedger): 'brand' | 'blue' | 'red' | 'neutral' {
    if (entry.transactionType === CreditTransactionType.Refund) {
      return 'brand';
    }
    if (entry.creditsAmount < 0) {
      return 'red';
    }
    if (entry.transactionType === CreditTransactionType.Reserve) {
      return 'blue';
    }
    return 'neutral';
  }

  private async loadWorkspaceData(workspaceId: string): Promise<void> {
    await this.store.loadDashboard(workspaceId);
    if (this.permissions.canViewCreditLedger()) {
      await this.store.loadCreditLedger(workspaceId);
    }
  }
}

