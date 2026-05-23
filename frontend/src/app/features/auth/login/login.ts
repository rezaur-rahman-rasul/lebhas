import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { NotificationStateService } from '@app/core/state/notification-state.service';
import { RememberedProfile } from '../models/auth.models';
import { LoginFormComponent, LoginFormValue } from '../components/login-form/login-form';
import { RememberedProfilesComponent } from '../components/remembered-profiles/remembered-profiles';
import { RememberedPasswordDialogComponent } from '../components/remembered-password-dialog/remembered-password-dialog';
import { RemoveProfilesDialogComponent } from '../components/remove-profiles-dialog/remove-profiles-dialog';
import { AuthFacade } from '../services/auth.facade';
import { RememberedProfilesStorage } from '../services/remembered-profiles.storage';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [
    LoginFormComponent,
    RememberedProfilesComponent,
    RememberedPasswordDialogComponent,
    RemoveProfilesDialogComponent,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPageComponent {
  private readonly auth = inject(AuthFacade);
  private readonly notifications = inject(NotificationStateService);
  private readonly route = inject(ActivatedRoute);
  private readonly rememberedProfilesStorage = inject(RememberedProfilesStorage);

  readonly showSavedProfiles = input(true);

  protected readonly loginFormError = signal('');
  protected readonly loginFieldErrors = signal<Readonly<Record<string, string>>>({});
  protected readonly selectedProfile = signal<RememberedProfile | null>(null);
  protected readonly rememberedPasswordError = signal('');
  protected readonly manageProfilesOpen = signal(false);
  protected readonly showManualLogin = signal(false);

  protected readonly authLoading = this.auth.authLoading;
  protected readonly rememberedProfiles = this.rememberedProfilesStorage.profiles;
  protected readonly showProfilePicker = computed(
    () => this.showSavedProfiles() && this.rememberedProfiles().length > 0 && !this.showManualLogin(),
  );

  protected async submitLoginForm(value: LoginFormValue): Promise<void> {
    this.loginFormError.set('');
    this.loginFieldErrors.set({});

    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? undefined;
    const result = await this.auth.login(
      {
        email: value.email,
        password: value.password,
      },
      returnUrl,
      { rememberMe: value.rememberMe },
    );

    if (result.ok) {
      return;
    }

    const mappedFieldErrors = this.mapLoginFieldErrors(result.fieldErrors);
    this.loginFieldErrors.set(mappedFieldErrors);

    if (Object.keys(mappedFieldErrors).length === 0) {
      this.loginFormError.set(result.message);
    }
  }

  protected showForgotPasswordHelp(): void {
    this.notifications.info(
      'Password recovery',
      'Use your workspace recovery flow or contact your account administrator.',
    );
  }

  protected selectRememberedProfile(profile: RememberedProfile): void {
    this.clearAllErrors();
    this.rememberedPasswordError.set('');
    this.selectedProfile.set(profile);
  }

  protected closeRememberedProfileDialog(): void {
    this.rememberedPasswordError.set('');
    this.selectedProfile.set(null);
  }

  protected async submitRememberedProfilePassword(password: string): Promise<void> {
    const profile = this.selectedProfile();
    if (!profile) {
      return;
    }

    this.rememberedPasswordError.set('');

    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? undefined;
    const result = await this.auth.login({
      email: profile.email,
      password,
    }, returnUrl);

    if (result.ok) {
      this.closeRememberedProfileDialog();
      return;
    }

    this.rememberedPasswordError.set(result.fieldErrors['password'] || result.message);
  }

  protected openManualLogin(): void {
    this.clearAllErrors();
    this.closeRememberedProfileDialog();
    this.showManualLogin.set(true);
  }

  protected returnToSavedProfiles(): void {
    this.clearAllErrors();
    this.closeRememberedProfileDialog();
    this.showManualLogin.set(false);
  }

  protected openManageProfiles(): void {
    this.manageProfilesOpen.set(true);
  }

  protected closeManageProfiles(): void {
    this.manageProfilesOpen.set(false);
  }

  protected removeRememberedProfile(profileId: string): void {
    this.rememberedProfilesStorage.removeProfile(profileId);

    if (this.selectedProfile()?.id === profileId) {
      this.closeRememberedProfileDialog();
    }

    if (this.rememberedProfiles().length === 0) {
      this.manageProfilesOpen.set(false);
      this.showManualLogin.set(true);
    }
  }

  private clearAllErrors(): void {
    this.loginFormError.set('');
    this.loginFieldErrors.set({});
  }

  private mapLoginFieldErrors(fieldErrors: Readonly<Record<string, string>>): Record<string, string> {
    const identifierError =
      fieldErrors['identifier'] ||
      fieldErrors['email'] ||
      fieldErrors['mobile'] ||
      fieldErrors['phone'];

    return {
      ...(identifierError ? { email: identifierError } : {}),
      ...(fieldErrors['password'] ? { password: fieldErrors['password'] } : {}),
    };
  }
}
