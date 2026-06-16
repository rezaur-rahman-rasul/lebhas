import { Injectable, computed, inject, signal } from '@angular/core';
import { normalizeHttpError } from '@app/core/api/http-error';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { AiProviderCredentialView, AiProviderView, CreateProviderCredentialRequest, CreateProviderRequest, OpenAiCostSyncResult, ProviderManagementCategory, ProviderModelsJsonView, SmsProviderActionResult } from '../models/provider-credit-exchange.models';
import { ProviderSettingsApiService } from '../services/provider-settings-api.service';

@Injectable({ providedIn: 'root' })
export class ProviderSettingsStore {
  private readonly api = inject(ProviderSettingsApiService);
  private readonly notifications = inject(NotificationStateService);

  private readonly providersSignal = signal<readonly AiProviderView[]>([]);
  private readonly selectedProviderIdSignal = signal<string | null>(null);
  private readonly credentialsSignal = signal<readonly AiProviderCredentialView[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly savingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly modelsJsonSignal = signal<ProviderModelsJsonView | null>(null);
  private readonly modelsJsonLoadingSignal = signal(false);

  readonly providers = this.providersSignal.asReadonly();
  readonly selectedProviderId = this.selectedProviderIdSignal.asReadonly();
  readonly credentials = this.credentialsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly saving = this.savingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly modelsJson = this.modelsJsonSignal.asReadonly();
  readonly modelsJsonLoading = this.modelsJsonLoadingSignal.asReadonly();

  readonly activeProviders = computed(() => this.providersSignal().filter((provider) => provider.active));
  readonly selectedProvider = computed(() => this.providersSignal().find((provider) => provider.id === this.selectedProviderIdSignal()) ?? this.providersSignal()[0] ?? null);
  readonly providerOptions = computed(() => this.providersSignal().map((provider) => ({ id: provider.id, label: provider.providerName || provider.providerCode })));

  setSelectedProvider(providerId: string | null): void {
    const currentProviderId = this.selectedProviderIdSignal();
    if (currentProviderId === providerId) {
      if (providerId && this.credentialsSignal().length === 0) {
        void this.loadCredentials(providerId);
      }
      return;
    }
    this.selectedProviderIdSignal.set(providerId);
    this.credentialsSignal.set([]);
    if (providerId) {
      void this.loadCredentials(providerId);
    }
  }

  async loadProviders(category?: ProviderManagementCategory | string | null): Promise<void> {
    await this.run(async () => {
      const providers = await this.api.getProviders(category);
      this.providersSignal.set(providers);
      const selectedId = this.selectedProviderIdSignal();
      const selectedStillVisible = selectedId && providers.some((provider) => provider.id === selectedId);
      this.selectedProviderIdSignal.set(selectedStillVisible ? selectedId : providers[0]?.id ?? null);
      if (this.selectedProviderIdSignal()) {
        await this.loadCredentials(this.selectedProviderIdSignal()!);
      } else {
        this.credentialsSignal.set([]);
      }
    });
  }

  async refreshProvidersQuietly(category?: ProviderManagementCategory | string | null): Promise<void> {
    try {
      const providers = await this.api.getProviders(category);
      this.providersSignal.set(providers);
      const selectedId = this.selectedProviderIdSignal();
      if (!selectedId || providers.some((provider) => provider.id === selectedId)) {
        return;
      }
      const nextProviderId = providers[0]?.id ?? null;
      this.selectedProviderIdSignal.set(nextProviderId);
      this.credentialsSignal.set([]);
      if (nextProviderId) {
        await this.loadCredentials(nextProviderId);
      }
    } catch {
      // Background refresh must not replace the visible page state with a transient polling error.
    }
  }

  async saveProvider(payload: CreateProviderRequest, providerId?: string): Promise<boolean> {
    return this.save(async () => {
      const provider = providerId ? await this.api.updateProvider(providerId, payload) : await this.api.createProvider(payload);
      this.providersSignal.update((items) => upsert(items, provider));
      this.selectedProviderIdSignal.set(provider.id);
      this.credentialsSignal.set(await this.api.getCredentials(provider.id));
      this.notifications.success('Provider saved successfully.', 'Provider saved successfully.');
    }, 'Provider could not be saved.');
  }

  async loadCredentials(providerId: string): Promise<void> {
    await this.run(async () => this.credentialsSignal.set(await this.api.getCredentials(providerId)));
  }

  async saveCredential(providerId: string, payload: CreateProviderCredentialRequest, credentialId?: string): Promise<boolean> {
    return this.save(async () => {
      const credential = credentialId
        ? await this.api.updateCredential(providerId, credentialId, payload)
        : await this.api.createCredential(providerId, payload);
      this.credentialsSignal.update((items) => upsert(items, credential));
      this.providersSignal.update((items) =>
        items.map((item) =>
          item.id === providerId
            ? { ...item, maskedApiKey: credential.maskedApiKey, credentialConfigured: true, updatedAt: credential.updatedAt }
            : item,
        ),
      );
      this.notifications.success('Credential saved securely.', 'Credential saved securely.');
    }, 'Credential could not be saved.');
  }

  async deleteCredential(providerId: string, credentialId: string): Promise<boolean> {
    return this.save(async () => {
      await this.api.deleteCredential(providerId, credentialId);
      this.credentialsSignal.update((items) => items.filter((item) => item.id !== credentialId));
      this.notifications.success('Credential saved securely.', 'Credential deactivated securely.');
    }, 'Credential could not be saved.');
  }

  async toggleProvider(provider: AiProviderView): Promise<boolean> {
    return this.save(async () => {
      const updated = await this.api.toggleProvider(provider.id, !provider.active);
      this.providersSignal.update((items) => upsert(items, updated));
      if (this.selectedProviderIdSignal() === updated.id) {
        this.credentialsSignal.set(await this.api.getCredentials(updated.id));
      }
      this.notifications.success(updated.active ? 'Provider enabled.' : 'Provider disabled.', 'Provider status updated.');
    }, 'Provider status could not be updated.');
  }

  async deleteProvider(provider: AiProviderView): Promise<boolean> {
    return this.save(async () => {
      await this.api.deleteProvider(provider.id);
      this.providersSignal.update((items) => items.filter((item) => item.id !== provider.id));
      const nextProvider = this.providersSignal()[0] ?? null;
      this.selectedProviderIdSignal.set(nextProvider?.id ?? null);
      this.credentialsSignal.set(nextProvider ? await this.api.getCredentials(nextProvider.id) : []);
      this.notifications.success('Provider deleted.', 'Provider and saved credentials were deactivated.');
    }, 'Provider could not be deleted.');
  }

  async testConnection(provider: AiProviderView): Promise<boolean> {
    return this.save(async () => {
      await this.api.testConnection(provider.id);
      this.notifications.info('Connection test completed.', 'Provider health was refreshed.');
      await this.loadProviders();
    }, 'Provider connection could not be tested.');
  }

  async loadModelsJson(provider: AiProviderView): Promise<boolean> {
    this.modelsJsonLoadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      this.modelsJsonSignal.set(await this.api.getModelsJson(provider.id));
      this.notifications.info('Models JSON loaded.', 'OpenAI /v1/models response was loaded.');
      return true;
    } catch (error) {
      const message = normalizeHttpError(error).message || 'OpenAI models JSON could not be loaded.';
      this.errorSignal.set(message);
      this.notifications.error('Models JSON could not be loaded.', message);
      return false;
    } finally {
      this.modelsJsonLoadingSignal.set(false);
    }
  }

  async testSms(provider: AiProviderView, mobileNumber: string, message?: string): Promise<SmsProviderActionResult | null> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);
    try {
      const result = await this.api.testSms(provider.id, mobileNumber, message);
      this.notifications.info(result.success ? 'Test SMS sent.' : 'Test SMS failed.', result.message || 'SMS provider test completed.');
      return result;
    } catch (error) {
      const message = normalizeHttpError(error).message || 'Test SMS could not be sent.';
      this.errorSignal.set(message);
      this.notifications.error('Test SMS could not be sent.', message);
      return null;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async checkSmsBalance(provider: AiProviderView): Promise<SmsProviderActionResult | null> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);
    try {
      const result = await this.api.checkSmsBalance(provider.id);
      this.notifications.info(result.success ? 'SMS balance checked.' : 'SMS balance check failed.', result.message || 'SMS balance request completed.');
      return result;
    } catch (error) {
      const message = normalizeHttpError(error).message || 'SMS balance could not be checked.';
      this.errorSignal.set(message);
      this.notifications.error('SMS balance could not be checked.', message);
      return null;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async checkProviderBalance(provider: AiProviderView): Promise<SmsProviderActionResult | null> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);
    try {
      const result = await this.api.checkProviderBalance(provider.id);
      this.notifications.info(result.success ? 'Provider balance checked.' : 'Provider balance check failed.', result.message || 'Provider balance request completed.');
      if (result.success) {
        await this.loadProviders();
      }
      return result;
    } catch (error) {
      const message = normalizeHttpError(error).message || 'Provider balance could not be checked.';
      this.errorSignal.set(message);
      this.notifications.error('Provider balance could not be checked.', message);
      return null;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async syncOpenAiCosts(provider: AiProviderView): Promise<OpenAiCostSyncResult | null> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);
    try {
      const result = await this.api.syncOpenAiCosts(provider.id);
      this.notifications.info(result.success ? 'OpenAI costs synced.' : 'OpenAI cost sync failed.', result.message || 'Cost sync completed.');
      await this.loadProviders();
      return result;
    } catch (error) {
      const message = normalizeHttpError(error).message || 'OpenAI costs could not be synced.';
      this.errorSignal.set(message);
      this.notifications.error('OpenAI costs could not be synced.', message);
      return null;
    } finally {
      this.savingSignal.set(false);
    }
  }

  clearModelsJson(): void {
    this.modelsJsonSignal.set(null);
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      await action();
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message || 'Provider settings could not be loaded.');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private async save(action: () => Promise<void>, fallback: string): Promise<boolean> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);
    try {
      await action();
      return true;
    } catch (error) {
      const message = normalizeHttpError(error).message || fallback;
      this.errorSignal.set(message.includes('apiKey') ? fallback : message);
      this.notifications.error(fallback, fallback);
      return false;
    } finally {
      this.savingSignal.set(false);
    }
  }
}

function upsert<T extends { readonly id: string }>(items: readonly T[], item: T): readonly T[] {
  return items.some((entry) => entry.id === item.id) ? items.map((entry) => entry.id === item.id ? item : entry) : [...items, item];
}
