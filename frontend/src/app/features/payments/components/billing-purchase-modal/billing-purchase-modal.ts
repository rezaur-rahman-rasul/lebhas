import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { IconComponent } from '@app/shared/components/icon/icon';
import { CreditPackage } from '../../models/payment.models';
import { BillingModalService } from '../../services/billing-modal.service';
import { PaymentStore } from '../../state/payment.store';

@Component({
  selector: 'app-billing-purchase-modal',
  standalone: true,
  imports: [DecimalPipe, RouterLink, IconComponent],
  templateUrl: './billing-purchase-modal.html',
  styleUrl: './billing-purchase-modal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BillingPurchaseModalComponent {
  protected readonly modal = inject(BillingModalService);
  protected readonly store = inject(PaymentStore);
  private readonly auth = inject(CurrentUserStore);
  protected readonly permissions = inject(PermissionStore);

  protected readonly selectedPackageId = signal<string | null>(null);
  protected readonly checkoutMessage = signal<string | null>(null);

  protected readonly packages = computed(() => this.store.activeCreditPackages());
  protected readonly selectedPackage = computed(
    () => this.packages().find((item) => item.id === this.selectedPackageId()) ?? this.packages()[0] ?? null,
  );

  constructor() {
    effect(() => {
      if (!this.modal.open() || !this.permissions.canPurchaseCredits()) {
        return;
      }

      void this.store.loadCreditPackages().then(() => {
        if (!this.selectedPackageId() && this.packages()[0]) {
          this.selectedPackageId.set(this.packages()[0].id);
        }
      });
    });
  }

  protected close(): void {
    this.checkoutMessage.set(null);
    this.modal.hide();
  }

  protected selectPackage(item: CreditPackage): void {
    this.selectedPackageId.set(item.id);
    this.checkoutMessage.set(null);
  }

  protected isBestValue(item: CreditPackage): boolean {
    const packages = this.packages();
    if (!packages.length) {
      return false;
    }
    const value = (item.credits + item.bonusCredits) / Math.max(1, item.price);
    return value === Math.max(...packages.map((pkg) => (pkg.credits + pkg.bonusCredits) / Math.max(1, pkg.price)));
  }

  protected async continueCheckout(): Promise<void> {
    const workspaceId = this.auth.activeWorkspaceId();
    const selected = this.selectedPackage();
    if (!workspaceId || !selected) {
      this.checkoutMessage.set('Select a workspace and a credit package before continuing.');
      return;
    }

    const result = await this.store.purchaseCredits(workspaceId, {
      creditPackageId: selected.id,
      returnUrl: window.location.href,
    });
    const session = this.store.activePaymentSession();

    if (result.ok && session?.paymentRedirectUrl) {
      window.open(session.paymentRedirectUrl, '_blank', 'noopener,noreferrer');
      this.close();
      return;
    }

    this.checkoutMessage.set('Payment checkout will be available soon. Contact support or admin.');
  }
}

