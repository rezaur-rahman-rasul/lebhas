import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { CrewPermission } from '../../models/crew.models';
import { selectionRequiredValidator } from '../../workspace-form.validators';
import { CrewPermissionPanelComponent } from '../crew-permission-panel/crew-permission-panel';

export interface InviteCrewFormValue {
  readonly firstName: string;
  readonly email: string;
  readonly permissions: readonly CrewPermission[];
}

type InviteControlName = 'firstName' | 'email' | 'permissions';

@Component({
  selector: 'app-crew-invite-modal',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    ModalShellComponent,
    CrewPermissionPanelComponent,
  ],
  templateUrl: './crew-invite-modal.html',
  styleUrl: './crew-invite-modal.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CrewInviteModalComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  readonly open = input(false);
  readonly submitting = input(false);
  readonly serverErrors = input<Readonly<Record<string, string>>>({});
  readonly closed = output<void>();
  readonly invited = output<InviteCrewFormValue>();

  protected readonly submitted = signal(false);
  protected readonly form = this.formBuilder.group({
    firstName: ['', [Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    permissions: this.formBuilder.control<readonly CrewPermission[]>(['WORKSPACE_VIEW'], {
      validators: [selectionRequiredValidator()],
    }),
  });

  constructor() {
    effect(() => {
      if (!this.open()) {
        return;
      }

      this.submitted.set(false);
      this.form.reset(
        {
          firstName: '',
          email: '',
          permissions: ['WORKSPACE_VIEW'],
        },
        { emitEvent: false },
      );
    });
  }

  protected submit(): void {
    this.submitted.set(true);
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    const value = this.form.getRawValue();
    this.invited.emit({
      firstName: value.firstName.trim(),
      email: value.email.trim(),
      permissions: value.permissions,
    });
  }

  protected updatePermissions(permissions: readonly CrewPermission[]): void {
    this.form.controls.permissions.setValue(permissions);
    this.form.controls.permissions.markAsTouched();
    this.form.controls.permissions.updateValueAndValidity();
  }

  protected fieldError(controlName: InviteControlName): string {
    const control = this.form.controls[controlName];
    const serverError = this.serverErrors()[controlName];

    if (serverError) {
      return serverError;
    }

    if (!this.submitted() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return controlName === 'permissions'
        ? 'Select at least one permission.'
        : 'This field is required.';
    }

    if (controlName === 'email' && control.hasError('email')) {
      return 'Enter a valid email address.';
    }

    if (control.hasError('maxlength')) {
      return `Keep this under ${control.getError('maxlength').requiredLength} characters.`;
    }

    if (control.hasError('selectionRequired')) {
      return 'Select at least one permission.';
    }

    return '';
  }

  protected inputClasses(): string {
    return 'h-11 w-full rounded-md border border-border bg-white px-3 text-sm text-ink shadow-sm outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
  }
}
