import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { RememberedProfile } from '@app/features/auth/models/auth.models';
import { AvatarComponent } from '@app/shared/components/avatar/avatar';
import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PasswordFieldComponent } from '../password-field/password-field';

@Component({
  selector: 'app-remembered-password-dialog',
  standalone: true,
  imports: [ReactiveFormsModule, AvatarComponent, ButtonComponent, IconComponent, PasswordFieldComponent],
  templateUrl: './remembered-password-dialog.html',
  styleUrl: './remembered-password-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RememberedPasswordDialogComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  readonly open = input(false);
  readonly profile = input<RememberedProfile | null>(null);
  readonly loading = input(false);
  readonly errorMessage = input('');
  readonly closed = output<void>();
  readonly forgotPassword = output<void>();
  readonly submitted = output<string>();

  protected readonly attemptedSubmit = signal(false);
  protected readonly form = this.formBuilder.group({
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  constructor() {
    effect(() => {
      const open = this.open();
      const profileId = this.profile()?.id ?? '';

      if (!open || !!profileId) {
        this.form.reset();
        this.attemptedSubmit.set(false);
      }
    }, { allowSignalWrites: true });
  }

  protected submit(): void {
    this.attemptedSubmit.set(true);
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    this.submitted.emit(this.form.controls.password.getRawValue());
  }

  protected close(): void {
    this.form.reset();
    this.attemptedSubmit.set(false);
    this.closed.emit();
  }

  protected passwordError(): string {
    const control = this.form.controls.password;
    if (this.errorMessage()) {
      return this.errorMessage();
    }

    if (!this.attemptedSubmit() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Enter your password to continue.';
    }

    if (control.hasError('minlength')) {
      return 'Password must be at least 8 characters.';
    }

    return '';
  }
}
