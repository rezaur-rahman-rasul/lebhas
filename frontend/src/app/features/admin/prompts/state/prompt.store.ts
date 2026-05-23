import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { normalizeHttpError } from '@app/core/api/http-error';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { Asset, DEFAULT_ASSET_FILTERS } from '@app/features/admin/assets/models/asset.models';
import { AssetService } from '@app/features/admin/assets/services/asset.service';
import { BrandProfile } from '@app/features/admin/workspace/models/brand-profile.models';
import { BrandProfileService } from '@app/features/admin/workspace/services/brand-profile.service';
import { ApiError } from '@app/shared/models/api-response.model';
import {
  DEFAULT_PROMPT_HISTORY_FILTERS,
  DEFAULT_PROMPT_HISTORY_PAGINATION,
  DEFAULT_PROMPT_TEMPLATE_FILTERS,
  DEFAULT_PROMPT_TEMPLATE_PAGINATION,
  EMPTY_PROMPT_SUGGESTIONS,
  PromptActionResult,
  PromptEnhanceRequest,
  PromptEnhanceResponse,
  PromptHistory,
  PromptHistoryFilter,
  PromptLanguage,
  PromptPagination,
  PromptRewriteRequest,
  PromptRewriteResponse,
  PromptSuggestionListResponse,
  PromptSuggestionsRequest,
  PromptSuggestionsResponse,
  PromptTemplate,
  PromptTemplateFilter,
  PromptTemplatePayload,
  SuggestionType,
  normalizePromptText,
} from '../models/prompt.models';
import { PromptService } from '../services/prompt.service';

@Injectable({ providedIn: 'root' })
export class PromptStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly promptService = inject(PromptService);
  private readonly brandProfileService = inject(BrandProfileService);
  private readonly assetService = inject(AssetService);

  private readonly templatesSignal = signal<readonly PromptTemplate[]>([]);
  private readonly historySignal = signal<readonly PromptHistory[]>([]);
  private readonly selectedHistorySignal = signal<PromptHistory | null>(null);
  private readonly currentPromptSignal = signal('');
  private readonly enhancedPromptSignal = signal<PromptEnhanceResponse | null>(null);
  private readonly rewriteResultSignal = signal<PromptRewriteResponse | null>(null);
  private readonly suggestionsSignal = signal<PromptSuggestionsResponse | null>(null);
  private readonly selectedAssetsSignal = signal<readonly Asset[]>([]);
  private readonly availableAssetsSignal = signal<readonly Asset[]>([]);
  private readonly brandProfileSignal = signal<BrandProfile | null>(null);
  private readonly brandContextEnabledSignal = signal(true);
  private readonly currentLanguageSignal = signal<PromptLanguage | null>('ENGLISH');
  private readonly templateFiltersSignal = signal<PromptTemplateFilter>(
    DEFAULT_PROMPT_TEMPLATE_FILTERS,
  );
  private readonly historyFiltersSignal = signal<PromptHistoryFilter>(
    DEFAULT_PROMPT_HISTORY_FILTERS,
  );
  private readonly templatePaginationSignal = signal<PromptPagination>(
    DEFAULT_PROMPT_TEMPLATE_PAGINATION,
  );
  private readonly historyPaginationSignal = signal<PromptPagination>(
    DEFAULT_PROMPT_HISTORY_PAGINATION,
  );
  private readonly promptLoadingSignal = signal(false);
  private readonly promptErrorSignal = signal<string | null>(null);

  readonly templates = this.templatesSignal.asReadonly();
  readonly history = this.historySignal.asReadonly();
  readonly selectedHistory = this.selectedHistorySignal.asReadonly();
  readonly currentPrompt = this.currentPromptSignal.asReadonly();
  readonly enhancedPrompt = this.enhancedPromptSignal.asReadonly();
  readonly rewriteResult = this.rewriteResultSignal.asReadonly();
  readonly suggestions = this.suggestionsSignal.asReadonly();
  readonly selectedAssets = this.selectedAssetsSignal.asReadonly();
  readonly availableAssets = this.availableAssetsSignal.asReadonly();
  readonly brandProfile = this.brandProfileSignal.asReadonly();
  readonly brandContextEnabled = this.brandContextEnabledSignal.asReadonly();
  readonly currentLanguage = this.currentLanguageSignal.asReadonly();
  readonly templateFilters = this.templateFiltersSignal.asReadonly();
  readonly historyFilters = this.historyFiltersSignal.asReadonly();
  readonly templatePagination = this.templatePaginationSignal.asReadonly();
  readonly historyPagination = this.historyPaginationSignal.asReadonly();
  readonly promptLoading = this.promptLoadingSignal.asReadonly();
  readonly promptError = this.promptErrorSignal.asReadonly();

  readonly hasPrompt = computed(() => normalizePromptText(this.currentPromptSignal()).length > 0);
  readonly canEnhancePrompt = computed(
    () =>
      this.canUsePromptBuilder() &&
      normalizePromptText(this.currentPromptSignal()).length >= 5,
  );
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
  readonly isBanglaSelected = computed(() => this.currentLanguageSignal() === 'BANGLA');
  readonly canManageTemplates = computed(() => this.hasPermission('PROMPT_TEMPLATE_MANAGE'));
  readonly canViewTemplates = computed(
    () =>
      this.canManageTemplates() || this.hasPermission('PROMPT_TEMPLATE_VIEW'),
  );
  readonly canUsePromptBuilder = computed(() =>
    this.hasPermission('PROMPT_INTELLIGENCE_USE'),
  );
  readonly canViewHistory = computed(() => this.hasPermission('PROMPT_HISTORY_VIEW'));
  readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));

  async loadBuilderContext(assetSearch = ''): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.runLoader(async () => {
      const operations: Promise<unknown>[] = [this.loadBrandProfile(workspaceId)];

      if (this.canViewTemplates()) {
        operations.push(this.fetchTemplates(workspaceId));
      } else {
        this.templatesSignal.set([]);
      }

      if (this.auth.hasPermission('ASSET_VIEW')) {
        operations.push(this.fetchAvailableAssets(workspaceId, assetSearch));
      } else {
        this.availableAssetsSignal.set([]);
      }

      await Promise.all(operations);
    });
  }

  async loadTemplates(filters?: PromptTemplateFilter): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    if (filters) {
      this.templateFiltersSignal.set(filters);
    }

    await this.runLoader(async () => {
      await this.fetchTemplates(workspaceId);
    });
  }

  async loadHistory(filters?: PromptHistoryFilter, page?: number): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    if (filters) {
      this.historyFiltersSignal.set(filters);
    }

    if (page !== undefined) {
      this.historyPaginationSignal.update((current) => ({ ...current, page }));
    }

    await this.runLoader(async () => {
      const result = await firstValueFrom(
        this.promptService.listHistory(
          workspaceId,
          this.historyFiltersSignal(),
          this.historyPaginationSignal().page,
          this.historyPaginationSignal().size,
          this.requestContext(),
        ),
      );

      this.historySignal.set(result.items);
      this.historyPaginationSignal.set(result.pagination);
      this.syncSelectedHistory();
    });
  }

  async loadHistoryDetail(historyId: string): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.runLoader(async () => {
      const history = await firstValueFrom(
        this.promptService.getHistory(workspaceId, historyId, this.requestContext()),
      );
      this.selectedHistorySignal.set(history);
      this.historySignal.update((items) => upsertHistory(items, history));
    });
  }

  async searchAvailableAssets(search: string): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId || !this.auth.hasPermission('ASSET_VIEW')) {
      return;
    }

    await this.runLoader(async () => {
      await this.fetchAvailableAssets(workspaceId, search);
    });
  }

  setCurrentPrompt(prompt: string): void {
    this.currentPromptSignal.set(prompt);
  }

  setCurrentLanguage(language: PromptLanguage | null): void {
    this.currentLanguageSignal.set(language);
  }

  setBrandContextEnabled(enabled: boolean): void {
    this.brandContextEnabledSignal.set(enabled);
  }

  setSelectedHistory(history: PromptHistory | null): void {
    this.selectedHistorySignal.set(history);
  }

  selectTemplate(template: PromptTemplate | null): void {
    if (!template) {
      return;
    }

    this.currentPromptSignal.set(template.templateText);
    this.currentLanguageSignal.set(template.language);
  }

  toggleAsset(asset: Asset): void {
    this.selectedAssetsSignal.update((assets) => {
      if (assets.some((item) => item.id === asset.id)) {
        return assets.filter((item) => item.id !== asset.id);
      }

      return [...assets, asset];
    });
  }

  removeSelectedAsset(assetId: string): void {
    this.selectedAssetsSignal.update((assets) => assets.filter((asset) => asset.id !== assetId));
  }

  clearSelections(): void {
    this.selectedAssetsSignal.set([]);
    this.enhancedPromptSignal.set(null);
    this.rewriteResultSignal.set(null);
    this.suggestionsSignal.set(null);
  }

  async createTemplate(payload: PromptTemplatePayload): Promise<PromptActionResult> {
    return this.saveTemplateInternal(payload);
  }

  async updateTemplate(templateId: string, payload: PromptTemplatePayload): Promise<PromptActionResult> {
    return this.saveTemplateInternal(payload, templateId);
  }

  async deleteTemplate(templateId: string): Promise<PromptActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.promptLoadingSignal.set(true);
      this.promptErrorSignal.set(null);
      await firstValueFrom(this.promptService.deleteTemplate(workspaceId, templateId));
      this.templatesSignal.update((templates) =>
        templates.filter((template) => template.id !== templateId),
      );
      this.notifications.success('Template deleted', 'The prompt template has been removed.');
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.promptLoadingSignal.set(false);
    }
  }

  async enhancePrompt(payload: PromptEnhanceRequest): Promise<PromptActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.promptLoadingSignal.set(true);
      this.promptErrorSignal.set(null);
      const response = await firstValueFrom(this.promptService.enhancePrompt(workspaceId, payload));
      this.enhancedPromptSignal.set(response);
      this.notifications.success('Prompt enhanced', 'Prompt intelligence generated a stronger version.');
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.promptLoadingSignal.set(false);
    }
  }

  async rewritePrompt(payload: PromptRewriteRequest): Promise<PromptActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.promptLoadingSignal.set(true);
      this.promptErrorSignal.set(null);
      const response = await firstValueFrom(this.promptService.rewritePrompt(workspaceId, payload));
      this.rewriteResultSignal.set(response);
      this.notifications.success('Prompt rewritten', 'Alternative prompt variations are ready.');
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.promptLoadingSignal.set(false);
    }
  }

  async generateSuggestions(payload: PromptSuggestionsRequest): Promise<PromptActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.promptLoadingSignal.set(true);
      this.promptErrorSignal.set(null);
      const response = await firstValueFrom(
        this.promptService.generateSuggestions(workspaceId, payload),
      );
      this.suggestionsSignal.set(response);
      this.notifications.success('Suggestions ready', 'Prompt guidance is ready to review.');
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.promptLoadingSignal.set(false);
    }
  }

  async generateSpecificSuggestions(
    type:
      | 'cta'
      | 'headline'
      | 'offer'
      | 'angle'
      | 'tone'
      | 'category',
    payload: PromptSuggestionsRequest,
  ): Promise<PromptActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.promptLoadingSignal.set(true);
      this.promptErrorSignal.set(null);
      const response = await firstValueFrom(this.suggestionRequestForType(workspaceId, type, payload));
      this.mergeSuggestionList(response);
      this.notifications.success('Suggestions ready', 'Targeted prompt suggestions have been updated.');
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.promptLoadingSignal.set(false);
    }
  }

  private async fetchTemplates(workspaceId: string): Promise<void> {
    const templates = await firstValueFrom(
      this.promptService.listTemplates(workspaceId, this.templateFiltersSignal(), this.requestContext()),
    );

    this.templatesSignal.set(templates);
    this.templatePaginationSignal.set({
      page: 0,
      size: templates.length || DEFAULT_PROMPT_TEMPLATE_PAGINATION.size,
      totalItems: templates.length,
      totalPages: templates.length ? 1 : 0,
      first: true,
      last: true,
    });
  }

  private async loadBrandProfile(workspaceId: string): Promise<void> {
    try {
      const brandProfile = await firstValueFrom(
        this.brandProfileService.getBrandProfile(workspaceId, this.requestContext()),
      );
      this.brandProfileSignal.set(brandProfile);
    } catch {
      this.brandProfileSignal.set(null);
    }
  }

  private async fetchAvailableAssets(workspaceId: string, search: string): Promise<void> {
    try {
      const result = await firstValueFrom(
        this.assetService.listAssets(
          workspaceId,
          {
            ...DEFAULT_ASSET_FILTERS,
            search: search.trim(),
          },
          0,
          12,
          this.requestContext(),
        ),
      );

      this.availableAssetsSignal.set(
        result.items.filter((asset) => asset.status === 'READY'),
      );
    } catch {
      this.availableAssetsSignal.set([]);
    }
  }

  private async saveTemplateInternal(
    payload: PromptTemplatePayload,
    templateId?: string,
  ): Promise<PromptActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return this.missingWorkspaceResult();
    }

    try {
      this.promptLoadingSignal.set(true);
      this.promptErrorSignal.set(null);

      const template = await firstValueFrom(
        templateId
          ? this.promptService.updateTemplate(workspaceId, templateId, payload)
          : this.promptService.createTemplate(workspaceId, payload),
      );

      this.templatesSignal.update((templates) => upsertTemplate(templates, template));
      this.notifications.success(
        templateId ? 'Template updated' : 'Template created',
        `${template.name} is available in this workspace.`,
      );
      return this.successResult();
    } catch (error) {
      return this.failureResult(error);
    } finally {
      this.promptLoadingSignal.set(false);
    }
  }

  private suggestionRequestForType(
    workspaceId: string,
    type: 'cta' | 'headline' | 'offer' | 'angle' | 'tone' | 'category',
    payload: PromptSuggestionsRequest,
  ) {
    switch (type) {
      case 'cta':
        return this.promptService.generateCtaSuggestions(workspaceId, payload);
      case 'headline':
        return this.promptService.generateHeadlineSuggestions(workspaceId, payload);
      case 'offer':
        return this.promptService.generateOfferSuggestions(workspaceId, payload);
      case 'angle':
        return this.promptService.generateCreativeAngleSuggestions(workspaceId, payload);
      case 'tone':
        return this.promptService.generateCampaignToneSuggestions(workspaceId, payload);
      case 'category':
      default:
        return this.promptService.generateBusinessCategorySuggestions(workspaceId, payload);
    }
  }

  private mergeSuggestionList(response: PromptSuggestionListResponse): void {
    const current = this.suggestionsSignal() ?? EMPTY_PROMPT_SUGGESTIONS;
    const base: PromptSuggestionsResponse = {
      ...current,
      reasoningSummary: response.reasoningSummary,
      aiProvider: response.aiProvider,
      aiModel: response.aiModel,
      tokenUsage: response.tokenUsage,
    };

    let next: PromptSuggestionsResponse;
    switch (response.suggestionType) {
      case 'CTA_SUGGESTIONS':
        next = { ...base, ctaSuggestions: response.suggestions };
        break;
      case 'HEADLINE_SUGGESTIONS':
        next = { ...base, headlineSuggestions: response.suggestions };
        break;
      case 'OFFER_SUGGESTIONS':
        next = { ...base, offerSuggestions: response.suggestions };
        break;
      case 'CREATIVE_ANGLE_SUGGESTIONS':
        next = { ...base, creativeAngleSuggestions: response.suggestions };
        break;
      case 'CAMPAIGN_TONE_SUGGESTIONS':
        next = { ...base, campaignToneSuggestions: response.suggestions };
        break;
      case 'BUSINESS_CATEGORY_SUGGESTIONS':
        next = { ...base, businessCategorySuggestions: response.suggestions };
        break;
      default:
        next = base;
        break;
    }

    this.suggestionsSignal.set(next);
  }

  private syncSelectedHistory(): void {
    const selectedHistoryId = this.selectedHistorySignal()?.id;
    if (!selectedHistoryId) {
      return;
    }

    const latest = this.historySignal().find((item) => item.id === selectedHistoryId);
    if (latest) {
      this.selectedHistorySignal.set(latest);
    }
  }

  private runLoader(operation: () => Promise<void>): Promise<void> {
    return (async () => {
      try {
        this.promptLoadingSignal.set(true);
        this.promptErrorSignal.set(null);
        await operation();
      } catch (error) {
        this.promptErrorSignal.set(this.mapLoadError(error));
      } finally {
        this.promptLoadingSignal.set(false);
      }
    })();
  }

  private resolveWorkspaceId(): string | null {
    const workspaceId = this.auth.activeWorkspaceId();
    if (workspaceId) {
      return workspaceId;
    }

    this.promptErrorSignal.set('Select a workspace before opening prompt intelligence.');
    return null;
  }

  private hasPermission(
    permission:
      | 'PROMPT_INTELLIGENCE_USE'
      | 'PROMPT_TEMPLATE_VIEW'
      | 'PROMPT_TEMPLATE_MANAGE'
      | 'PROMPT_HISTORY_VIEW',
  ): boolean {
    return this.auth.permissions().includes(permission);
  }

  private successResult(): PromptActionResult {
    return { ok: true, fieldErrors: {} };
  }

  private missingWorkspaceResult(): PromptActionResult {
    const message = 'Select a workspace before opening prompt intelligence.';
    this.promptErrorSignal.set(message);
    return { ok: false, message, fieldErrors: {} };
  }

  private failureResult(error: unknown): PromptActionResult {
    const normalized = normalizeHttpError(error);
    this.promptErrorSignal.set(normalized.message);
    return {
      ok: false,
      message: normalized.message,
      fieldErrors: this.mapFieldErrors(normalized.errors),
    };
  }

  private mapFieldErrors(errors: readonly ApiError[]): Readonly<Record<string, string>> {
    return errors.reduce<Record<string, string>>((result, error) => {
      if (error.field) {
        result[error.field] = error.message;
      }
      return result;
    }, {});
  }

  private mapLoadError(error: unknown): string {
    const normalized = normalizeHttpError(error);

    if (normalized.status === 403) {
      return 'You do not have access to this prompt workspace.';
    }

    if (normalized.status === 404) {
      return 'Prompt data could not be found for this workspace.';
    }

    if (normalized.status >= 500 || normalized.message === 'Unexpected server error') {
      return 'Prompt intelligence is unavailable right now.';
    }

    return normalized.message;
  }

  private requestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}

function upsertTemplate(
  templates: readonly PromptTemplate[],
  template: PromptTemplate,
): readonly PromptTemplate[] {
  const withoutCurrent = templates.filter((item) => item.id !== template.id);
  return [template, ...withoutCurrent].sort(
    (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
  );
}

function upsertHistory(
  history: readonly PromptHistory[],
  item: PromptHistory,
): readonly PromptHistory[] {
  const withoutCurrent = history.filter((entry) => entry.id !== item.id);
  return [item, ...withoutCurrent];
}
