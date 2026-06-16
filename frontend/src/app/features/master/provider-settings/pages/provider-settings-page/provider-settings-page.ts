import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { interval } from 'rxjs';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ModalComponent } from '@app/shared/components/app-dialog/app-dialog';
import { AiProviderCredentialView, AiProviderView, ProviderManagementCategory, SmsProviderActionResult } from '../../models/provider-credit-exchange.models';
import { MaskedSecretFieldComponent } from '../../components/masked-secret-field/masked-secret-field';
import { ProviderCardComponent } from '../../components/provider-card/provider-card';
import { ProviderCredentialFormComponent } from '../../components/provider-credential-form/provider-credential-form';
import { ProviderSettingsStore } from '../../state/providerSettings.store';

const CATEGORY_EMPTY_COPY: Record<ProviderManagementCategory, ProviderCategoryEmptyCopy> = {
  AI: {
    title: 'No AI providers configured yet.',
    description: 'Add the model provider used for creative generation, prompt rewriting, captions, and media workflows.',
    checklist: [
      'Store the API key as an encrypted credential.',
      'Set default model and model JSON endpoint when available.',
      'Enable image, text, video, or audio capabilities as needed.',
    ],
    createLabel: 'Create AI provider',
  },
  SMS: {
    title: 'No SMS providers configured yet.',
    description: 'Connect an SMS gateway before mobile OTP, transactional alerts, or marketing SMS can be delivered.',
    checklist: [
      'Configure the send SMS endpoint and approved sender ID.',
      'Set OTP length, expiry, resend cooldown, and attempt limits.',
      'Save an active credential before sending test SMS.',
    ],
    createLabel: 'Create SMS provider',
  },
  EMAIL: {
    title: 'No email providers configured yet.',
    description: 'Add an email provider for OTP messages, notifications, invoices, and operational reports.',
    checklist: [
      'Save SMTP or API credentials securely.',
      'Use a verified sender domain or sender address.',
      'Keep email OTP and notification delivery separate from SMS.',
    ],
    createLabel: 'Create email provider',
  },
  STORAGE: {
    title: 'No storage providers configured yet.',
    description: 'Add storage for uploaded assets, generated creatives, previews, downloads, and delivery URLs.',
    checklist: [
      'Configure bucket or storage endpoint details.',
      'Store access credentials server-side only.',
      'Confirm preview and download URL behavior before production use.',
    ],
    createLabel: 'Create storage provider',
  },
  PAYMENT: {
    title: 'No payment providers configured yet.',
    description: 'Add payment gateways for credit purchases, subscriptions, renewals, and package upgrades.',
    checklist: [
      'Configure gateway credentials and callback endpoints.',
      'Verify sandbox payments before going live.',
      'Keep inactive gateways available for audit history.',
    ],
    createLabel: 'Create payment provider',
  },
};

const PROVIDER_SETTINGS_REFRESH_INTERVAL_MS = 60_000;

@Component({
  selector: 'app-provider-settings-page',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, AppErrorStateComponent, PageHeaderComponent, ModalComponent, ProviderCardComponent, ProviderCredentialFormComponent, MaskedSecretFieldComponent],
  templateUrl: './provider-settings-page.html',
  styleUrl: './provider-settings-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderSettingsPage {
  protected readonly store = inject(ProviderSettingsStore);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly providerDialogOpen = signal(false);
  protected readonly credentialDialogOpen = signal(false);
  protected readonly editingProvider = signal<AiProviderView | null>(null);
  protected readonly editingCredential = signal<AiProviderCredentialView | null>(null);
  protected readonly modelsDialogOpen = signal(false);
  protected readonly testSmsDialogOpen = signal(false);
  protected readonly searchTerm = signal('');
  protected readonly smsActionResult = signal<SmsProviderActionResult | null>(null);
  protected readonly selectedCategory = signal<ProviderManagementCategory>('AI');
  protected readonly categories: readonly ProviderCategoryOption[] = [
    { id: 'AI', label: 'AI Providers', description: 'Creative generation, prompts, captions, video, voice.' },
    { id: 'SMS', label: 'SMS Providers', description: 'OTP, transactional, and marketing SMS.' },
    { id: 'EMAIL', label: 'Email Providers', description: 'OTP, notifications, reports, invoices.' },
    { id: 'STORAGE', label: 'Storage Providers', description: 'Assets, creatives, downloads, delivery.' },
    { id: 'PAYMENT', label: 'Payment Providers', description: 'Credits, subscriptions, package upgrades.' },
  ];

  protected readonly providerForm = new FormGroup({
    providerCode: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    providerName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    apiKey: new FormControl('', { nonNullable: true }),
    baseUrl: new FormControl('', { nonNullable: true }),
    defaultModel: new FormControl('', { nonNullable: true }),
    modelsEndpoint: new FormControl('', { nonNullable: true }),
    modelsEndpointAuth: new FormControl('BEARER', { nonNullable: true }),
    apiKeyQueryParam: new FormControl('', { nonNullable: true }),
    sendSmsEndpoint: new FormControl('', { nonNullable: true }),
    balanceEndpoint: new FormControl('', { nonNullable: true }),
    requestMethod: new FormControl('GET', { nonNullable: true }),
    senderId: new FormControl('', { nonNullable: true }),
    otpLength: new FormControl(6, { nonNullable: true, validators: [Validators.min(4)] }),
    otpExpiryMinutes: new FormControl(5, { nonNullable: true, validators: [Validators.min(1)] }),
    resendCooldownSeconds: new FormControl(60, { nonNullable: true, validators: [Validators.min(0)] }),
    maxAttempts: new FormControl(3, { nonNullable: true, validators: [Validators.min(1)] }),
    balanceMonitoringEnabled: new FormControl(true, { nonNullable: true }),
    healthCheckEnabled: new FormControl(true, { nonNullable: true }),
    priority: new FormControl(100, { nonNullable: true, validators: [Validators.min(0)] }),
    rateLimitPerMinute: new FormControl(60, { nonNullable: true, validators: [Validators.min(1)] }),
    costMultiplier: new FormControl(1, { nonNullable: true, validators: [Validators.min(0.0001)] }),
    availableCreditBalance: new FormControl<number | null>(null, { validators: [Validators.min(0)] }),
    openAiAdminApiKey: new FormControl('', { nonNullable: true }),
    providerTopUpAmountUsd: new FormControl<number | null>(null, { validators: [Validators.min(0)] }),
    providerTopUpDate: new FormControl<string | null>(null),
    providerManualBalanceUsd: new FormControl<number | null>(null, { validators: [Validators.min(0)] }),
    costSyncEnabled: new FormControl(false, { nonNullable: true }),
    metadataJson: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    supportsImage: new FormControl(true, { nonNullable: true }),
    supportsText: new FormControl(true, { nonNullable: true }),
    supportsVideo: new FormControl(false, { nonNullable: true }),
    supportsVoice: new FormControl(false, { nonNullable: true }),
    supportsOtp: new FormControl(true, { nonNullable: true }),
    supportsNotificationSms: new FormControl(true, { nonNullable: true }),
    supportsMarketingSms: new FormControl(false, { nonNullable: true }),
  });

  protected readonly testSmsForm = new FormGroup({
    mobileNumber: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    message: new FormControl('Your Lebhas test OTP is 123456', { nonNullable: true }),
  });

  protected readonly selectedProviderId = this.store.selectedProviderId;
  protected readonly selectedProvider = computed(() => {
    const selectedId = this.selectedProviderId();
    if (!selectedId) {
      return null;
    }
    return this.store.providers().find((provider) => provider.id === selectedId) ?? null;
  });
  protected readonly selectedProviderCredentials = computed(() => this.store.credentials());
  protected readonly modelsJsonText = computed(() => JSON.stringify(this.store.modelsJson()?.modelsJson ?? {}, null, 2));
  protected readonly actionResultBalanceLabel = computed(() => {
    const result = this.smsActionResult();
    const response = result?.response;
    if (!response) {
      return null;
    }
    const balance = this.normalizeOptionalNumber(
      response['availableCreditBalance'] ??
      response['estimatedRemainingBalanceUsd'] ??
      response['balance'] ??
      response['available_balance'] ??
      response['credit_balance'] ??
      response['total_available'],
    );
    if (balance === null) {
      return null;
    }
    return String(response['currency'] ?? '').toUpperCase() === 'USD'
      ? this.creditBalanceLabel(balance)
      : new Intl.NumberFormat('en-US', { maximumFractionDigits: 2, minimumFractionDigits: 2 }).format(balance);
  });
  protected readonly actionResultDetail = computed(() => {
    const result = this.smsActionResult();
    if (!result?.response) {
      return null;
    }
    const responseCode = result.response['response_code'];
    const balance = result.response['balance'];
    if (balance !== undefined && balance !== null) {
      return responseCode ? `Response code ${responseCode}. Balance ${this.actionResultBalanceLabel()}.` : `Balance ${this.actionResultBalanceLabel()}.`;
    }
    const body = result.response['body'];
    if (typeof body === 'string' && body.trim()) {
      return body.length > 120 ? `${body.slice(0, 120)}...` : body;
    }
    return null;
  });
  protected readonly categoryProviders = computed(() =>
    this.store.providers().filter((provider) => this.providerCategory(provider) === this.selectedCategory()),
  );
  protected readonly filteredProviders = computed(() => {
    const query = this.searchTerm().trim().toLowerCase();
    const providers = this.categoryProviders();
    if (!query) {
      return providers;
    }
    return providers.filter((provider) => this.providerSearchText(provider).includes(query));
  });

  constructor() {
    effect(() => void this.store.loadProviders());
    interval(PROVIDER_SETTINGS_REFRESH_INTERVAL_MS)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.store.saving()) {
          void this.store.refreshProvidersQuietly();
        }
      });
    this.providerForm.controls.providerCode.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.applyProviderTemplate(value));
  }

  openProvider(provider?: AiProviderView): void {
    this.editingProvider.set(provider ?? null);
    if (provider) {
      this.selectedCategory.set(this.providerCategory(provider));
    }
    this.providerForm.reset(provider ? {
      providerCode: provider.providerCode,
      providerName: provider.providerName,
      apiKey: '',
      baseUrl: provider.baseUrl ?? '',
      defaultModel: provider.defaultModel ?? '',
      modelsEndpoint: provider.modelsEndpoint ?? '',
      modelsEndpointAuth: provider.modelsEndpointAuth ?? 'BEARER',
      apiKeyQueryParam: provider.apiKeyQueryParam ?? '',
      sendSmsEndpoint: provider.sendSmsEndpoint ?? '',
      balanceEndpoint: provider.balanceEndpoint ?? '',
      requestMethod: provider.requestMethod ?? 'GET',
      senderId: provider.senderId ?? '',
      otpLength: provider.otpLength ?? 6,
      otpExpiryMinutes: provider.otpExpiryMinutes ?? 5,
      resendCooldownSeconds: provider.resendCooldownSeconds ?? 60,
      maxAttempts: provider.maxAttempts ?? 3,
      balanceMonitoringEnabled: provider.balanceMonitoringEnabled ?? true,
      healthCheckEnabled: provider.healthCheckEnabled ?? true,
      priority: provider.priority ?? 100,
      rateLimitPerMinute: provider.rateLimitPerMinute ?? 60,
      costMultiplier: Number(provider.costMultiplier ?? 1),
      availableCreditBalance: this.normalizeOptionalNumber(provider.availableCreditBalance),
      openAiAdminApiKey: '',
      providerTopUpAmountUsd: this.normalizeOptionalNumber(provider.providerTopUpAmountUsd),
      providerTopUpDate: provider.providerTopUpDate ?? null,
      providerManualBalanceUsd: this.normalizeOptionalNumber(provider.providerManualBalanceUsd),
      costSyncEnabled: Boolean(provider.costSyncEnabled),
      metadataJson: provider.metadataJson ?? '',
      active: provider.active,
      supportsImage: provider.supportsImage,
      supportsText: provider.supportsText,
      supportsVideo: provider.supportsVideo,
      supportsVoice: provider.supportsVoice,
      supportsOtp: provider.supportsOtp ?? false,
      supportsNotificationSms: provider.supportsNotificationSms ?? false,
      supportsMarketingSms: provider.supportsMarketingSms ?? false,
    } : this.defaultProviderFormValue());
    this.providerDialogOpen.set(true);
  }

  async saveProvider(): Promise<void> {
    const existingProvider = this.existingProviderByCode(this.providerForm.controls.providerCode.value);
    if (!this.editingProvider() && !existingProvider && !this.providerForm.controls.apiKey.value.trim()) {
      this.providerForm.controls.apiKey.setErrors({ required: true });
    }
    if (this.providerForm.controls.metadataJson.value.trim()) {
      try {
        JSON.parse(this.providerForm.controls.metadataJson.value);
      } catch {
        this.providerForm.controls.metadataJson.setErrors({ json: true });
      }
    }
    if (this.providerForm.invalid) {
      this.providerForm.markAllAsTouched();
      return;
    }
    const providerId = this.editingProvider()?.id ?? existingProvider?.id;
    const ok = await this.store.saveProvider({
      ...this.providerForm.getRawValue(),
      providerType: this.selectedCategory(),
    }, providerId);
    if (ok) this.providerDialogOpen.set(false);
  }

  selectCategory(category: ProviderManagementCategory): void {
    this.selectedCategory.set(category);
    this.searchTerm.set('');
    const firstProvider = this.store.providers().find((provider) => this.providerCategory(provider) === category);
    this.store.setSelectedProvider(firstProvider?.id ?? null);
  }

  categoryCount(category: ProviderManagementCategory): number {
    return this.store.providers().filter((provider) => this.providerCategory(provider) === category).length;
  }

  selectProvider(provider: AiProviderView): void {
    this.store.setSelectedProvider(provider.id);
    this.editingCredential.set(null);
  }

  updateSearchTerm(value: string): void {
    this.searchTerm.set(value);
    const visibleProviders = this.matchingProviders(value);
    const selectedId = this.selectedProviderId();
    if (!selectedId || !visibleProviders.some((provider) => provider.id === selectedId)) {
      this.store.setSelectedProvider(visibleProviders[0]?.id ?? null);
    }
  }

  configure(provider: AiProviderView): void {
    this.store.setSelectedProvider(provider.id);
    this.editingCredential.set(null);
    this.credentialDialogOpen.set(true);
  }

  rotate(credential: AiProviderCredentialView): void {
    this.editingCredential.set(credential);
    this.credentialDialogOpen.set(true);
  }

  async saveCredential(payload: Parameters<ProviderSettingsStore['saveCredential']>[1]): Promise<void> {
    const provider = this.selectedProvider();
    if (!provider) return;
    const ok = await this.store.saveCredential(provider.id, payload, this.editingCredential()?.id);
    if (ok) {
      this.credentialDialogOpen.set(false);
      this.editingCredential.set(null);
    }
  }

  async deactivate(credential: AiProviderCredentialView): Promise<void> {
    const provider = this.selectedProvider();
    if (provider && confirm('Deactivate this provider credential?')) {
      await this.store.deleteCredential(provider.id, credential.id);
    }
  }

  async toggle(provider: AiProviderView): Promise<void> {
    if (provider.active && !confirm('Disable this provider? Routing will stop using it.')) return;
    await this.store.toggleProvider(provider);
  }

  async deleteProvider(provider: AiProviderView): Promise<void> {
    const name = provider.displayName || provider.providerName || provider.providerCode;
    if (!confirm(`Delete ${name}? This will deactivate its saved credentials and remove it from Provider Management.`)) return;
    await this.store.deleteProvider(provider);
  }

  async test(provider: AiProviderView): Promise<void> {
    await this.store.testConnection(provider);
  }

  openTestSmsDialog(provider: AiProviderView): void {
    this.store.setSelectedProvider(provider.id);
    this.smsActionResult.set(null);
    this.testSmsForm.reset({ mobileNumber: '', message: 'Your Lebhas test OTP is 123456' });
    this.testSmsDialogOpen.set(true);
  }

  async sendTestSms(): Promise<void> {
    const provider = this.selectedProvider();
    if (!provider || this.testSmsForm.invalid) {
      this.testSmsForm.markAllAsTouched();
      return;
    }
    const value = this.testSmsForm.getRawValue();
    this.smsActionResult.set(await this.store.testSms(provider, value.mobileNumber, value.message));
  }

  async checkBalance(provider: AiProviderView): Promise<void> {
    this.store.setSelectedProvider(provider.id);
    this.smsActionResult.set(
      this.providerCategory(provider) === 'SMS'
        ? await this.store.checkSmsBalance(provider)
        : await this.store.checkProviderBalance(provider),
    );
  }

  async syncCosts(provider: AiProviderView): Promise<void> {
    this.store.setSelectedProvider(provider.id);
    const result = await this.store.syncOpenAiCosts(provider);
    if (result) {
      this.smsActionResult.set(null);
    }
  }

  async viewModelsJson(provider: AiProviderView): Promise<void> {
    const ok = await this.store.loadModelsJson(provider);
    if (ok) this.modelsDialogOpen.set(true);
  }

  closeModelsDialog(): void {
    this.modelsDialogOpen.set(false);
    this.store.clearModelsJson();
  }

  clearSearch(): void {
    this.searchTerm.set('');
    const firstProvider = this.categoryProviders()[0] ?? null;
    this.store.setSelectedProvider(firstProvider?.id ?? null);
  }

  protected isAiCategory(): boolean {
    return this.selectedCategory() === 'AI';
  }

  protected isSmsCategory(): boolean {
    return this.selectedCategory() === 'SMS';
  }

  protected selectedCategoryLabel(): string {
    return this.categories.find((category) => category.id === this.selectedCategory())?.label ?? 'Providers';
  }

  protected selectedCategoryEmptyTitle(): string {
    return CATEGORY_EMPTY_COPY[this.selectedCategory()].title;
  }

  protected selectedCategoryEmptyDescription(): string {
    return CATEGORY_EMPTY_COPY[this.selectedCategory()].description;
  }

  protected selectedCategoryEmptyChecklist(): readonly string[] {
    return CATEGORY_EMPTY_COPY[this.selectedCategory()].checklist;
  }

  protected selectedCategoryCreateLabel(): string {
    return CATEGORY_EMPTY_COPY[this.selectedCategory()].createLabel;
  }

  protected isSelectedSmsBalanceResult(): boolean {
    const result = this.smsActionResult();
    const provider = this.selectedProvider();
    return Boolean(result && provider && result.action === 'balance-check' && this.providerCategory(provider) === 'SMS');
  }

  viewPool(provider: AiProviderView): void { void this.router.navigate(['/master/provider-credit-pools'], { queryParams: { providerId: provider.id } }); }
  viewPolicy(provider: AiProviderView): void { void this.router.navigate(['/master/exchange-policies'], { queryParams: { providerId: provider.id } }); }

  private providerSearchText(provider: AiProviderView): string {
    return [
      provider.displayName,
      provider.providerName,
      provider.providerCode,
      provider.providerType,
      provider.category,
      provider.defaultModel,
      provider.status,
      provider.healthStatus,
      provider.maskedApiKey ? 'api key saved credentials saved' : 'api key not saved credentials missing',
      provider.active ? 'active enabled' : 'inactive disabled',
      provider.supportsImage ? 'image' : null,
      provider.supportsText ? 'text' : null,
      provider.supportsVideo ? 'video' : null,
      provider.supportsVoice ? 'audio voice' : null,
    ]
      .filter((value): value is string => Boolean(value))
      .join(' ')
      .toLowerCase();
  }

  private providerCategory(provider: AiProviderView): ProviderManagementCategory {
    const category = String(provider.providerCategory || provider.providerType || provider.category || 'AI').toUpperCase();
    if (category === 'NOTIFICATION' && String(provider.category).toUpperCase().includes('SMS')) return 'SMS';
    if (category === 'NOTIFICATION' && String(provider.category).toUpperCase().includes('EMAIL')) return 'EMAIL';
    if (category === 'SMS' || category === 'EMAIL' || category === 'STORAGE' || category === 'PAYMENT') return category;
    return 'AI';
  }

  private existingProviderByCode(providerCode: string): AiProviderView | null {
    const normalized = providerCode.trim().replaceAll('-', '_').replaceAll(' ', '_').toUpperCase();
    if (!normalized) {
      return null;
    }
    return this.store.providers().find((provider) => provider.providerCode.trim().toUpperCase() === normalized) ?? null;
  }

  private matchingProviders(queryValue: string): readonly AiProviderView[] {
    const query = queryValue.trim().toLowerCase();
    const providers = this.categoryProviders();
    if (!query) {
      return providers;
    }
    return providers.filter((provider) => this.providerSearchText(provider).includes(query));
  }

  private defaultProviderFormValue(): ProviderFormValue {
    const isSms = this.selectedCategory() === 'SMS';
    return {
      providerCode: '',
      providerName: '',
      apiKey: '',
      baseUrl: '',
      defaultModel: '',
      modelsEndpoint: '',
      modelsEndpointAuth: 'BEARER',
      apiKeyQueryParam: '',
      sendSmsEndpoint: '',
      balanceEndpoint: '',
      requestMethod: 'GET',
      senderId: '',
      otpLength: 6,
      otpExpiryMinutes: 5,
      resendCooldownSeconds: 60,
      maxAttempts: 3,
      balanceMonitoringEnabled: isSms,
      healthCheckEnabled: true,
      priority: 100,
      rateLimitPerMinute: 60,
      costMultiplier: 1,
      availableCreditBalance: null,
      openAiAdminApiKey: '',
      providerTopUpAmountUsd: null,
      providerTopUpDate: null,
      providerManualBalanceUsd: null,
      costSyncEnabled: false,
      metadataJson: '',
      active: true,
      supportsImage: !isSms,
      supportsText: !isSms,
      supportsVideo: false,
      supportsVoice: false,
      supportsOtp: isSms,
      supportsNotificationSms: isSms,
      supportsMarketingSms: false,
    };
  }

  private applyProviderTemplate(value: string): void {
    if (this.editingProvider() || this.selectedCategory() !== 'SMS') {
      return;
    }
    if (value.trim().toUpperCase() !== 'BULKSMSBD') {
      return;
    }
    this.providerForm.patchValue({
      providerName: 'BulkSMSBD',
      baseUrl: 'http://bulksmsbd.net/api',
      sendSmsEndpoint: '/smsapi',
      balanceEndpoint: '/getBalanceApi',
      requestMethod: 'GET',
      otpLength: 6,
      otpExpiryMinutes: 5,
      resendCooldownSeconds: 60,
      maxAttempts: 3,
      supportsOtp: true,
      supportsNotificationSms: true,
      supportsMarketingSms: false,
      metadataJson: JSON.stringify({
        provider: 'BulkSMSBD',
        country: 'Bangladesh',
        supportsUnicode: true,
        sendParams: ['api_key', 'senderid', 'type', 'number', 'message'],
        successCode: '202',
      }, null, 2),
    }, { emitEvent: false });
  }

  private normalizeOptionalNumber(value: unknown): number | null {
    if (value === null || value === undefined || value === '') {
      return null;
    }
    const normalized = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(normalized) ? normalized : null;
  }

  private creditBalanceLabel(value: number): string {
    return new Intl.NumberFormat('en-US', {
      currency: 'USD',
      maximumFractionDigits: 2,
      minimumFractionDigits: 2,
      style: 'currency',
    }).format(value);
  }

}

interface ProviderCategoryOption {
  readonly id: ProviderManagementCategory;
  readonly label: string;
  readonly description: string;
}

interface ProviderCategoryEmptyCopy {
  readonly title: string;
  readonly description: string;
  readonly checklist: readonly string[];
  readonly createLabel: string;
}

interface ProviderFormValue {
  readonly providerCode: string;
  readonly providerName: string;
  readonly apiKey: string;
  readonly baseUrl: string;
  readonly defaultModel: string;
  readonly modelsEndpoint: string;
  readonly modelsEndpointAuth: string;
  readonly apiKeyQueryParam: string;
  readonly sendSmsEndpoint: string;
  readonly balanceEndpoint: string;
  readonly requestMethod: string;
  readonly senderId: string;
  readonly otpLength: number;
  readonly otpExpiryMinutes: number;
  readonly resendCooldownSeconds: number;
  readonly maxAttempts: number;
  readonly balanceMonitoringEnabled: boolean;
  readonly healthCheckEnabled: boolean;
  readonly priority: number;
  readonly rateLimitPerMinute: number;
  readonly costMultiplier: number;
  readonly availableCreditBalance: number | null;
  readonly openAiAdminApiKey: string;
  readonly providerTopUpAmountUsd: number | null;
  readonly providerTopUpDate: string | null;
  readonly providerManualBalanceUsd: number | null;
  readonly costSyncEnabled: boolean;
  readonly metadataJson: string;
  readonly active: boolean;
  readonly supportsImage: boolean;
  readonly supportsText: boolean;
  readonly supportsVideo: boolean;
  readonly supportsVoice: boolean;
  readonly supportsOtp: boolean;
  readonly supportsNotificationSms: boolean;
  readonly supportsMarketingSms: boolean;
}
