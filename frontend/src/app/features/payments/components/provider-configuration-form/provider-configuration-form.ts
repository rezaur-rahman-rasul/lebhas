import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import {
  EnvironmentType,
  PaymentProvider,
  PaymentProviderConfiguration,
  PaymentProviderConfigurationPayload,
} from '../../models/payment.models';

@Component({
  selector: 'app-provider-configuration-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, CardComponent],
  templateUrl: './provider-configuration-form.html',
  styleUrl: './provider-configuration-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderConfigurationFormComponent {
  readonly providers = input<readonly PaymentProvider[]>([]);
  readonly configuration = input<PaymentProviderConfiguration | null>(null);
  readonly loading = input(false);
  readonly submitted = output<PaymentProviderConfigurationPayload>();
  readonly cancelled = output<void>();

  private readonly fb = new FormBuilder();
  protected readonly environmentTypes = Object.values(EnvironmentType);

  protected readonly form = this.fb.nonNullable.group({
    providerId: ['', Validators.required],
    environmentType: [EnvironmentType.Sandbox as EnvironmentType | string, Validators.required],
    apiBaseUrl: [''],
    merchantId: [''],
    successUrl: [''],
    failureUrl: [''],
    cancelUrl: [''],
    apiKey: [''],
    secret: [''],
    webhookSecret: [''],
    isActive: [true],
  });

  constructor() {
    effect(() => {
      const configuration = this.configuration();
      if (!configuration) {
        this.form.reset({
          providerId: '',
          environmentType: EnvironmentType.Sandbox,
          apiBaseUrl: '',
          merchantId: '',
          successUrl: '',
          failureUrl: '',
          cancelUrl: '',
          apiKey: '',
          secret: '',
          webhookSecret: '',
          isActive: true,
        });
        return;
      }

      this.form.reset({
        providerId: configuration.providerId,
        environmentType: configuration.environmentType,
        apiBaseUrl: configuration.apiBaseUrl ?? '',
        merchantId: configuration.merchantId ?? '',
        successUrl: configuration.successUrl ?? '',
        failureUrl: configuration.failureUrl ?? '',
        cancelUrl: configuration.cancelUrl ?? '',
        apiKey: '',
        secret: '',
        webhookSecret: '',
        isActive: configuration.isActive,
      });
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitted.emit({
      providerId: value.providerId,
      environmentType: value.environmentType,
      apiBaseUrl: value.apiBaseUrl || null,
      merchantId: value.merchantId || null,
      successUrl: value.successUrl || null,
      failureUrl: value.failureUrl || null,
      cancelUrl: value.cancelUrl || null,
      isActive: value.isActive,
      secrets: {
        apiKey: value.apiKey || null,
        secret: value.secret || null,
        webhookSecret: value.webhookSecret || null,
      },
    });
  }
}
