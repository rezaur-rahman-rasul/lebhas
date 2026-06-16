import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ProviderCreditsStore } from '../../state/providerCredits.store';

type CreditAdjustmentMode = 'ADD' | 'DEDUCT';
type CreditAdjustmentReason = 'REFUND' | 'ACCIDENTAL_INCREASE' | 'SPECIAL_CASE' | 'MANUAL_CORRECTION';

@Component({
  selector: 'app-workspace-credit-adjustment-page',
  standalone: true,
  imports: [DecimalPipe, ReactiveFormsModule, ButtonComponent, PageHeaderComponent],
  templateUrl: './workspace-credit-adjustment-page.html',
  styleUrl: './workspace-credit-adjustment-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceCreditAdjustmentPage {
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly credits = inject(ProviderCreditsStore);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly reasonOptions: readonly { value: CreditAdjustmentReason; label: string }[] = [
    { value: 'REFUND', label: 'Refund' },
    { value: 'ACCIDENTAL_INCREASE', label: 'Accidental increase correction' },
    { value: 'SPECIAL_CASE', label: 'Special case' },
    { value: 'MANUAL_CORRECTION', label: 'Manual correction' },
  ];

  protected readonly form = new FormGroup({
    workspaceId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    mode: new FormControl<CreditAdjustmentMode>('ADD', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl(0, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(0.0001)],
    }),
    reason: new FormControl<CreditAdjustmentReason>('SPECIAL_CASE', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    note: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(900)],
    }),
  });

  protected selectedWorkspace() {
    const workspaceId = this.form.controls.workspaceId.value;
    return this.workspace.workspaces().find((item) => item.id === workspaceId) ?? null;
  }

  protected signedAmount(): number {
    const amount = Math.abs(Number(this.form.controls.amount.value) || 0);
    return this.form.controls.mode.value === 'DEDUCT' ? -amount : amount;
  }

  constructor() {
    void this.workspace.initialize();

    effect(() => {
      const selectedId = this.form.controls.workspaceId.value;
      const activeId = this.workspace.activeWorkspaceId();
      const fallbackId = activeId ?? this.workspace.workspaces()[0]?.id ?? '';

      if (!selectedId && fallbackId) {
        this.form.controls.workspaceId.setValue(fallbackId);
        void this.credits.loadWorkspaceCredits(fallbackId);
      } else if (selectedId) {
        void this.credits.loadWorkspaceCredits(selectedId);
      }
    });

    this.form.controls.workspaceId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((workspaceId) => {
        if (workspaceId) {
          void this.credits.loadWorkspaceCredits(workspaceId);
        }
      });
  }

  protected projectedAvailableCredits(): number | null {
    const current = this.credits.workspaceCreditAccount()?.availableCredits;
    return typeof current === 'number' ? current + this.signedAmount() : null;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const workspaceName = this.selectedWorkspace()?.name ?? value.workspaceId;
    const action = value.mode === 'DEDUCT' ? 'deduct' : 'add';

    if (!confirm(`Confirm ${action} ${Math.abs(this.signedAmount())} credits for ${workspaceName}?`)) {
      return;
    }

    void this.credits.adjustWorkspaceCredits(value.workspaceId, {
      creditsAmount: this.signedAmount(),
      referenceType: value.reason,
      description: `${this.reasonLabel(value.reason)}: ${value.note}`,
    }).then((ok) => {
      if (ok) {
        this.form.patchValue({ amount: 0, note: '' });
      }
    });
  }

  private reasonLabel(value: CreditAdjustmentReason): string {
    return this.reasonOptions.find((item) => item.value === value)?.label ?? 'Manual adjustment';
  }
}
