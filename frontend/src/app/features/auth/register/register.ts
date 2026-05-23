import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AutofocusDirective } from '@app/shared/directives/autofocus.directive';
import { matchingFieldsValidator } from '@app/shared/validators/matching-fields.validator';
import { ButtonComponent } from '@app/shared/components/button/button';
import { AuthCardComponent } from '../components/auth-card/auth-card';
import { AuthHeaderComponent } from '../components/auth-header/auth-header';
import { PasswordFieldComponent } from '../components/password-field/password-field';
import { AuthFacade } from '../services/auth.facade';

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    AutofocusDirective,
    ButtonComponent,
    AuthCardComponent,
    AuthHeaderComponent,
    PasswordFieldComponent,
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterPageComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;
  protected readonly auth = inject(AuthFacade);

  protected readonly submitted = signal(false);

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

  protected fieldError(
    controlName: 'firstName' | 'lastName' | 'email' | 'phone' | 'password',
  ): string {
    const control = this.form.controls[controlName];
    if (!this.submitted() && !control.touched) {
      return '';
    }

    const serverError = control.errors?.['server'] as string | undefined;
    if (serverError) {
      return serverError;
    }

    if (control.hasError('required')) {
      return 'This field is required.';
    }

    if (controlName === 'email' && control.hasError('email')) {
      return 'Enter a valid email address.';
    }

    if (controlName === 'password' && control.hasError('minlength')) {
      return 'Password must be at least 8 characters.';
    }

    return '';
  }

  protected confirmPasswordError(): string {
    const control = this.form.controls.confirmPassword;
    if (!this.submitted() && !control.touched) {
      return '';
    }

    const serverError = control.errors?.['server'] as string | undefined;
    if (serverError) {
      return serverError;
    }

    if (control.hasError('required')) {
      return 'Confirm your password.';
    }

    if (this.form.hasError('mismatch')) {
      return 'Passwords do not match.';
    }

    return '';
  }

  protected async submit(): Promise<void> {
    this.submitted.set(true);
    this.form.markAllAsTouched();
    this.clearServerErrors();

    if (this.form.invalid) {
      return;
    }

    const { confirmPassword: _confirmPassword, ...payload } = this.form.getRawValue();
    const result = await this.auth.register({
      ...payload,
      phone: payload.phone || null,
    });

    if (!result.ok) {
      this.applyServerErrors(result.fieldErrors);
    }
  }

  private applyServerErrors(fieldErrors: Readonly<Record<string, string>>): void {
    for (const [field, message] of Object.entries(fieldErrors)) {
      const control = this.form.get(field);
      if (control) {
        control.setErrors({ ...(control.errors ?? {}), server: message });
      }
    }
  }

  private clearServerErrors(): void {
    for (const control of Object.values(this.form.controls)) {
      if (control.errors?.['server']) {
        const { server: _server, ...remaining } = control.errors;
        control.setErrors(Object.keys(remaining).length > 0 ? remaining : null);
      }
    }
  }
}
