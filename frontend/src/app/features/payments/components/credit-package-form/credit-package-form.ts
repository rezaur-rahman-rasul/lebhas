import { ChangeDetectionStrategy, Component, computed, effect, input, output } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { CreditPackage, CreditPackagePayload } from '../../models/payment.models';

@Component({
  selector: 'app-credit-package-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, CardComponent],
  templateUrl: './credit-package-form.html',
  styleUrl: './credit-package-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditPackageFormComponent {
  readonly creditPackage = input<CreditPackage | null>(null);
  readonly loading = input(false);
  readonly submitted = output<CreditPackagePayload>();
  readonly cancelled = output<void>();

  private readonly fb = new FormBuilder();
  protected readonly submitLabel = computed(() => (this.creditPackage() ? 'Update package' : 'Create package'));

  protected readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    code: ['', [Validators.required, Validators.maxLength(80)]],
    credits: [0, [Validators.required, Validators.min(0)]],
    bonusCredits: [0, [Validators.required, Validators.min(0)]],
    price: [0, [Validators.required, Validators.min(0)]],
    currency: ['', [Validators.required, Validators.maxLength(12)]],
    isActive: [true],
    sortOrder: [1, [Validators.required, Validators.min(0)]],
  });

  constructor() {
    effect(() => {
      const creditPackage = this.creditPackage();
      if (!creditPackage) {
        this.form.reset({
          name: '',
          code: '',
          credits: 0,
          bonusCredits: 0,
          price: 0,
          currency: '',
          isActive: true,
          sortOrder: 1,
        });
        return;
      }

      this.form.reset({
        name: creditPackage.name,
        code: creditPackage.code,
        credits: creditPackage.credits,
        bonusCredits: creditPackage.bonusCredits,
        price: creditPackage.price,
        currency: creditPackage.currency,
        isActive: creditPackage.isActive,
        sortOrder: creditPackage.sortOrder,
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
