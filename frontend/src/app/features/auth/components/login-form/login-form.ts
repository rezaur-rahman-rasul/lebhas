import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/button/button';
import { InputComponent } from '@app/shared/components/input/input';
import { PasswordInputComponent } from '@app/shared/components/password-input/password-input';

export interface LoginFormValue {
  readonly email: string;
  readonly password: string;
  readonly rememberMe: boolean;
}

@Component({
  selector: 'app-login-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, InputComponent, PasswordInputComponent],
  templateUrl: './login-form.html',
  styleUrl: './login-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginFormComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  readonly loading = input(false);
  readonly errorMessage = input('');
  readonly fieldErrors = input<Readonly<Record<string, string>>>({});
  readonly submitted = output<LoginFormValue>();
  readonly forgotPassword = output<void>();

  protected readonly attemptedSubmit = signal(false);
  protected readonly form = this.formBuilder.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    rememberMe: [false],
  });

  protected submit(): void {
    this.attemptedSubmit.set(true);
    this.form.markAllAsTouched();

    if (this.form.invalid) {
      return;
    }

    const value = this.form.getRawValue();
    this.submitted.emit({
      email: value.email.trim(),
      password: value.password,
      rememberMe: value.rememberMe,
    });
  }

  protected emailError(): string {
    const control = this.form.controls.email;
    const serverError = this.fieldErrors()['email'] || this.fieldErrors()['identifier'];

    if (serverError) {
      return serverError;
    }

    if (!this.attemptedSubmit() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Enter your email address.';
    }

    if (control.hasError('email')) {
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
      return 'Enter your password.';
    }

    if (control.hasError('minlength')) {
      return 'Password must be at least 8 characters.';
    }

    return '';
  }
}
