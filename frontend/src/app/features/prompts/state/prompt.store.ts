import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { Asset } from '@app/features/assets/models/asset.models';
import {
  EnhancePromptRequest,
  EnhancePromptResponse,
  Prompt,
  PromptSuggestionsRequest,
  PromptSuggestionsResponse,
  SuggestionType,
} from '../models';
import { PromptStatus } from '../models/prompt.enums';
import { PromptApiService } from '../services/prompt-api.service';
import { validateEnhanceRequest } from '../services/prompt.validation';
import {
  DEFAULT_PROMPT_BUILDER_SETTINGS,
  failureResult,
  fieldErrorsResult,
  PromptActionResult,
  PromptBuilderSettings,
  SelectedProjectContext,
  successResult,
} from './prompt.state';

@Injectable({ providedIn: 'root' })
export class PromptStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly permissions = inject(PermissionStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly promptApi = inject(PromptApiService);

  private readonly promptsSignal = signal<readonly Prompt[]>([]);
  private readonly selectedProjectContextSignal = signal<SelectedProjectContext | null>(null);
  private readonly selectedAssetsSignal = signal<readonly Asset[]>([]);
  private readonly sourcePromptSignal = signal('');
  private readonly enhancedPromptSignal = signal<EnhancePromptResponse | null>(null);
  private readonly suggestionsSignal = signal<PromptSuggestionsResponse | null>(null);
  private readonly builderSettingsSignal = signal<PromptBuilderSettings>(DEFAULT_PROMPT_BUILDER_SETTINGS);
  private readonly aiLoadingSignal = signal(false);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly prompts = this.promptsSignal.asReadonly();
  readonly selectedProjectContext = this.selectedProjectContextSignal.asReadonly();
  readonly selectedAssets = this.selectedAssetsSignal.asReadonly();
  readonly sourcePrompt = this.sourcePromptSignal.asReadonly();
  readonly enhancedPrompt = this.enhancedPromptSignal.asReadonly();
  readonly suggestions = this.suggestionsSignal.asReadonly();
  readonly builderSettings = this.builderSettingsSignal.asReadonly();
  readonly aiLoading = this.aiLoadingSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly hasProjectContext = computed(() => Boolean(this.selectedProjectContextSignal()?.projectId));
  readonly hasSourcePrompt = computed(() => this.sourcePromptSignal().trim().length > 0);
  readonly canEnhancePrompt = computed(
    () => this.permissions.canEnhancePrompt() && this.sourcePromptSignal().trim().length >= 5,
  );
  readonly hasEnhancedPrompt = computed(() => Boolean(this.enhancedPromptSignal()?.enhancedPrompt));
  readonly hasSuggestions = computed(() => {
    const suggestions = this.suggestionsSignal();
    if (!suggestions) {
      return false;
    }

    return [
      suggestions.ctaSuggestions,
      suggestions.headlineSuggestions,
      suggestions.offerSuggestions,
      suggestions.creativeAngleSuggestions,
      suggestions.campaignToneSuggestions,
      suggestions.businessCategorySuggestions,
    ].some((items) => items.length > 0);
  });
  readonly selectedAssetCount = computed(() => this.selectedAssetsSignal().length);

  readonly canUsePromptBuilder = this.permissions.canUsePromptBuilder;
  readonly canViewTemplates = this.permissions.canViewPromptTemplates;
  readonly canViewHistory = this.permissions.canViewPromptHistory;

  setSelectedProjectContext(context: SelectedProjectContext | null): void {
    this.selectedProjectContextSignal.set(context);
  }

  setSourcePrompt(prompt: string): void {
    this.sourcePromptSignal.set(prompt);
  }

  patchBuilderSettings(settings: Partial<PromptBuilderSettings>): void {
    this.builderSettingsSignal.update((current) => ({ ...current, ...settings }));
  }

  selectAssets(assets: readonly Asset[]): void {
    this.selectedAssetsSignal.set([...assets]);
  }

  toggleAsset(asset: Asset): void {
    this.selectedAssetsSignal.update((current) => {
      const exists = current.some((item) => item.id === asset.id);
      return exists ? current.filter((item) => item.id !== asset.id) : [...current, asset];
    });
  }

  removeSelectedAsset(assetId: string): void {
    this.selectedAssetsSignal.update((assets) => assets.filter((asset) => asset.id !== assetId));
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  resetPrompt(): void {
    this.sourcePromptSignal.set('');
    this.enhancedPromptSignal.set(null);
    this.suggestionsSignal.set(null);
    this.selectedAssetsSignal.set([]);
    this.errorSignal.set(null);
  }

  async enhancePrompt(): Promise<PromptActionResult> {
    const context = this.selectedProjectContextSignal();
    const settings = this.builderSettingsSignal();
    const sourcePrompt = this.sourcePromptSignal();

    const fieldErrors = validateEnhanceRequest(context?.projectId, sourcePrompt, settings);
    if (Object.keys(fieldErrors).length > 0) {
      return fieldErrorsResult(fieldErrors);
    }

    if (!context) {
      return fieldErrorsResult({ projectId: 'Select a project before using prompt intelligence.' });
    }

    if (!this.permissions.canEnhancePrompt()) {
      return failureResult('You do not have permission to enhance prompts.');
    }

    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return fieldErrorsResult({ workspaceId: 'Select a workspace before enhancing prompts.' });
    }

    this.aiLoadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const payload = this.buildIntelligencePayload(sourcePrompt, settings);
      const response = await this.promptApi.enhancePrompt(
        workspaceId,
        context.projectId,
        payload,
        this.requestContext(),
      );

      this.enhancedPromptSignal.set(response);
      this.upsertDraftPrompt(context, sourcePrompt, response.enhancedPrompt);
      this.notifications.success('Prompt enhanced', 'A stronger campaign prompt is ready to review.');
      return successResult();
    } catch (error) {
      return this.handleAiFailure(error, 'Prompt enhancement failed');
    } finally {
      this.aiLoadingSignal.set(false);
    }
  }

  async getSuggestions(suggestionTypes: readonly SuggestionType[]): Promise<PromptActionResult> {
    const context = this.selectedProjectContextSignal();
    const settings = this.builderSettingsSignal();
    const sourcePrompt = this.sourcePromptSignal();

    const fieldErrors = validateEnhanceRequest(context?.projectId, sourcePrompt, settings);
    if (Object.keys(fieldErrors).length > 0) {
      return fieldErrorsResult(fieldErrors);
    }

    if (!suggestionTypes.length) {
      return fieldErrorsResult({ suggestionTypes: 'Select at least one suggestion type.' });
    }

    if (!context) {
      return fieldErrorsResult({ projectId: 'Select a project before requesting suggestions.' });
    }

    if (!this.permissions.canUsePromptBuilder()) {
      return failureResult('You do not have permission to generate prompt suggestions.');
    }

    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return fieldErrorsResult({ workspaceId: 'Select a workspace before requesting suggestions.' });
    }

    this.aiLoadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const payload: PromptSuggestionsRequest = {
        ...this.buildIntelligencePayload(sourcePrompt, settings),
        suggestionTypes: [...suggestionTypes],
      };

      const response = await this.promptApi.generateSuggestions(
        workspaceId,
        context.projectId,
        payload,
        this.requestContext(),
      );

      this.suggestionsSignal.set(response);
      this.notifications.success('Suggestions ready', 'Prompt guidance is ready to review.');
      return successResult();
    } catch (error) {
      return this.handleAiFailure(error, 'Prompt suggestions failed');
    } finally {
      this.aiLoadingSignal.set(false);
    }
  }

  private buildIntelligencePayload(
    sourcePrompt: string,
    settings: PromptBuilderSettings,
  ): EnhancePromptRequest {
    return {
      customPrompt: sourcePrompt.trim(),
      assetIds: this.selectedAssetsSignal().map((asset) => asset.id),
      templateId: settings.templateId,
      businessType: settings.businessType,
      campaignObjective: settings.campaignObjective,
      platform: settings.platform,
      creativeStyle: settings.creativeStyle,
      language: settings.language,
      tone: settings.tone,
      targetAudience: settings.targetAudience.trim() || null,
      offerDetails: settings.offerDetails.trim() || null,
      ctaPreference: settings.ctaPreference.trim() || null,
      useBrandProfile: settings.useBrandProfile,
    };
  }

  private upsertDraftPrompt(
    context: SelectedProjectContext,
    sourcePrompt: string,
    enhancedText: string,
  ): void {
    const settings = this.builderSettingsSignal();
    const draft: Prompt = {
      id: `draft-${context.projectId}`,
      workspaceId: context.workspaceId,
      brandId: context.brandId,
      productServiceId: context.productServiceId,
      projectCampaignId: context.projectId,
      createdBy: this.auth.currentUser()?.id ?? '',
      sourcePrompt,
      enhancedPrompt: enhancedText,
      language: settings.language,
      platform: settings.platform,
      campaignObjective: settings.campaignObjective,
      tone: settings.tone,
      targetAudience: settings.targetAudience.trim() || null,
      status: 'ENHANCED' satisfies PromptStatus,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    this.promptsSignal.update((prompts) => {
      const index = prompts.findIndex((item) => item.projectCampaignId === context.projectId);
      if (index === -1) {
        return [draft, ...prompts];
      }

      return prompts.map((item, itemIndex) => (itemIndex === index ? draft : item));
    });
  }

  private handleAiFailure(error: unknown, title: string): PromptActionResult {
    const normalized = normalizeHttpError(error);
    this.errorSignal.set(normalized.message);
    this.notifications.error(title, normalized.message);
    return failureResult(normalized.message);
  }

  private resolveWorkspaceId(): string | null {
    return this.auth.activeWorkspaceId();
  }

  private requestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}
