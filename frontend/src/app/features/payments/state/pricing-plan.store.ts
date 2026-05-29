import { Injectable, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import {
  PaymentActionResult,
  PlanFeaturePolicyPayload,
  PricingPlanDetail,
  PricingPlanPayload,
} from '../models/payment.models';
import { PaymentApiService } from '../services/payment-api.service';

@Injectable({ providedIn: 'root' })
export class PricingPlanStore {
  private readonly api = inject(PaymentApiService);

  private readonly pricingPlansSignal = signal<readonly PricingPlanDetail[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly pricingPlans = this.pricingPlansSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  async load(): Promise<PaymentActionResult> {
    return this.run(async () => {
      this.pricingPlansSignal.set(await this.api.getPricingPlans());
    });
  }

  async create(
    payload: PricingPlanPayload,
    policy: PlanFeaturePolicyPayload,
  ): Promise<PaymentActionResult> {
    return this.run(async () => {
      let detail = await this.api.createPricingPlan(payload);
      detail = await this.api.updatePlanFeaturePolicy(detail.pricingPlan.id, policy);
      this.pricingPlansSignal.update((plans) => upsertPlan(plans, detail));
    });
  }

  async update(
    pricingPlanId: string,
    payload: PricingPlanPayload,
    policy: PlanFeaturePolicyPayload,
  ): Promise<PaymentActionResult> {
    return this.run(async () => {
      let detail = await this.api.updatePricingPlan(pricingPlanId, payload);
      detail = await this.api.updatePlanFeaturePolicy(pricingPlanId, policy);
      this.pricingPlansSignal.update((plans) => upsertPlan(plans, detail));
    });
  }

  async disable(pricingPlanId: string): Promise<PaymentActionResult> {
    return this.run(async () => {
      const detail = await this.api.disablePricingPlan(pricingPlanId);
      this.pricingPlansSignal.update((plans) => upsertPlan(plans, detail));
    });
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  private async run(action: () => Promise<void>): Promise<PaymentActionResult> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await action();
      return { ok: true };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.errorSignal.set(normalized.message || 'Pricing packages could not load. Please try again.');
      return { ok: false, message: this.errorSignal() ?? undefined };
    } finally {
      this.loadingSignal.set(false);
    }
  }
}

function upsertPlan(
  plans: readonly PricingPlanDetail[],
  detail: PricingPlanDetail,
): readonly PricingPlanDetail[] {
  const index = plans.findIndex((plan) => plan.pricingPlan.id === detail.pricingPlan.id);
  if (index < 0) {
    return [...plans, detail].sort(sortPlanDetails);
  }

  return plans
    .map((plan, currentIndex) => (currentIndex === index ? detail : plan))
    .sort(sortPlanDetails);
}

function sortPlanDetails(left: PricingPlanDetail, right: PricingPlanDetail): number {
  return left.pricingPlan.sortOrder - right.pricingPlan.sortOrder || left.pricingPlan.name.localeCompare(right.pricingPlan.name);
}
