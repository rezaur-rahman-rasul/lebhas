import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { ModalComponent } from '@app/shared/components/app-dialog/app-dialog';
import { WorkspaceCreditAdjustmentRequest, WorkspaceCreditAdjustmentType } from '../../models/provider-credit-exchange.models';

@Component({
  selector: 'app-workspace-credit-adjustment-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, ModalComponent],
  templateUrl: './workspace-credit-adjustment-dialog.html',
  styleUrl: './workspace-credit-adjustment-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceCreditAdjustmentDialogComponent {
  readonly open = input(false);
  readonly workspaceName = input('Workspace');
  readonly adjusting = input(false);
  readonly submitted = output<WorkspaceCreditAdjustmentRequest>();
  readonly closed = output<void>();

  protected readonly form = new FormGroup({
    adjustmentType: new FormControl<WorkspaceCreditAdjustmentType>('ADD', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    amount: new FormControl(0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0.0001)],
    }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(1000)],
    }),
    referenceType: new FormControl('MASTER_CREDIT_ADJUSTMENT', {
      nonNullable: true,
      validators: [Validators.maxLength(80)],
    }),
  });

  submit(): void {
    if (this.form.invalid || !confirm('Submit this workspace credit adjustment?')) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const amount = Math.abs(Number(value.amount));
    this.submitted.emit({
      creditsAmount: value.adjustmentType === 'DEDUCT' ? -amount : amount,
      referenceType: value.referenceType || 'MASTER_CREDIT_ADJUSTMENT',
      description: value.description,
    });
  }

  close(): void {
    this.form.reset({
      adjustmentType: 'ADD',
      amount: 0,
      description: '',
      referenceType: 'MASTER_CREDIT_ADJUSTMENT',
    });
    this.closed.emit();
  }
}
