import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { roleBadgeTone } from '@app/core/auth/permissions';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { Asset } from '@app/features/assets/models/asset.models';
import { AssetStore } from '@app/features/assets/state/asset.store';
import { BrandLanguagePreference } from '@app/features/brands/brand.models';
import { BrandStore } from '@app/features/brands/brand.store';
import { ProductServiceStore } from '@app/features/product-services/product-service.store';
import { ProjectStore } from '@app/features/projects/project.store';
import {
  CampaignObjective,
  campaignObjectiveLabel,
  CreatePromptTemplateRequest,
  DEFAULT_BUILDER_SUGGESTION_TYPES,
  parseCampaignObjective,
  parsePromptPlatform,
  promptLanguageLabel,
  promptPlatformLabel,
  PROMPT_TONE_OPTIONS,
  PLATFORM_OPTIONS,
  CAMPAIGN_OBJECTIVE_OPTIONS,
  PROMPT_LANGUAGE_OPTIONS,
  PromptHistory,
  PromptLanguage,
  PromptPlatform,
  PromptTone,
} from '../models';
import { PromptHistoryStore } from '../state/prompt-history.store';
import { PromptStore } from '../state/prompt.store';
import { PromptTemplateStore } from '../state/prompt-template.store';
import { AssetContextSelector } from '../components/asset-context-selector/asset-context-selector';
import { AiLoadingState } from '../components/ai-loading-state/ai-loading-state';
import { PromptContextCard } from '../components/prompt-context-card/prompt-context-card';
import { PromptEditor } from '../components/prompt-editor/prompt-editor';
import { PromptEmptyState } from '../components/prompt-empty-state/prompt-empty-state';
import { PromptHistoryCard } from '../components/prompt-history-card/prompt-history-card';
import { PromptHistoryDetail } from '../components/prompt-history-detail/prompt-history-detail';
import { PromptTemplateForm } from '../components/prompt-template-form/prompt-template-form';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';

@Component({
  selector: 'app-prompt-builder-page',
  standalone: true,
  imports: [
    RouterLink,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    PageHeaderComponent,
    PromptContextCard,
    AssetContextSelector,
    PromptEditor,
    AiLoadingState,
    PromptEmptyState,
    PromptHistoryCard,
    PromptHistoryDetail,
    PromptTemplateForm,
    ModalShellComponent,
  ],
  templateUrl: './prompt-builder.html',
  styleUrl: './prompt-builder.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptBuilderPage {
  protected readonly promptStore = inject(PromptStore);
  protected readonly historyStore = inject(PromptHistoryStore);
  protected readonly templateStore = inject(PromptTemplateStore);
  private readonly permissions = inject(PermissionStore);
  private readonly auth = inject(CurrentUserStore);
  private readonly workspace = inject(WorkspaceStore);
  private readonly brandStore = inject(BrandStore);
  private readonly productStore = inject(ProductServiceStore);
  private readonly projectStore = inject(ProjectStore);
  protected readonly assetStore = inject(AssetStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly route = inject(ActivatedRoute);

  private readonly fieldErrorsSignal = signal<Readonly<Record<string, string>>>({});
  private readonly templateFieldErrorsSignal = signal<Readonly<Record<string, string>>>({});
  private readonly initializingSignal = signal(true);
  private readonly advancedOpenSignal = signal(false);
  private readonly historyOpenSignal = signal(false);
  private readonly historySearchSignal = signal('');
  private readonly historyDetailOpenSignal = signal(false);
  private readonly selectedHistoryEntrySignal = signal<PromptHistory | null>(null);
  private readonly templateDialogOpenSignal = signal(false);

  protected readonly fieldErrors = this.fieldErrorsSignal.asReadonly();
  protected readonly templateFieldErrors = this.templateFieldErrorsSignal.asReadonly();
  protected readonly initializing = this.initializingSignal.asReadonly();
  protected readonly advancedOpen = this.advancedOpenSignal.asReadonly();
  protected readonly historyOpen = this.historyOpenSignal.asReadonly();
  protected readonly historySearch = this.historySearchSignal.asReadonly();
  protected readonly historyDetailOpen = this.historyDetailOpenSignal.asReadonly();
  protected readonly selectedHistoryEntry = this.selectedHistoryEntrySignal.asReadonly();
  protected readonly templateDialogOpen = this.templateDialogOpenSignal.asReadonly();

  protected readonly projectId = computed(() => this.route.snapshot.paramMap.get('projectId') ?? '');
  protected readonly workspaceLabel = this.workspace.workspaceLabel;
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'User');
  protected readonly roleTone = computed(() => roleBadgeTone(this.auth.currentRole()));

  protected readonly project = computed(
    () => this.projectStore.items().find((item) => item.id === this.projectId()) ?? null,
  );
  protected readonly product = computed(() => {
    const project = this.project();
    return this.productStore.items().find((item) => item.id === project?.productServiceId) ?? null;
  });
  protected readonly brand = computed(() => {
    const product = this.product();
    return this.brandStore.items().find((item) => item.id === product?.brandId) ?? null;
  });

  protected readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));
  protected readonly hasProjectContext = computed(() => Boolean(this.project()));
  protected readonly contextReady = computed(
    () => this.hasWorkspaceContext() && this.hasProjectContext() && !this.initializing(),
  );
  protected readonly canUsePromptBuilder = this.permissions.canUsePromptBuilder;
  protected readonly canEnhancePrompt = this.promptStore.canEnhancePrompt;
  protected readonly aiBusy = computed(() => this.promptStore.aiLoading());

  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly campaignObjectiveOptions = CAMPAIGN_OBJECTIVE_OPTIONS;
  protected readonly languageOptions = PROMPT_LANGUAGE_OPTIONS;
  protected readonly toneOptions = PROMPT_TONE_OPTIONS;
  protected readonly promptLanguageLabel = promptLanguageLabel;
  protected readonly promptPlatformLabel = promptPlatformLabel;
  protected readonly campaignObjectiveLabel = campaignObjectiveLabel;

  protected readonly builderSettings = this.promptStore.builderSettings;
  protected readonly sourcePromptError = computed(() => this.fieldErrors()['sourcePrompt'] ?? null);
  protected readonly canManageTemplates = this.templateStore.canManageTemplates;
  protected readonly canViewHistory = this.historyStore.canViewHistory;
  protected readonly visibleLanguageOptions = computed(() => {
    const preference = this.brand()?.languagePreference;
    if (preference === 'BANGLA') {
      return PROMPT_LANGUAGE_OPTIONS.filter((option) => option.value === 'BANGLA');
    }

    if (preference === 'ENGLISH') {
      return PROMPT_LANGUAGE_OPTIONS.filter((option) => option.value === 'ENGLISH');
    }

    return PROMPT_LANGUAGE_OPTIONS;
  });
  protected readonly languageGuidance = computed(() => {
    const preference = this.brand()?.languagePreference;
    if (preference === 'BANGLA') {
      return 'Bangla is selected from this brand profile.';
    }

    if (preference === 'ENGLISH') {
      return 'English is selected from this brand profile.';
    }

    return 'This brand supports Bangla and English. Choose the language for this campaign.';
  });
  protected readonly filteredHistory = computed(() => {
    const search = this.historySearchSignal().trim().toLowerCase();
    const history = this.historyStore.history();
    if (!search) {
      return history;
    }

    return history.filter((entry) =>
      [entry.sourcePrompt, entry.enhancedPrompt, entry.businessType, entry.platform, entry.campaignObjective]
        .filter(Boolean)
        .some((value) => String(value).toLowerCase().includes(search)),
    );
  });
  protected readonly templatePromptBody = computed(
    () => this.promptStore.enhancedPrompt()?.enhancedPrompt ?? this.promptStore.sourcePrompt(),
  );

  protected readonly suggestionGroups = computed(() => {
    const suggestions = this.promptStore.suggestions();
    if (!suggestions) {
      return [] as const;
    }

    return [
      { title: 'CTA', items: suggestions.ctaSuggestions },
      { title: 'Headlines', items: suggestions.headlineSuggestions },
      { title: 'Offers', items: suggestions.offerSuggestions },
      { title: 'Creative angles', items: suggestions.creativeAngleSuggestions },
      { title: 'Campaign tone', items: suggestions.campaignToneSuggestions },
      { title: 'Business category', items: suggestions.businessCategorySuggestions },
    ].filter((group) => group.items.length > 0);
  });

  constructor() {
    void this.initialize();
  }

  protected async initialize(): Promise<void> {
    this.initializingSignal.set(true);
    this.fieldErrorsSignal.set({});

    const workspaceId = this.auth.activeWorkspaceId();
    const projectId = this.projectId();

    if (!workspaceId || !projectId) {
      this.promptStore.setSelectedProjectContext(null);
      this.initializingSignal.set(false);
      return;
    }

    await Promise.all([
      this.brandStore.load(workspaceId),
      this.productStore.load(workspaceId),
      this.projectStore.load(workspaceId),
    ]);

    const project = this.projectStore.items().find((item) => item.id === projectId) ?? null;
    const product =
      this.productStore.items().find((item) => item.id === project?.productServiceId) ?? null;
    const brand = this.brandStore.items().find((item) => item.id === product?.brandId) ?? null;

    if (project && product && brand) {
      this.promptStore.setSelectedProjectContext({
        workspaceId,
        projectId: project.id,
        brandId: brand.id,
        productServiceId: product.id,
        brandName: brand.name,
        productName: product.name,
        projectName: project.name,
      });

      this.promptStore.patchBuilderSettings({
        platform: parsePromptPlatform(project.targetPlatform),
        campaignObjective: parseCampaignObjective(project.campaignObjective),
        language: this.defaultLanguageForBrand(brand.languagePreference),
        businessType: brand.businessType,
        targetAudience: brand.targetAudience ?? product.targetAudience ?? '',
        useBrandProfile: true,
      });
    } else {
      this.promptStore.setSelectedProjectContext(null);
    }

    await this.assetStore.loadProjectAssets(projectId);
    if (this.historyStore.canViewHistory()) {
      this.historyStore.setSelectedProjectId(projectId);
      await this.historyStore.loadHistory(projectId);
    }
    this.initializingSignal.set(false);
  }

  protected updateSourcePrompt(value: string): void {
    this.fieldErrorsSignal.update((errors) => removeFieldError(errors, 'sourcePrompt'));
    this.promptStore.setSourcePrompt(value);
  }

  protected updatePlatform(value: string): void {
    this.patchSetting('platform', parsePromptPlatform(value));
  }

  protected updateCampaignObjective(value: string): void {
    this.patchSetting('campaignObjective', parseCampaignObjective(value));
  }

  protected updateLanguage(value: string): void {
    const language = PROMPT_LANGUAGE_OPTIONS.find((option) => option.value === value)?.value ?? null;
    this.patchSetting('language', language);
  }

  protected updateTone(value: string): void {
    const tone = PROMPT_TONE_OPTIONS.find((option) => option.value === value)?.value ?? null;
    this.patchSetting('tone', tone);
  }

  protected async enhancePrompt(): Promise<void> {
    this.fieldErrorsSignal.set({});
    const result = await this.promptStore.enhancePrompt();
    if (!result.ok) {
      this.fieldErrorsSignal.set(result.fieldErrors);
    }
  }

  protected async getSuggestions(): Promise<void> {
    this.fieldErrorsSignal.set({});
    const result = await this.promptStore.getSuggestions(DEFAULT_BUILDER_SUGGESTION_TYPES);
    if (!result.ok) {
      this.fieldErrorsSignal.set(result.fieldErrors);
    }
  }

  protected copyEnhancedPrompt(): void {
    const enhanced = this.promptStore.enhancedPrompt()?.enhancedPrompt;
    if (!enhanced?.trim()) {
      return;
    }

    void navigator.clipboard.writeText(enhanced).then(() => {
      this.notifications.success('Copied', 'Enhanced prompt copied to clipboard.');
    });
  }

  protected resetPrompt(): void {
    this.fieldErrorsSignal.set({});
    this.promptStore.resetPrompt();
  }

  protected toggleAdvanced(): void {
    this.advancedOpenSignal.update((open) => !open);
  }

  protected toggleHistory(): void {
    this.historyOpenSignal.update((open) => !open);
  }

  protected updateHistorySearch(value: string): void {
    this.historySearchSignal.set(value);
  }

  protected openHistoryDetail(entry: PromptHistory): void {
    this.selectedHistoryEntrySignal.set(entry);
    this.historyDetailOpenSignal.set(true);
  }

  protected closeHistoryDetail(): void {
    this.selectedHistoryEntrySignal.set(null);
    this.historyDetailOpenSignal.set(false);
  }

  protected reloadHistory(): void {
    void this.historyStore.loadHistory(this.projectId());
  }

  protected openTemplateDialog(): void {
    this.templateFieldErrorsSignal.set({});
    this.templateDialogOpenSignal.set(true);
  }

  protected closeTemplateDialog(): void {
    this.templateFieldErrorsSignal.set({});
    this.templateDialogOpenSignal.set(false);
  }

  protected async saveTemplate(payload: CreatePromptTemplateRequest): Promise<void> {
    this.templateFieldErrorsSignal.set({});
    const result = await this.templateStore.createTemplate(payload);
    if (result.ok) {
      this.closeTemplateDialog();
    } else {
      this.templateFieldErrorsSignal.set(result.fieldErrors);
    }
  }

  protected toggleAsset(asset: Asset): void {
    this.promptStore.toggleAsset(asset);
  }

  protected removeAsset(assetId: string): void {
    this.promptStore.removeSelectedAsset(assetId);
  }

  protected clearError(): void {
    this.promptStore.clearError();
    this.fieldErrorsSignal.set({});
  }

  private patchSetting(
    key: 'platform' | 'campaignObjective' | 'language' | 'tone',
    value: PromptPlatform | CampaignObjective | PromptLanguage | PromptTone | null,
  ): void {
    this.fieldErrorsSignal.update((errors) => removeFieldError(errors, key));
    this.promptStore.patchBuilderSettings({ [key]: value });
  }

  private defaultLanguageForBrand(preference: BrandLanguagePreference): PromptLanguage | null {
    switch (preference) {
      case 'BANGLA':
        return 'BANGLA';
      case 'ENGLISH':
        return 'ENGLISH';
      case 'BOTH':
        return null;
    }
  }
}

function removeFieldError(
  errors: Readonly<Record<string, string>>,
  key: string,
): Readonly<Record<string, string>> {
  if (!(key in errors)) {
    return errors;
  }

  const next = { ...errors };
  delete next[key];
  return next;
}
