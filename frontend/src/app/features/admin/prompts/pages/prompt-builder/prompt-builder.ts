import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { Asset } from '@app/features/admin/assets/models/asset.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  buildPromptSuggestionSections,
  CampaignObjective,
  CAMPAIGN_OBJECTIVE_OPTIONS,
  CreativeStyle,
  CREATIVE_STYLE_OPTIONS,
  PromptEnhanceRequest,
  PromptLanguage,
  PromptPlatform,
  PLATFORM_OPTIONS,
  PROMPT_LANGUAGE_OPTIONS,
  PromptSuggestionsRequest,
  PromptTone,
  PROMPT_TONE_OPTIONS,
  SuggestionType,
} from '../../models/prompt.models';
import { supportedPromptOptionValidator } from '../../prompt-form.validators';
import { PromptStore } from '../../state/prompt.store';
import { AssetContextPicker } from '../../components/asset-context-picker/asset-context-picker';
import { BrandContextCard } from '../../components/brand-context-card/brand-context-card';
import { PromptEditor } from '../../components/prompt-editor/prompt-editor';
import { PromptSuggestionPanel } from '../../components/prompt-suggestion-panel/prompt-suggestion-panel';
import { PromptToolbar, PromptToolbarAction } from '../../components/prompt-toolbar/prompt-toolbar';

@Component({
  selector: 'app-prompt-builder-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    BadgeComponent,
    CardComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    PromptEditor,
    PromptToolbar,
    PromptSuggestionPanel,
    BrandContextCard,
    AssetContextPicker,
  ],
  templateUrl: './prompt-builder.html',
  styleUrl: './prompt-builder.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptBuilderPage {
  protected readonly store = inject(PromptStore);
  private readonly auth = inject(CurrentUserStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly builderForm = new FormGroup({
    templateId: new FormControl('', { nonNullable: true }),
    prompt: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(5000)],
    }),
    platform: new FormControl<PromptPlatform | ''>('', {
      nonNullable: true,
      validators: [Validators.required, supportedPromptOptionValidator(PLATFORM_OPTIONS)],
    }),
    campaignObjective: new FormControl<CampaignObjective | ''>('', {
      nonNullable: true,
      validators: [
        Validators.required,
        supportedPromptOptionValidator(CAMPAIGN_OBJECTIVE_OPTIONS),
      ],
    }),
    businessType: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(80)] }),
    creativeStyle: new FormControl<CreativeStyle | ''>('', {
      nonNullable: true,
      validators: [supportedPromptOptionValidator(CREATIVE_STYLE_OPTIONS)],
    }),
    language: new FormControl<PromptLanguage | ''>('ENGLISH', {
      nonNullable: true,
      validators: [Validators.required, supportedPromptOptionValidator(PROMPT_LANGUAGE_OPTIONS)],
    }),
    tone: new FormControl<PromptTone | ''>('', {
      nonNullable: true,
      validators: [supportedPromptOptionValidator(PROMPT_TONE_OPTIONS)],
    }),
    targetAudience: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(160)] }),
    offerDetails: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(600)] }),
    ctaPreference: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    useBrandContext: new FormControl(true, { nonNullable: true }),
  });

  protected readonly workspaceLabel = computed(
    () =>
      this.auth.currentUser()?.workspaceName ??
      this.auth.activeWorkspaceId() ??
      'Workspace not selected',
  );
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'ADMIN');
  protected readonly roleTone = computed(() =>
    this.auth.currentRole() === 'MASTER'
      ? 'red'
      : this.auth.currentRole() === 'CREW'
        ? 'blue'
        : 'brand',
  );
  protected readonly suggestionSections = computed(() =>
    buildPromptSuggestionSections(this.store.suggestions()),
  );

  constructor() {
    void this.store.loadBuilderContext();

    this.builderForm.controls.prompt.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.store.setCurrentPrompt(value));

    this.builderForm.controls.language.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.store.setCurrentLanguage(value || null));

    this.builderForm.controls.useBrandContext.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => this.store.setBrandContextEnabled(value));

    this.builderForm.controls.templateId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((templateId) => {
        if (!templateId) {
          return;
        }

        const template = this.store.templates().find((item) => item.id === templateId);
        if (!template) {
          return;
        }

        this.store.selectTemplate(template);
        this.builderForm.patchValue(
          {
            prompt: template.templateText,
            platform: template.platform,
            campaignObjective: template.campaignObjective,
            businessType: template.businessType ?? '',
            language: template.language,
          },
          { emitEvent: false },
        );
        this.store.setCurrentPrompt(template.templateText);
        this.store.setCurrentLanguage(template.language);
      });
  }

  protected async runAction(action: PromptToolbarAction): Promise<void> {
    if (!this.validateBuilderForm()) {
      return;
    }

    switch (action) {
      case 'enhance':
        await this.store.enhancePrompt(this.buildEnhanceRequest());
        return;
      case 'rewrite':
        await this.store.rewritePrompt(this.buildEnhanceRequest());
        return;
      case 'suggestions':
        await this.store.generateSuggestions(this.buildSuggestionsRequest(['GENERAL_SUGGESTIONS']));
        return;
      case 'cta':
        await this.store.generateSpecificSuggestions('cta', this.buildSuggestionsRequest(['CTA_SUGGESTIONS']));
        return;
      case 'headline':
        await this.store.generateSpecificSuggestions(
          'headline',
          this.buildSuggestionsRequest(['HEADLINE_SUGGESTIONS']),
        );
        return;
      case 'offer':
        await this.store.generateSpecificSuggestions('offer', this.buildSuggestionsRequest(['OFFER_SUGGESTIONS']));
        return;
      case 'angle':
        await this.store.generateSpecificSuggestions(
          'angle',
          this.buildSuggestionsRequest(['CREATIVE_ANGLE_SUGGESTIONS']),
        );
        return;
      case 'tone':
        await this.store.generateSpecificSuggestions(
          'tone',
          this.buildSuggestionsRequest(['CAMPAIGN_TONE_SUGGESTIONS']),
        );
        return;
      case 'category':
        await this.store.generateSpecificSuggestions(
          'category',
          this.buildSuggestionsRequest(['BUSINESS_CATEGORY_SUGGESTIONS']),
        );
        return;
      default:
        return;
    }
  }

  protected async searchAssets(search: string): Promise<void> {
    await this.store.searchAvailableAssets(search);
  }

  protected toggleBrandContext(enabled: boolean): void {
    this.builderForm.controls.useBrandContext.setValue(enabled);
  }

  protected toggleAsset(asset: Asset): void {
    this.store.toggleAsset(asset);
  }

  protected removeAsset(assetId: string): void {
    this.store.removeSelectedAsset(assetId);
  }

  protected async copySuggestion(text: string): Promise<void> {
    try {
      await navigator.clipboard.writeText(text);
      this.notifications.success('Copied', 'Suggestion copied to the clipboard.');
    } catch {
      this.notifications.info('Clipboard unavailable', 'Copy the suggestion manually from the panel.');
    }
  }

  protected insertSuggestion(text: string): void {
    const prompt = this.builderForm.controls.prompt.value.trim();
    const nextPrompt = prompt ? `${prompt}\n${text}` : text;
    this.builderForm.controls.prompt.setValue(nextPrompt);
    this.store.setCurrentPrompt(nextPrompt);
  }

  protected replacePrompt(text: string): void {
    this.builderForm.controls.prompt.setValue(text);
    this.store.setCurrentPrompt(text);
  }

  protected saveSuggestionSelection(text: string): void {
    this.notifications.info(
      'Selection saved',
      `Saved foundation for later template work: ${text.slice(0, 72)}${text.length > 72 ? '...' : ''}`,
    );
  }

  protected reloadBuilder(): void {
    void this.store.loadBuilderContext();
  }

  private validateBuilderForm(): boolean {
    if (this.builderForm.invalid) {
      this.builderForm.markAllAsTouched();
      return false;
    }

    return true;
  }

  private buildEnhanceRequest(): PromptEnhanceRequest {
    const value = this.builderForm.getRawValue();

    return {
      prompt: value.prompt.trim(),
      assetIds: this.store.selectedAssets().map((asset) => asset.id),
      templateId: value.templateId || null,
      businessType: value.businessType.trim() || null,
      campaignObjective: value.campaignObjective || null,
      platform: value.platform || null,
      creativeStyle: value.creativeStyle || null,
      language: value.language || null,
      tone: value.tone || null,
      targetAudience: value.targetAudience.trim() || null,
      offerDetails: value.offerDetails.trim() || null,
      ctaPreference: value.ctaPreference.trim() || null,
      useBrandContext: value.useBrandContext,
    };
  }

  private buildSuggestionsRequest(
    suggestionTypes: readonly SuggestionType[],
  ): PromptSuggestionsRequest {
    return {
      ...this.buildEnhanceRequest(),
      suggestionTypes,
    };
  }
}
