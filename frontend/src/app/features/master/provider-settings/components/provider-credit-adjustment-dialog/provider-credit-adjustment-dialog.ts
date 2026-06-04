import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { ModalComponent } from '@app/shared/components/app-dialog/app-dialog';
import { ProviderCreditAdjustmentRequest, ProviderCreditAdjustmentType } from '../../models/provider-credit-exchange.models';

@Component({
  selector: 'app-provider-credit-adjustment-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, ModalComponent],
  templateUrl: './provider-credit-adjustment-dialog.html',
  styleUrl: './provider-credit-adjustment-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderCreditAdjustmentDialogComponent {
  readonly open = input(false);
  readonly providerName = input('Provider');
  readonly adjusting = input(false);
  readonly submitted = output<ProviderCreditAdjustmentRequest>();
  readonly closed = output<void>();

  protected readonly form = new FormGroup({
    adjustmentType: new FormControl<ProviderCreditAdjustmentType>('ADD', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0.0001)] }),
    reason: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    note: new FormControl('', { nonNullable: true }),
  });

  submit(): void {
    if (this.form.invalid || !confirm('Submit this provider credit adjustment?')) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitted.emit(this.form.getRawValue());
  }

  close(): void {
    this.form.reset({ adjustmentType: 'ADD', amount: 0, reason: '', note: '' });
    this.closed.emit();
  }
}
