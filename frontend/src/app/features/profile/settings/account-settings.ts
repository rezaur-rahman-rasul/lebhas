import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ThemeStore } from '@app/core/theme/theme.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ProfileLoadingStateComponent } from '../components/profile-loading-state/profile-loading-state';
import { PreferredLanguage, ThemePreference } from '../models/profile.models';
import { ProfileStore } from '../state/profile.store';

@Component({
  selector: 'app-account-settings-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
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

  protected readonly successMessage = signal<string | null>(null);
  protected readonly accessDenied = computed(() => !this.permissions.canEditOwnProfile());
  protected readonly languages = Object.values(PreferredLanguage);
  protected readonly themes = Object.values(ThemePreference);

  protected readonly form = this.fb.nonNullable.group({
    preferredLanguage: [PreferredLanguage.English as PreferredLanguage | string],
    themePreference: [ThemePreference.System as ThemePreference | string],
    notificationEmailEnabled: [true],
    notificationInAppEnabled: [true],
    marketingEmailEnabled: [false],
  });

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

      this.form.reset({
        preferredLanguage: settings.preferredLanguage,
        themePreference: settings.themePreference,
        notificationEmailEnabled: settings.notificationEmailEnabled,
        notificationInAppEnabled: settings.notificationInAppEnabled,
        marketingEmailEnabled: settings.marketingEmailEnabled,
      });
    });
  }

  protected cancel(): void {
    void this.router.navigate(['/profile']);
  }

  protected retry(): void {
    this.store.clearError();
    void this.store.loadMyProfile();
  }

  protected async submit(): Promise<void> {
    this.successMessage.set(null);
    const payload = this.form.getRawValue();
    const result = await this.store.updateMySettings(payload);

    if (!result.ok) {
      return;
    }

    if (payload.themePreference === ThemePreference.Dark) {
      this.theme.setTheme('dark');
    } else if (payload.themePreference === ThemePreference.Light) {
      this.theme.setTheme('light');
    }

    this.successMessage.set('Account settings updated successfully.');
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
