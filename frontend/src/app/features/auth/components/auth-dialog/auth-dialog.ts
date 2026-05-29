import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { NotificationStateService } from '@app/core/state/notification-state.service';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { BrandLogoComponent } from '@app/shared/components/brand-logo/brand-logo';
import { IconComponent } from '@app/shared/components/icon/icon';
import { ModalComponent } from '@app/shared/components/modal/modal';
import { LoginFormComponent, LoginFormValue } from '../login-form/login-form';
import { RegisterFormComponent, RegisterFormValue } from '../register-form/register-form';
import { AuthFacade } from '../../services/auth.facade';

type AuthTab = 'login' | 'register';
type SocialProvider = 'google' | 'facebook' | 'apple';

interface SocialProviderOption {
  readonly id: SocialProvider;
  readonly label: string;
}

@Component({
  selector: 'app-auth-dialog',
  standalone: true,
  imports: [
    BrandLogoComponent,
    IconComponent,
    LoginFormComponent,
    ModalComponent,
    RegisterFormComponent,
  ],
  templateUrl: './auth-dialog.html',
  styleUrl: './auth-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthDialogComponent {
  private readonly auth = inject(AuthFacade);
  private readonly route = inject(ActivatedRoute);
  private readonly notifications = inject(NotificationStateService);

  readonly open = input(false);
  readonly initialTab = input<AuthTab>('login');
  readonly closed = output<void>();

  protected readonly activeTab = signal<AuthTab>('login');
  protected readonly loginError = signal('');
  protected readonly loginFieldErrors = signal<Readonly<Record<string, string>>>({});
  protected readonly registerError = signal('');
  protected readonly registerFieldErrors = signal<Readonly<Record<string, string>>>({});
  protected readonly authLoading = this.auth.authLoading;

  protected readonly socialProviders: readonly SocialProviderOption[] = [
    { id: 'google', label: 'Continue with Google' },
    { id: 'facebook', label: 'Continue with Facebook' },
    { id: 'apple', label: 'Continue with Apple' },
  ];

  protected readonly creativeOutcomes = [
    'Brand consistency',
    'Higher engagement',
    'Better ROI',
    'Faster launch',
  ] as const;

  constructor() {
    effect(() => {
      if (this.open()) {
        this.activeTab.set(this.initialTab());
        this.resetErrors();
      }
    });
  }

  protected setTab(tab: AuthTab): void {
    this.activeTab.set(tab);
    this.resetErrors();
  }

  protected async submitLogin(value: LoginFormValue): Promise<void> {
    this.loginError.set('');
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
      this.loginError.set(result.message);
    }
  }

  protected async submitRegister(value: RegisterFormValue): Promise<void> {
    this.registerError.set('');
    this.registerFieldErrors.set({});

    const payload = {
      firstName: value.firstName,
      lastName: value.lastName,
      email: value.email,
      phone: value.phone,
      password: value.password,
    };
    const result = await this.auth.register(payload);
    if (result.ok) {
      return;
    }

    this.registerFieldErrors.set(result.fieldErrors);
    if (Object.keys(result.fieldErrors).length === 0) {
      this.registerError.set(result.message);
    }
  }

  protected showForgotPasswordHelp(): void {
    this.notifications.info(
      'Password recovery',
      'Use your workspace recovery flow or contact your account administrator.',
    );
  }

  protected socialLogin(provider: SocialProvider): void {
    const providerLabel = provider.charAt(0).toUpperCase() + provider.slice(1);

    this.notifications.info(
      `${providerLabel} sign-in`,
      'Social authentication will be available soon.',
    );
  }

  private resetErrors(): void {
    this.loginError.set('');
    this.loginFieldErrors.set({});
    this.registerError.set('');
    this.registerFieldErrors.set({});
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
