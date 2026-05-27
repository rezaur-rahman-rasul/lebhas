import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import {
  PreferredLanguage,
  ThemePreference,
  UserAccountSettings,
} from '../../models/profile.models';

@Component({
  selector: 'app-profile-settings-card',
  standalone: true,
  imports: [ButtonComponent, CardComponent],
  templateUrl: './profile-settings-card.html',
  styleUrl: './profile-settings-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileSettingsCardComponent {
  readonly settings = input.required<UserAccountSettings>();
  readonly editable = input(true);

  readonly editSettings = output<void>();

  protected readonly languageLabel = computed(() => preferredLanguageLabel(this.settings().preferredLanguage));
  protected readonly themeLabel = computed(() => themePreferenceLabel(this.settings().themePreference));
  protected readonly notificationSummary = computed(() => {
    const settings = this.settings();
    const enabled = [
      settings.notificationInAppEnabled ? 'in-app' : null,
      settings.notificationEmailEnabled ? 'email' : null,
      settings.marketingEmailEnabled ? 'marketing email' : null,
    ].filter(Boolean);

    return enabled.length ? enabled.join(', ') : 'Notifications are off';
  });
}

export function preferredLanguageLabel(value: PreferredLanguage | string): string {
  switch (value) {
    case PreferredLanguage.Bangla:
      return 'Bangla';
    case PreferredLanguage.English:
      return 'English';
    case PreferredLanguage.Both:
      return 'Bangla and English';
    default:
      return 'Not set';
  }
}

export function themePreferenceLabel(value: ThemePreference | string): string {
  switch (value) {
    case ThemePreference.System:
      return 'Use system setting';
    case ThemePreference.Dark:
      return 'Dark';
    case ThemePreference.Light:
      return 'Light';
    default:
      return 'Not set';
  }
}
