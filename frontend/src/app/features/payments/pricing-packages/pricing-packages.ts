import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { AppDrawerComponent } from '@app/shared/components/app-drawer/app-drawer';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { PricingPlanDetail } from '../models/payment.models';
import { PricingPlanStore } from '../state/pricing-plan.store';

type PricingDrawerMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-pricing-packages-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    AppDrawerComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
  ],
  templateUrl: './pricing-packages.html',
  styleUrl: './pricing-packages.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PricingPackagesPage {
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(PricingPlanStore);

  protected readonly drawerMode = signal<PricingDrawerMode>(null);
  protected readonly selectedPlan = signal<PricingPlanDetail | null>(null);

  protected readonly accessDenied = computed(() => this.permissions.role() !== 'MASTER');
  protected readonly plans = computed(() =>
    [...this.store.pricingPlans()].sort(
      (left, right) => left.pricingPlan.sortOrder - right.pricingPlan.sortOrder || left.pricingPlan.name.localeCompare(right.pricingPlan.name),
    ),
  );
  protected readonly activePlans = computed(() => this.plans().filter((plan) => plan.pricingPlan.active));
  protected readonly inactivePlans = computed(() => this.plans().filter((plan) => !plan.pricingPlan.active));
  protected readonly drawerOpen = computed(() => this.drawerMode() !== null);
  protected readonly drawerTitle = computed(() =>
    this.drawerMode() === 'edit' ? 'Edit pricing package' : 'Create pricing package',
  );

  protected readonly form = this.formBuilder.group({
    name: ['', Validators.required],
    code: ['', Validators.required],
    description: [''],
    monthlyPrice: [0, [Validators.required, Validators.min(0)]],
    yearlyPrice: [0, [Validators.required, Validators.min(0)]],
    currency: ['USD', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    billingCycle: ['MONTHLY'],
    active: [true],
    defaultPlan: [false],
    sortOrder: [0, [Validators.required, Validators.min(0)]],
    monthlyCreditLimit: [0, [Validators.required, Validators.min(0)]],
    maxBrands: [0, [Validators.required, Validators.min(0)]],
    maxProductServices: [0, [Validators.required, Validators.min(0)]],
    maxProjects: [0, [Validators.required, Validators.min(0)]],
    maxAssets: [0, [Validators.required, Validators.min(0)]],
    maxGeneratedVersionsPerRequest: [0, [Validators.required, Validators.min(0)]],
    allowApprovalWorkflow: [true],
    allowPublicShareLinks: [true],
    allowVideoGeneration: [false],
    allowAdvancedPromptIntelligence: [true],
    allowTeamCollaboration: [true],
    allowExportWithoutWatermark: [false],
  });

  constructor() {
    effect(() => {
      if (!this.accessDenied()) {
        void this.store.load();
      }
    });
  }

  protected refresh(): void {
    if (!this.accessDenied()) {
      void this.store.load();
    }
  }

  protected openCreate(): void {
    this.selectedPlan.set(null);
    this.form.reset({
      name: '',
      code: '',
      description: '',
      monthlyPrice: 0,
      yearlyPrice: 0,
      currency: 'USD',
      billingCycle: 'MONTHLY',
      active: true,
      defaultPlan: false,
      sortOrder: this.plans().length,
      monthlyCreditLimit: 0,
      maxBrands: 0,
      maxProductServices: 0,
      maxProjects: 0,
      maxAssets: 0,
      maxGeneratedVersionsPerRequest: 0,
      allowApprovalWorkflow: true,
      allowPublicShareLinks: true,
      allowVideoGeneration: false,
      allowAdvancedPromptIntelligence: true,
      allowTeamCollaboration: true,
      allowExportWithoutWatermark: false,
    });
    this.drawerMode.set('create');
  }

  protected openEdit(plan: PricingPlanDetail): void {
    const pricingPlan = plan.pricingPlan;
    const policy = plan.featurePolicy;
    this.selectedPlan.set(plan);
    this.form.reset({
      name: pricingPlan.name,
      code: pricingPlan.code,
      description: pricingPlan.description ?? '',
      monthlyPrice: pricingPlan.monthlyPrice,
      yearlyPrice: pricingPlan.yearlyPrice,
      currency: pricingPlan.currency,
      billingCycle: 'MONTHLY',
      active: pricingPlan.active,
      defaultPlan: pricingPlan.defaultPlan,
      sortOrder: pricingPlan.sortOrder,
      monthlyCreditLimit: policy?.monthlyCreditLimit ?? 0,
      maxBrands: policy?.maxBrands ?? 0,
      maxProductServices: policy?.maxProductServices ?? 0,
      maxProjects: policy?.maxProjects ?? 0,
      maxAssets: policy?.maxStorageGb ?? 0,
      maxGeneratedVersionsPerRequest: policy?.maxGeneratedVersionsPerRequest ?? 0,
      allowApprovalWorkflow: policy?.allowApprovalWorkflow ?? false,
      allowPublicShareLinks: policy?.allowPublicShareLinks ?? false,
      allowVideoGeneration: policy?.allowVideoGeneration ?? false,
      allowAdvancedPromptIntelligence: policy?.allowAdvancedPromptIntelligence ?? false,
      allowTeamCollaboration: policy?.allowTeamCollaboration ?? false,
      allowExportWithoutWatermark: policy?.allowExportWithoutWatermark ?? false,
    });
    this.drawerMode.set('edit');
  }

  protected closeDrawer(): void {
    this.drawerMode.set(null);
    this.selectedPlan.set(null);
  }

  protected async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload = {
      name: value.name,
      code: value.code,
      description: value.description || null,
      monthlyPrice: value.monthlyPrice,
      yearlyPrice: value.yearlyPrice,
      currency: value.currency.toUpperCase(),
      defaultPlan: value.defaultPlan,
      active: value.active,
      sortOrder: value.sortOrder,
    };
    const policy = {
      monthlyCreditLimit: value.monthlyCreditLimit,
      maxBrands: value.maxBrands,
      maxProductServices: value.maxProductServices,
      maxProjects: value.maxProjects,
      maxTeamMembers: value.maxAssets,
      maxStorageGb: value.maxAssets,
      maxGeneratedVersionsPerRequest: value.maxGeneratedVersionsPerRequest,
      allowApprovalWorkflow: value.allowApprovalWorkflow,
      allowPublicShareLinks: value.allowPublicShareLinks,
      allowVideoGeneration: value.allowVideoGeneration,
      allowAdvancedPromptIntelligence: value.allowAdvancedPromptIntelligence,
      allowTeamCollaboration: value.allowTeamCollaboration,
      allowExportWithoutWatermark: value.allowExportWithoutWatermark,
    };

    const plan = this.selectedPlan();
    const result = plan
      ? await this.store.update(plan.pricingPlan.id, payload, policy)
      : await this.store.create(payload, policy);

    if (result.ok) {
      this.closeDrawer();
    }
  }

  protected async disable(plan: PricingPlanDetail): Promise<void> {
    await this.store.disable(plan.pricingPlan.id);
  }

  protected planPrice(plan: PricingPlanDetail): number {
    return this.form.controls.billingCycle.value === 'YEARLY'
      ? plan.pricingPlan.yearlyPrice
      : plan.pricingPlan.monthlyPrice;
  }

  protected limitsSummary(plan: PricingPlanDetail): string {
    const policy = plan.featurePolicy;
    if (!policy) {
      return 'Limits not configured';
    }

    return `${policy.monthlyCreditLimit ?? 0} credits, ${policy.maxBrands ?? 0} brands, ${policy.maxProductServices ?? 0} products, ${policy.maxProjects ?? 0} projects`;
  }
}
