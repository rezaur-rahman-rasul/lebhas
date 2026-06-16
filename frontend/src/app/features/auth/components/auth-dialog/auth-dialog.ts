import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { ModalComponent } from '@app/shared/components/modal/modal';
import { BrandLogoComponent } from '@app/shared/components/brand-logo/brand-logo';
import { AuthFacade } from '../../services/auth.facade';
import { RegistrationNextStep, RegistrationStepResponse } from '../../models/auth.models';

type AuthTab = 'REGISTER' | 'LOGIN';
type RegisterStep = Exclude<RegistrationNextStep, 'CREATIVE_GENERATOR'> | 'MOBILE_INPUT';

@Component({
  selector: 'app-auth-dialog',
  standalone: true,
  imports: [FormsModule, ModalComponent, BrandLogoComponent],
  templateUrl: './auth-dialog.html',
  styleUrl: './auth-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthDialogComponent {
  private readonly auth = inject(AuthFacade);
  private readonly route = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  readonly open = input(false);
  readonly initialTab = input<'login' | 'register'>('login');
  readonly closed = output<void>();

  protected readonly activeTab = signal<AuthTab>('REGISTER');
  protected readonly registerStep = signal<RegisterStep>('MOBILE_INPUT');
  protected readonly registrationSessionToken = signal<string | null>(null);
  protected readonly mobileNumberMasked = signal('');
  protected readonly emailMasked = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal('');
  protected readonly resendCountdown = signal(0);
  protected readonly otpLength = signal(6);

  protected readonly mobileNumber = signal('');
  protected readonly mobileOtp = signal('');
  protected readonly email = signal('');
  protected readonly emailOtp = signal('');
  protected readonly password = signal('');
  protected readonly confirmPassword = signal('');
  protected readonly brandName = signal('');
  protected readonly productServiceName = signal('');
  protected readonly projectCampaignName = signal('');
  protected readonly loginEmail = signal('');
  protected readonly loginPassword = signal('');

  protected readonly visual = computed(() => this.visualFor(this.activeTab(), this.registerStep()));
  protected readonly canResend = computed(() => this.resendCountdown() === 0 && !this.loading());

  private countdownId: ReturnType<typeof setInterval> | null = null;

  constructor() {
    effect(() => {
      if (this.open()) {
        this.reset();
      }
    });

    this.destroyRef.onDestroy(() => this.clearCountdown());
  }

  protected switchTab(tab: AuthTab): void {
    if (this.activeTab() === tab || this.loading()) {
      return;
    }
    this.activeTab.set(tab);
    this.error.set('');
    this.clearCountdown();
  }

  protected async submitMobile(): Promise<void> {
    const mobileNumber = this.mobileNumber().trim();
    if (!this.isValidBangladeshMobile(mobileNumber)) {
      this.error.set('Enter a valid Bangladesh mobile number, for example 01712345678.');
      return;
    }

    await this.runStep(async () => {
      const response = await this.auth.startRegistrationMobile({ mobileNumber });
      this.applyStep(response);
      this.mobileOtp.set('');
      this.startCountdown(response.resendAfterSeconds || 60);
    });
  }

  protected async verifyMobileOtp(): Promise<void> {
    const token = this.requireSessionToken();
    if (!token) {
      return;
    }
    const otp = this.mobileOtp().trim();
    if (otp.length !== this.otpLength()) {
      this.error.set(`Enter the ${this.otpLength()} digit OTP.`);
      return;
    }

    await this.runStep(async () => {
      this.applyStep(await this.auth.verifyRegistrationMobile({ otpToken: token, otp }));
      this.clearCountdown();
    });
  }

  protected async skipEmail(): Promise<void> {
    const token = this.requireSessionToken();
    if (!token) {
      return;
    }

    await this.runStep(async () => this.applyStep(await this.auth.skipRegistrationEmail({ registrationSessionToken: token })));
  }

  protected async startEmail(): Promise<void> {
    const token = this.requireSessionToken();
    if (!token) {
      return;
    }
    const email = this.email().trim();
    if (!email || !email.includes('@')) {
      this.error.set('Enter a valid email address or skip this step.');
      return;
    }

    await this.runStep(async () => {
      const response = await this.auth.startRegistrationEmail({ registrationSessionToken: token, email });
      this.applyStep(response);
      this.emailOtp.set('');
      this.startCountdown(response.resendAfterSeconds || 60);
    });
  }

  protected async verifyEmailOtp(): Promise<void> {
    const token = this.requireSessionToken();
    if (!token) {
      return;
    }
    const otp = this.emailOtp().trim();
    if (otp.length !== this.otpLength()) {
      this.error.set(`Enter the ${this.otpLength()} digit email OTP.`);
      return;
    }

    await this.runStep(async () => {
      this.applyStep(await this.auth.verifyRegistrationEmail({ registrationSessionToken: token, otp }));
      this.clearCountdown();
    });
  }

  protected async setPassword(): Promise<void> {
    const token = this.requireSessionToken();
    if (!token) {
      return;
    }
    if (this.password() !== this.confirmPassword()) {
      this.error.set('Password and confirm password do not match.');
      return;
    }

    await this.runStep(async () => this.applyStep(await this.auth.setRegistrationPassword({
      registrationSessionToken: token,
      password: this.password(),
      confirmPassword: this.confirmPassword(),
    })));
  }

  protected async submitBrand(): Promise<void> {
    const token = this.requireSessionToken();
    if (!token) {
      return;
    }
    const brandName = this.brandName().trim();
    if (brandName.length < 2) {
      this.error.set('Enter a brand name.');
      return;
    }

    await this.runStep(async () => this.applyStep(await this.auth.completeRegistrationBrand({
      registrationSessionToken: token,
      brandName,
    })));
  }

  protected async submitProductService(): Promise<void> {
    const token = this.requireSessionToken();
    if (!token) {
      return;
    }
    const productServiceName = this.productServiceName().trim();
    if (productServiceName.length < 2) {
      this.error.set('Enter a product or service name.');
      return;
    }

    await this.runStep(async () => this.applyStep(await this.auth.completeRegistrationProductService({
      registrationSessionToken: token,
      productServiceName,
    })));
  }

  protected async submitProjectCampaign(): Promise<void> {
    const token = this.requireSessionToken();
    if (!token) {
      return;
    }
    const projectCampaignName = this.projectCampaignName().trim();
    if (projectCampaignName.length < 2) {
      this.error.set('Enter a project or campaign name.');
      return;
    }

    this.loading.set(true);
    this.error.set('');
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? undefined;
    const result = await this.auth.completeRegistrationProjectCampaign({
      registrationSessionToken: token,
      projectCampaignName,
    }, returnUrl);
    if (!result.ok) {
      this.loading.set(false);
      this.error.set(result.message);
    }
  }

  protected async login(): Promise<void> {
    const email = this.loginEmail().trim();
    const password = this.loginPassword();
    if (!email || !email.includes('@') || !password) {
      this.error.set('Enter email and password.');
      return;
    }

    this.loading.set(true);
    this.error.set('');
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') ?? undefined;
    const result = await this.auth.login({ email, password, workspaceId: null }, returnUrl, { rememberMe: true });
    if (!result.ok) {
      this.loading.set(false);
      this.error.set(result.message);
    }
  }

  protected async resendOtp(): Promise<void> {
    if (!this.canResend()) {
      return;
    }
    if (this.registerStep() === 'MOBILE_OTP') {
      await this.submitMobile();
      return;
    }
    if (this.registerStep() === 'EMAIL_OTP') {
      await this.startEmail();
    }
  }

  protected changeMobile(): void {
    if (this.loading()) {
      return;
    }
    this.clearCountdown();
    this.registrationSessionToken.set(null);
    this.mobileOtp.set('');
    this.registerStep.set('MOBILE_INPUT');
    this.error.set('');
  }

  protected changeEmail(): void {
    if (this.loading()) {
      return;
    }
    this.clearCountdown();
    this.emailOtp.set('');
    this.registerStep.set('EMAIL_OPTIONAL');
    this.error.set('');
  }

  protected close(): void {
    this.closed.emit();
  }

  private async runStep(action: () => Promise<void>): Promise<void> {
    this.loading.set(true);
    this.error.set('');
    try {
      await action();
    } catch (error) {
      this.error.set(this.errorText(error));
    } finally {
      this.loading.set(false);
    }
  }

  private applyStep(response: RegistrationStepResponse): void {
    this.registrationSessionToken.set(response.registrationSessionToken);
    this.mobileNumberMasked.set(response.mobileNumberMasked ?? '');
    this.emailMasked.set(response.emailMasked ?? this.emailMasked());
    this.otpLength.set(response.otpLength || 6);
    if (response.nextStep !== 'CREATIVE_GENERATOR') {
      this.registerStep.set(response.nextStep);
    }
  }

  private requireSessionToken(): string | null {
    const token = this.registrationSessionToken();
    if (!token) {
      this.error.set('Registration session expired. Start again with your mobile number.');
      this.registerStep.set('MOBILE_INPUT');
      return null;
    }
    return token;
  }

  private reset(): void {
    this.clearCountdown();
    this.activeTab.set(this.initialTab() === 'login' ? 'LOGIN' : 'REGISTER');
    this.registerStep.set('MOBILE_INPUT');
    this.registrationSessionToken.set(null);
    this.mobileNumberMasked.set('');
    this.emailMasked.set('');
    this.loading.set(false);
    this.error.set('');
    this.resendCountdown.set(0);
    this.otpLength.set(6);
    this.mobileNumber.set('');
    this.mobileOtp.set('');
    this.email.set('');
    this.emailOtp.set('');
    this.password.set('');
    this.confirmPassword.set('');
    this.brandName.set('');
    this.productServiceName.set('');
    this.projectCampaignName.set('');
    this.loginEmail.set('');
    this.loginPassword.set('');
  }

  private startCountdown(seconds: number): void {
    this.clearCountdown();
    this.resendCountdown.set(seconds);
    this.countdownId = setInterval(() => {
      const next = Math.max(0, this.resendCountdown() - 1);
      this.resendCountdown.set(next);
      if (next === 0) {
        this.clearCountdown();
      }
    }, 1000);
  }

  private clearCountdown(): void {
    if (this.countdownId) {
      clearInterval(this.countdownId);
      this.countdownId = null;
    }
  }

  private isValidBangladeshMobile(value: string): boolean {
    const digits = value.replace(/[^0-9]/g, '').replace(/^880/, '0');
    return /^01[3-9][0-9]{8}$/.test(digits);
  }

  private errorText(error: unknown): string {
    if (error && typeof error === 'object' && 'message' in error && typeof error.message === 'string') {
      return error.message;
    }
    return 'Request failed. Please try again.';
  }

  private visualFor(tab: AuthTab, step: RegisterStep): { kicker: string; title: string; caption: string; stage: string } {
    if (tab === 'LOGIN') {
      return {
        kicker: 'Secure return',
        title: 'Resume the creative pipeline',
        caption: 'Email and password restore your workspace, credits, assets, and campaign context.',
        stage: 'Session -> Workspace -> Creative generator',
      };
    }

    const copy: Record<RegisterStep, { kicker: string; title: string; caption: string; stage: string }> = {
      MOBILE_INPUT: {
        kicker: 'Raw signal',
        title: 'Start with a verified mobile identity',
        caption: 'A number becomes a protected workspace without issuing credits or JWTs too early.',
        stage: 'Raw image -> Mobile trust',
      },
      MOBILE_OTP: {
        kicker: 'Conversion check',
        title: 'Verify before rewards unlock',
        caption: 'The workspace is reserved, but signup credits wait until OTP verification succeeds.',
        stage: 'Mobile OTP -> Signup credits',
      },
      EMAIL_OPTIONAL: {
        kicker: 'Optional layer',
        title: 'Add email or skip cleanly',
        caption: 'Email is useful for password login and rewards, but it is not required to finish onboarding.',
        stage: 'Identity -> Optional email',
      },
      EMAIL_OTP: {
        kicker: 'Inbox proof',
        title: 'Attach one unique email',
        caption: 'Email OTP prevents account sharing collisions and unlocks the configured email reward once.',
        stage: 'Email OTP -> Reward claim',
      },
      PASSWORD_SETUP: {
        kicker: 'Account hardening',
        title: 'Create the password login path',
        caption: 'A strong password enables normal email login for admins and crew members.',
        stage: 'Password -> Secure login',
      },
      BRAND_NAME: {
        kicker: 'Brand identity',
        title: 'Create or select the brand',
        caption: 'Brand uniqueness stays scoped to the workspace and existing names are reused.',
        stage: 'Workspace -> Brand',
      },
      PRODUCT_SERVICE_NAME: {
        kicker: 'Catalog layer',
        title: 'Add the product or service',
        caption: 'The item is linked to the selected brand so Creative Generator has product context.',
        stage: 'Brand -> Product/service',
      },
      PROJECT_CAMPAIGN_NAME: {
        kicker: 'Campaign launch',
        title: 'Name the campaign',
        caption: 'This completes the minimum creative hierarchy and unlocks the generator session.',
        stage: 'Product -> Campaign -> Generator',
      },
    };
    return copy[step];
  }
}
