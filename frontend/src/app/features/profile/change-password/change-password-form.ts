import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { PasswordInputComponent } from '@app/shared/components/password-input/password-input';
import { matchingFieldsValidator } from '@app/shared/validators/matching-fields.validator';
import { PasswordStrengthMeterComponent } from '../components/password-strength-meter/password-strength-meter';
import { ProfileStore } from '../state/profile.store';
import { clearPasswordFormState } from '../services/profile-security';

@Component({
  selector: 'app-change-password-form-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    PasswordInputComponent,
    PasswordStrengthMeterComponent,
  ],
  templateUrl: './change-password-form.html',
  styleUrl: './change-password-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChangePasswordFormPage {
  private readonly fb = new FormBuilder();
  private readonly router = inject(Router);
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(ProfileStore);

  protected readonly submitted = signal(false);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly accessDenied = computed(() => !this.permissions.canChangeOwnPassword());

  protected readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: matchingFieldsValidator('newPassword', 'confirmPassword') },
  );

  protected cancel(): void {
    clearPasswordFormState(this.form);
    void this.router.navigate(['/profile']);
  }

  protected async submit(): Promise<void> {
    this.submitted.set(true);
    this.successMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const result = await this.store.changePassword(this.form.getRawValue());
    if (result.ok) {
      clearPasswordFormState(this.form);
      this.submitted.set(false);
      this.successMessage.set('Password changed successfully.');
      return;
    }

    this.form.controls.currentPassword.reset('');
    this.form.controls.currentPassword.markAsTouched();
  }
}
