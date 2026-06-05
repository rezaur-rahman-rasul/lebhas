import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ModalComponent } from '@app/shared/components/app-dialog/app-dialog';
import { AiProviderCredentialView, AiProviderView } from '../../models/provider-credit-exchange.models';
import { MaskedSecretFieldComponent } from '../../components/masked-secret-field/masked-secret-field';
import { ProviderCardComponent } from '../../components/provider-card/provider-card';
import { ProviderCredentialFormComponent } from '../../components/provider-credential-form/provider-credential-form';
import { ProviderSettingsStore } from '../../state/providerSettings.store';

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
  protected readonly providerDialogOpen = signal(false);
  protected readonly credentialDialogOpen = signal(false);
  protected readonly editingProvider = signal<AiProviderView | null>(null);
  protected readonly editingCredential = signal<AiProviderCredentialView | null>(null);
  protected readonly modelsDialogOpen = signal(false);

  protected readonly providerForm = new FormGroup({
    providerCode: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    providerName: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    apiKey: new FormControl('', { nonNullable: true }),
    defaultModel: new FormControl('', { nonNullable: true }),
    modelsEndpoint: new FormControl('', { nonNullable: true }),
    modelsEndpointAuth: new FormControl('BEARER', { nonNullable: true }),
    apiKeyQueryParam: new FormControl('', { nonNullable: true }),
    priority: new FormControl(100, { nonNullable: true, validators: [Validators.min(0)] }),
    rateLimitPerMinute: new FormControl(60, { nonNullable: true, validators: [Validators.min(1)] }),
    costMultiplier: new FormControl(1, { nonNullable: true, validators: [Validators.min(0.0001)] }),
    metadataJson: new FormControl('', { nonNullable: true }),
    active: new FormControl(true, { nonNullable: true }),
    supportsImage: new FormControl(true, { nonNullable: true }),
    supportsText: new FormControl(true, { nonNullable: true }),
    supportsVideo: new FormControl(false, { nonNullable: true }),
    supportsVoice: new FormControl(false, { nonNullable: true }),
  });

  protected readonly selectedProvider = this.store.selectedProvider;
  protected readonly selectedProviderCredentials = computed(() => this.store.credentials());
  protected readonly modelsJsonText = computed(() => JSON.stringify(this.store.modelsJson()?.modelsJson ?? {}, null, 2));

  constructor() {
    effect(() => void this.store.loadProviders());
  }

  openProvider(provider?: AiProviderView): void {
    this.editingProvider.set(provider ?? null);
    this.providerForm.reset(provider ? {
      providerCode: provider.providerCode,
      providerName: provider.providerName,
      apiKey: '',
      defaultModel: provider.defaultModel ?? '',
      modelsEndpoint: provider.modelsEndpoint ?? '',
      modelsEndpointAuth: provider.modelsEndpointAuth ?? 'BEARER',
      apiKeyQueryParam: provider.apiKeyQueryParam ?? '',
      priority: provider.priority ?? 100,
      rateLimitPerMinute: provider.rateLimitPerMinute ?? 60,
      costMultiplier: Number(provider.costMultiplier ?? 1),
      metadataJson: provider.metadataJson ?? '',
      active: provider.active,
      supportsImage: provider.supportsImage,
      supportsText: provider.supportsText,
      supportsVideo: provider.supportsVideo,
      supportsVoice: provider.supportsVoice,
    } : { providerCode: '', providerName: '', apiKey: '', defaultModel: '', modelsEndpoint: '', modelsEndpointAuth: 'BEARER', apiKeyQueryParam: '', priority: 100, rateLimitPerMinute: 60, costMultiplier: 1, metadataJson: '', active: true, supportsImage: true, supportsText: true, supportsVideo: false, supportsVoice: false });
    this.providerDialogOpen.set(true);
  }

  async saveProvider(): Promise<void> {
    if (!this.editingProvider() && !this.providerForm.controls.apiKey.value.trim()) {
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
    const ok = await this.store.saveProvider(this.providerForm.getRawValue(), this.editingProvider()?.id);
    if (ok) this.providerDialogOpen.set(false);
  }

  selectProvider(provider: AiProviderView): void {
    this.store.setSelectedProvider(provider.id);
    this.editingCredential.set(null);
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
    if (!confirm(`Delete ${name}? This will deactivate its saved credentials and remove it from Provider Settings.`)) return;
    await this.store.deleteProvider(provider);
  }

  async test(provider: AiProviderView): Promise<void> {
    await this.store.testConnection(provider);
  }

  async viewModelsJson(provider: AiProviderView): Promise<void> {
    const ok = await this.store.loadModelsJson(provider);
    if (ok) this.modelsDialogOpen.set(true);
  }

  closeModelsDialog(): void {
    this.modelsDialogOpen.set(false);
    this.store.clearModelsJson();
  }

  viewPool(provider: AiProviderView): void { void this.router.navigate(['/master/provider-credit-pools'], { queryParams: { providerId: provider.id } }); }
  viewPolicy(provider: AiProviderView): void { void this.router.navigate(['/master/exchange-policies'], { queryParams: { providerId: provider.id } }); }
}
