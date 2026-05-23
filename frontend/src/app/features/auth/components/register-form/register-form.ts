import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/button/button';
import { InputComponent } from '@app/shared/components/input/input';
import { PasswordInputComponent } from '@app/shared/components/password-input/password-input';
import { matchingFieldsValidator } from '@app/shared/validators/matching-fields.validator';

export interface RegisterFormValue {
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly phone: string | null;
  readonly password: string;
}

@Component({
  selector: 'app-register-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, InputComponent, PasswordInputComponent],
  templateUrl: './register-form.html',
  styleUrl: './register-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterFormComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  readonly loading = input(false);
  readonly errorMessage = input('');
  readonly fieldErrors = input<Readonly<Record<string, string>>>({});
  readonly submitted = output<RegisterFormValue>();

  protected readonly attemptedSubmit = signal(false);
  protected readonly form = this.formBuilder.group(
    {
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      email: ['', [Validators.required, Validators.email]],
      phone: [''],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    {
      validators: matchingFieldsValidator('password', 'confirmPassword'),
    },
  );

  protected submit(): void {
    this.attemptedSubmit.set(true);
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    const value = this.form.getRawValue();
    this.submitted.emit({
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      email: value.email.trim(),
      phone: value.phone.trim() || null,
      password: value.password,
    });
  }

  protected fieldError(fieldName: 'firstName' | 'lastName' | 'email' | 'phone'): string {
    const control = this.form.controls[fieldName];
    const serverError = this.fieldErrors()[fieldName];

    if (serverError) {
      return serverError;
    }

    if (!this.attemptedSubmit() && !control.touched) {
      return '';
    }

    if (fieldName !== 'phone' && control.hasError('required')) {
      return 'This field is required.';
    }

    if (fieldName === 'email' && control.hasError('email')) {
      return 'Enter a valid email address.';
    }

    return '';
  }

  protected passwordError(): string {
    const control = this.form.controls.password;
    const serverError = this.fieldErrors()['password'];

    if (serverError) {
      return serverError;
    }

    if (!this.attemptedSubmit() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Enter a password.';
    }

    if (control.hasError('minlength')) {
      return 'Password must be at least 8 characters.';
    }

    return '';
  }

  protected confirmPasswordError(): string {
    const control = this.form.controls.confirmPassword;

    if (!this.attemptedSubmit() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Confirm your password.';
    }

    if (this.form.hasError('mismatch')) {
      return 'Passwords do not match.';
    }

    return '';
  }
}
