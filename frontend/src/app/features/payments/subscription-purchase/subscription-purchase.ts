import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BillingCycleToggleComponent } from '../components/billing-cycle-toggle/billing-cycle-toggle';
import { PaymentEmptyStateComponent } from '../components/payment-empty-state/payment-empty-state';
import { PaymentLoadingStateComponent } from '../components/payment-loading-state/payment-loading-state';
import { PaymentStatusBadgeComponent } from '../components/payment-status-badge/payment-status-badge';
import { SubscriptionPlanCardComponent } from '../components/subscription-plan-card/subscription-plan-card';
import {
  BillingCycle,
  CurrentSubscription,
  PricingPlanForPurchase,
  SubscriptionPurchasePayload,
} from '../models/payment.models';
import { PaymentStore } from '../state/payment.store';

type SubscriptionAction = 'purchase' | 'upgrade' | 'renew';

@Component({
  selector: 'app-subscription-purchase-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BillingCycleToggleComponent,
    PaymentEmptyStateComponent,
    PaymentLoadingStateComponent,
    PaymentStatusBadgeComponent,
    SubscriptionPlanCardComponent,
  ],
  templateUrl: './subscription-purchase.html',
  styleUrl: './subscription-purchase.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SubscriptionPurchasePage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly store = inject(PaymentStore);

  protected readonly selectedAction = signal<SubscriptionAction>('purchase');

  protected readonly accessDenied = computed(() => !this.permissions.canPurchaseSubscription());
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly plans = this.store.pricingPlansForPurchase;
  protected readonly hasPlans = computed(() => this.plans().length > 0);
  protected readonly selectedPlan = this.store.selectedPricingPlan;
  protected readonly selectedCycle = computed(
    () => this.store.selectedBillingCycle() ?? this.selectedPlan()?.billingCycle ?? BillingCycle.Monthly,
  );
  protected readonly selectedSummary = this.store.selectedSubscriptionSummary;
  protected readonly paymentSession = this.store.activePaymentSession;
  protected readonly currentSubscription = computed<CurrentSubscription | null>(() => {
    const storeSubscription = this.store.currentSubscription();
    if (storeSubscription) {
      return storeSubscription;
    }

    const workspaceSubscription = this.workspace.subscription();
    if (!workspaceSubscription) {
      return null;
    }

    return {
      id: workspaceSubscription.id ?? '',
      workspaceId: this.workspaceId() ?? '',
      pricingPlanId: workspaceSubscription.planId ?? '',
      pricingPlanName: workspaceSubscription.planName ?? 'Package details unavailable',
      billingCycle: workspaceSubscription.billingCycle ?? 'Unknown',
      status: workspaceSubscription.status ?? 'Unknown',
      startsAt: null,
      expiresAt: workspaceSubscription.currentPeriodEnd ?? workspaceSubscription.trialEndsAt ?? null,
      renewsAt: workspaceSubscription.currentPeriodEnd ?? null,
    };
  });

  protected readonly recommendedAction = computed<SubscriptionAction>(() => {
    const subscription = this.currentSubscription();
    const status = subscription?.status?.toLowerCase();

    if (!subscription?.id) {
      return 'purchase';
    }

    return status === 'expired' || status === 'cancelled' ? 'renew' : 'upgrade';
  });

  protected readonly canStartPayment = computed(
    () => Boolean(this.workspaceId() && this.selectedPlan()) && !this.store.loading(),
  );

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      this.selectedAction.set(this.recommendedAction());
    });

    effect(() => {
      if (!this.accessDenied()) {
        void this.workspace.initialize();
      }
    });
  }

  protected selectPlan(plan: PricingPlanForPurchase): void {
    this.store.clearActivePaymentSession();
    this.store.setSelectedPricingPlan(plan);
  }

  protected setBillingCycle(cycle: BillingCycle): void {
    this.store.clearActivePaymentSession();
    this.store.setSelectedBillingCycle(cycle);
  }

  protected setAction(action: SubscriptionAction): void {
    this.store.clearActivePaymentSession();
    this.selectedAction.set(action);
  }

  protected retry(): void {
    this.store.clearError();
    void this.workspace.initialize();
  }

  protected async startPayment(): Promise<void> {
    const workspaceId = this.workspaceId();
    const plan = this.selectedPlan();
    const billingCycle = this.selectedCycle();

    if (!workspaceId || !plan || !billingCycle) {
      return;
    }

    const payload: SubscriptionPurchasePayload = {
      pricingPlanId: plan.id,
      billingCycle,
      returnUrl: typeof window === 'undefined' ? null : window.location.href,
    };

    const result =
      this.selectedAction() === 'renew'
        ? await this.store.renewSubscription(workspaceId, payload)
        : this.selectedAction() === 'upgrade'
          ? await this.store.upgradeSubscription(workspaceId, payload)
          : await this.store.purchaseSubscription(workspaceId, payload);

    if (!result.ok) {
      return;
    }
  }

  protected continueToPayment(): void {
    const redirectUrl = this.paymentSession()?.paymentRedirectUrl;
    if (!redirectUrl) {
      return;
    }

    window.location.assign(redirectUrl);
  }

  protected actionLabel(action: SubscriptionAction): string {
    switch (action) {
      case 'renew':
        return 'Renew subscription';
      case 'upgrade':
        return 'Upgrade subscription';
      default:
        return 'Start subscription';
    }
  }
}
