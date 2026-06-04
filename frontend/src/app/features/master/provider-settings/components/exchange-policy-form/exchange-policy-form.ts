import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { ProviderCreditExchangePolicyView } from '../../models/provider-credit-exchange.models';

@Component({
  selector: 'app-exchange-policy-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent],
  templateUrl: './exchange-policy-form.html',
  styleUrl: './exchange-policy-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExchangePolicyFormComponent {
  readonly providerId = input.required<string>();
  readonly policy = input<ProviderCreditExchangePolicyView | null>(null);
  readonly saving = input(false);
  readonly saved = output<ProviderCreditExchangePolicyView>();

  protected readonly form = new FormGroup({
    internalCreditPerProviderUnit: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    freeSignupCreditEnabled: new FormControl(true, { nonNullable: true }),
    freeSignupCreditPercentage: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0), Validators.max(100)] }),
    maxFreeSignupCredits: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    minProviderBalanceRequired: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    fallbackFreeCredits: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    active: new FormControl(true, { nonNullable: true }),
  });

  constructor() {
    effect(() => {
      const policy = this.policy();
      if (policy) {
        this.form.patchValue(policy, { emitEvent: false });
      }
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saved.emit({ providerId: this.providerId(), updatedAt: this.policy()?.updatedAt ?? '', ...this.form.getRawValue() });
  }
}
