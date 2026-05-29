import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ThemeStore } from '@app/core/theme/theme.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { PasswordInputComponent } from '@app/shared/components/password-input/password-input';
import { matchingFieldsValidator } from '@app/shared/validators/matching-fields.validator';
import { PasswordStrengthMeterComponent } from '../components/password-strength-meter/password-strength-meter';
import { ProfileLoadingStateComponent } from '../components/profile-loading-state/profile-loading-state';
import { PreferredLanguage, ThemePreference } from '../models/profile.models';
import { clearPasswordFormState } from '../services/profile-security';
import { ProfileStore } from '../state/profile.store';

@Component({
  selector: 'app-account-settings-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    ButtonComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    PasswordInputComponent,
    PasswordStrengthMeterComponent,
    ProfileLoadingStateComponent,
  ],
  templateUrl: './account-settings.html',
  styleUrl: './account-settings.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AccountSettingsPage {
  private readonly fb = new FormBuilder();
  private readonly router = inject(Router);
  private readonly theme = inject(ThemeStore);
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(ProfileStore);

  protected readonly settingsSaved = signal<string | null>(null);
  protected readonly passwordSaved = signal<string | null>(null);
  protected readonly passwordSubmitted = signal(false);
  protected readonly accessDenied = computed(() => !this.permissions.canEditOwnProfile());
  protected readonly canChangePassword = this.permissions.canChangeOwnPassword;
  protected readonly languages = Object.values(PreferredLanguage);
  protected readonly themes = Object.values(ThemePreference);

  protected readonly settingsForm = this.fb.nonNullable.group({
    preferredLanguage: [PreferredLanguage.English as PreferredLanguage | string],
    themePreference: [ThemePreference.System as ThemePreference | string],
    notificationEmailEnabled: [true],
    notificationInAppEnabled: [true],
    marketingEmailEnabled: [false],
  });

  protected readonly passwordForm = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required]],
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    },
    { validators: matchingFieldsValidator('newPassword', 'confirmPassword') },
  );

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      const settings = this.store.accountSettings();
      if (!settings) {
        void this.store.loadMyProfile();
        return;
      }

      this.settingsForm.reset({
        preferredLanguage: settings.preferredLanguage,
        themePreference: settings.themePreference,
        notificationEmailEnabled: settings.notificationEmailEnabled,
        notificationInAppEnabled: settings.notificationInAppEnabled,
        marketingEmailEnabled: settings.marketingEmailEnabled,
      });
    });
  }

  protected backToProfile(): void {
    clearPasswordFormState(this.passwordForm);
    void this.router.navigate(['/profile']);
  }

  protected retry(): void {
    this.store.clearError();
    void this.store.loadMyProfile();
  }

  protected async saveSettings(): Promise<void> {
    this.settingsSaved.set(null);
    const payload = this.settingsForm.getRawValue();
    const result = await this.store.updateMySettings(payload);
    if (!result.ok) {
      return;
    }

    if (payload.themePreference === ThemePreference.Dark) {
      this.theme.setTheme('dark');
    } else if (payload.themePreference === ThemePreference.Light) {
      this.theme.setTheme('light');
    }

    this.settingsSaved.set('Account settings updated.');
  }

  protected async changePassword(): Promise<void> {
    this.passwordSubmitted.set(true);
    this.passwordSaved.set(null);

    if (this.passwordForm.invalid || !this.canChangePassword()) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const result = await this.store.changePassword(this.passwordForm.getRawValue());
    if (result.ok) {
      clearPasswordFormState(this.passwordForm);
      this.passwordSubmitted.set(false);
      this.passwordSaved.set('Password changed successfully.');
      return;
    }

    this.passwordForm.controls.currentPassword.reset('');
    this.passwordForm.controls.currentPassword.markAsTouched();
  }

  protected languageLabel(value: PreferredLanguage | string): string {
    switch (value) {
      case PreferredLanguage.Bangla:
        return 'Bangla';
      case PreferredLanguage.Both:
        return 'Bangla and English';
      default:
        return 'English';
    }
  }

  protected themeLabel(value: ThemePreference | string): string {
    switch (value) {
      case ThemePreference.Dark:
        return 'Dark';
      case ThemePreference.Light:
        return 'Light';
      default:
        return 'Use system setting';
    }
  }
}
