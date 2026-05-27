import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { AppDrawerComponent } from '@app/shared/components/app-drawer/app-drawer';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { CreditPackageCardComponent } from '../components/credit-package-card/credit-package-card';
import { CreditPackageFormComponent } from '../components/credit-package-form/credit-package-form';
import { PaymentEmptyStateComponent } from '../components/payment-empty-state/payment-empty-state';
import { PaymentLoadingStateComponent } from '../components/payment-loading-state/payment-loading-state';
import { CreditPackage, CreditPackagePayload } from '../models/payment.models';
import { PaymentStore } from '../state/payment.store';

type CreditPackageDrawerMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-credit-packages-page',
  standalone: true,
  imports: [
    ButtonComponent,
    CardComponent,
    AppDrawerComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    CreditPackageCardComponent,
    CreditPackageFormComponent,
    PaymentEmptyStateComponent,
    PaymentLoadingStateComponent,
  ],
  templateUrl: './credit-packages.html',
  styleUrl: './credit-packages.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditPackagesPage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(PaymentStore);

  protected readonly drawerMode = signal<CreditPackageDrawerMode>(null);
  protected readonly selectedCreditPackage = signal<CreditPackage | null>(null);

  protected readonly accessDenied = computed(() => !this.permissions.canManageCreditPackages());
  protected readonly creditPackages = computed(() =>
    [...this.store.creditPackages()].sort((a, b) => a.sortOrder - b.sortOrder),
  );
  protected readonly hasCreditPackages = computed(() => this.creditPackages().length > 0);
  protected readonly inactiveCreditPackages = computed(() =>
    this.store.creditPackages().filter((creditPackage) => !creditPackage.isActive),
  );
  protected readonly drawerOpen = computed(() => this.drawerMode() !== null);
  protected readonly drawerTitle = computed(() =>
    this.drawerMode() === 'edit' ? 'Edit credit package' : 'Create credit package',
  );

  constructor() {
    effect(() => {
      if (this.permissions.canManageCreditPackages()) {
        void this.store.loadCreditPackages();
      }
    });
  }

  protected refresh(): void {
    if (!this.permissions.canManageCreditPackages()) {
      return;
    }

    void this.store.loadCreditPackages();
  }

  protected openCreate(): void {
    this.selectedCreditPackage.set(null);
    this.drawerMode.set('create');
  }

  protected openEdit(creditPackage: CreditPackage): void {
    this.selectedCreditPackage.set(creditPackage);
    this.drawerMode.set('edit');
  }

  protected closeDrawer(): void {
    this.drawerMode.set(null);
    this.selectedCreditPackage.set(null);
  }

  protected async saveCreditPackage(payload: CreditPackagePayload): Promise<void> {
    const creditPackage = this.selectedCreditPackage();
    const result = creditPackage
      ? await this.store.updateCreditPackage(creditPackage.id, payload)
      : await this.store.createCreditPackage(payload);

    if (result.ok) {
      this.closeDrawer();
    }
  }

  protected async toggleCreditPackage(creditPackage: CreditPackage): Promise<void> {
    await this.store.updateCreditPackage(creditPackage.id, {
      name: creditPackage.name,
      code: creditPackage.code,
      credits: creditPackage.credits,
      bonusCredits: creditPackage.bonusCredits,
      price: creditPackage.price,
      currency: creditPackage.currency,
      isActive: !creditPackage.isActive,
      sortOrder: creditPackage.sortOrder,
    });
  }
}
