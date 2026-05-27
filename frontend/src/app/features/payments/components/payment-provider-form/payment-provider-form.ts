import { ChangeDetectionStrategy, Component, computed, effect, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import {
  PaymentProvider,
  PaymentProviderPayload,
  PaymentProviderType,
} from '../../models/payment.models';

@Component({
  selector: 'app-payment-provider-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, CardComponent],
  templateUrl: './payment-provider-form.html',
  styleUrl: './payment-provider-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentProviderFormComponent {
  readonly provider = input<PaymentProvider | null>(null);
  readonly loading = input(false);
  readonly submitted = output<PaymentProviderPayload>();
  readonly cancelled = output<void>();

  private readonly fb = new FormBuilder();
  protected readonly providerTypes = Object.values(PaymentProviderType);
  protected readonly submitLabel = computed(() => (this.provider() ? 'Update provider' : 'Create provider'));

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    code: ['', [Validators.required, Validators.maxLength(80)]],
    providerType: [PaymentProviderType.Sslcommerz as PaymentProviderType | string, Validators.required],
    isEnabled: [true],
    sandboxEnabled: [true],
    liveEnabled: [false],
    priority: [1, [Validators.required, Validators.min(0)]],
  });

  constructor() {
    effect(() => {
      const provider = this.provider();
      if (!provider) {
        this.form.reset({
          name: '',
          code: '',
          providerType: PaymentProviderType.Sslcommerz,
          isEnabled: true,
          sandboxEnabled: true,
          liveEnabled: false,
          priority: 1,
        });
        return;
      }

      this.form.reset({
        name: provider.name,
        code: provider.code,
        providerType: provider.providerType,
        isEnabled: provider.isEnabled,
        sandboxEnabled: provider.sandboxEnabled,
        liveEnabled: provider.liveEnabled,
        priority: provider.priority,
      });
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitted.emit(this.form.getRawValue());
  }
}
