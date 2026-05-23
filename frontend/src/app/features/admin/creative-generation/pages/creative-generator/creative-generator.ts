import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnDestroy,
  computed,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { Asset } from '@app/features/admin/assets/models/asset.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import { BrandContextSummary } from '../../components/brand-context-summary/brand-context-summary';
import { CreativeOutputDrawer } from '../../components/creative-output-drawer/creative-output-drawer';
import { CreativeOutputGallery } from '../../components/creative-output-gallery/creative-output-gallery';
import { GenerationForm } from '../../components/generation-form/generation-form';
import { GenerationJobCard } from '../../components/generation-job-card/generation-job-card';
import { GenerationProgress } from '../../components/generation-progress/generation-progress';
import { PromptContextSummary } from '../../components/prompt-context-summary/prompt-context-summary';
import { SelectedAssetsSummary } from '../../components/selected-assets-summary/selected-assets-summary';
import {
  CreateCreativeGenerationRequest,
  CreativeGenerationDraft,
  CreativeOutput,
  CreativeOutputFormat,
  CreativeType,
} from '../../models/creative-generation.models';
import { CreativeGenerationStore } from '../../state/creative-generation.store';
import {
  creativeGenerationFormValidator,
  generationConfigJsonValidator,
  supportedGenerationOptionValidator,
} from '../../creative-generation.validators';
import {
  CAMPAIGN_OBJECTIVE_OPTIONS,
  CampaignObjective,
  PLATFORM_OPTIONS,
  PROMPT_LANGUAGE_OPTIONS,
  PromptLanguage,
  PromptPlatform,
} from '@app/features/admin/prompts/models/prompt.models';
import {
  CREATIVE_OUTPUT_FORMAT_OPTIONS,
  CREATIVE_TYPE_OPTIONS,
  DEFAULT_GENERATION_DRAFT,
} from '../../models/creative-generation.models';

@Component({
  selector: 'app-creative-generator-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    BadgeComponent,
    EmptyStateComponent,
    PageHeaderComponent,
    GenerationForm,
    GenerationProgress,
    GenerationJobCard,
    CreativeOutputGallery,
    CreativeOutputDrawer,
    PromptContextSummary,
    BrandContextSummary,
    SelectedAssetsSummary,
  ],
  templateUrl: './creative-generator.html',
  styleUrl: './creative-generator.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreativeGeneratorPage implements OnDestroy {
  protected readonly store = inject(CreativeGenerationStore);
  private readonly auth = inject(CurrentUserStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly generationForm = new FormGroup(
    {
      promptHistoryId: new FormControl('', { nonNullable: true }),
      sourcePrompt: new FormControl(DEFAULT_GENERATION_DRAFT.sourcePrompt, {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(5), Validators.maxLength(32000)],
      }),
      enhancedPrompt: new FormControl(DEFAULT_GENERATION_DRAFT.enhancedPrompt, {
        nonNullable: true,
        validators: [Validators.maxLength(32000)],
      }),
      creativeType: new FormControl<CreativeType | ''>(DEFAULT_GENERATION_DRAFT.creativeType ?? '', {
        nonNullable: true,
        validators: [
          Validators.required,
          supportedGenerationOptionValidator(CREATIVE_TYPE_OPTIONS),
        ],
      }),
      platform: new FormControl<PromptPlatform | ''>(DEFAULT_GENERATION_DRAFT.platform ?? '', {
        nonNullable: true,
        validators: [Validators.required, supportedGenerationOptionValidator(PLATFORM_OPTIONS)],
      }),
      campaignObjective: new FormControl<CampaignObjective | ''>(
        DEFAULT_GENERATION_DRAFT.campaignObjective ?? '',
        {
          nonNullable: true,
          validators: [
            Validators.required,
            supportedGenerationOptionValidator(CAMPAIGN_OBJECTIVE_OPTIONS),
          ],
        },
      ),
      outputFormat: new FormControl<CreativeOutputFormat | ''>(
        DEFAULT_GENERATION_DRAFT.outputFormat ?? '',
        {
          nonNullable: true,
          validators: [
            Validators.required,
            supportedGenerationOptionValidator(CREATIVE_OUTPUT_FORMAT_OPTIONS),
          ],
        },
      ),
      language: new FormControl<PromptLanguage | ''>(DEFAULT_GENERATION_DRAFT.language ?? '', {
        nonNullable: true,
        validators: [Validators.required, supportedGenerationOptionValidator(PROMPT_LANGUAGE_OPTIONS)],
      }),
      width: new FormControl<number | null>(DEFAULT_GENERATION_DRAFT.width),
      height: new FormControl<number | null>(DEFAULT_GENERATION_DRAFT.height),
      duration: new FormControl<number | null>(DEFAULT_GENERATION_DRAFT.duration),
      useBrandContext: new FormControl(DEFAULT_GENERATION_DRAFT.useBrandContext, {
        nonNullable: true,
      }),
      generationConfigText: new FormControl('{}', {
        nonNullable: true,
        validators: [generationConfigJsonValidator()],
      }),
    },
    { validators: [creativeGenerationFormValidator()] },
  );

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
  protected readonly selectedPromptHistory = computed(() => {
    const promptHistoryId = this.store.generationFormDraft().promptHistoryId;
    return this.store.promptHistory().find((history) => history.id === promptHistoryId) ?? null;
  });

  constructor() {
    void this.store.loadGeneratorContext();
    this.store.updateDraft(this.buildDraft());

    this.generationForm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.store.updateDraft(this.buildDraft()));
  }

  ngOnDestroy(): void {
    this.store.stopPolling();
  }

  protected onPromptHistorySelected(historyId: string | null): void {
    const history = this.store.selectPromptHistory(historyId);
    if (!history) {
      return;
    }

    this.generationForm.patchValue({
      promptHistoryId: history.id,
      sourcePrompt: history.sourcePrompt,
      enhancedPrompt: history.enhancedPrompt ?? '',
      platform: history.platform ?? this.generationForm.controls.platform.value,
      campaignObjective:
        history.campaignObjective ?? this.generationForm.controls.campaignObjective.value,
      language: history.language ?? this.generationForm.controls.language.value,
    });
  }

  protected onSizeSelected(size: { width: number; height: number }): void {
    this.generationForm.patchValue(size);
  }

  protected async onSubmit(): Promise<void> {
    if (this.generationForm.invalid) {
      this.generationForm.markAllAsTouched();
      return;
    }

    await this.store.submitGeneration(this.buildRequest());
  }

  protected toggleAsset(asset: Asset): void {
    this.store.toggleAsset(asset);
  }

  protected removeAsset(assetId: string): void {
    this.store.removeSelectedAsset(assetId);
  }

  protected async previewOutput(output: CreativeOutput): Promise<void> {
    const url = await this.store.openPreviewUrl(output);
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  }

  protected async downloadOutput(output: CreativeOutput): Promise<void> {
    const url = await this.store.openDownloadUrl(output);
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  }

  protected openOutputDetail(output: CreativeOutput): void {
    this.store.setSelectedOutput(output);
  }

  protected closeOutputDetail(): void {
    this.store.setSelectedOutput(null);
  }

  protected async retryGeneration(): Promise<void> {
    await this.store.retrySelectedGeneration();
  }

  protected async cancelGeneration(): Promise<void> {
    await this.store.cancelSelectedGeneration();
  }

  protected refreshGeneration(): void {
    void this.store.refreshSelectedRequest();
  }

  protected searchAssets(search: string): void {
    void this.store.searchAssets(search);
  }

  protected reloadContext(): void {
    void this.store.loadGeneratorContext();
  }

  private buildDraft(): CreativeGenerationDraft {
    const value = this.generationForm.getRawValue();
    return {
      promptHistoryId: value.promptHistoryId || null,
      sourcePrompt: value.sourcePrompt.trim(),
      enhancedPrompt: value.enhancedPrompt.trim(),
      creativeType: value.creativeType || null,
      platform: value.platform || null,
      campaignObjective: value.campaignObjective || null,
      outputFormat: value.outputFormat || null,
      language: value.language || null,
      width: value.width,
      height: value.height,
      duration: value.duration,
      generationConfig: this.parseGenerationConfig(),
      useBrandContext: value.useBrandContext,
    };
  }

  private buildRequest(): CreateCreativeGenerationRequest {
    const value = this.generationForm.getRawValue();
    return {
      promptHistoryId: value.promptHistoryId || null,
      sourcePrompt: value.sourcePrompt.trim(),
      enhancedPrompt: value.enhancedPrompt.trim() || null,
      assetIds: this.store.selectedAssets().map((asset) => asset.id),
      creativeType: value.creativeType as CreativeType,
      platform: value.platform as PromptPlatform,
      campaignObjective: value.campaignObjective as CampaignObjective,
      outputFormat: value.outputFormat as CreativeOutputFormat,
      language: value.language as PromptLanguage,
      width: value.width,
      height: value.height,
      duration: value.duration,
      generationConfig: this.parseGenerationConfig(),
      useBrandContext: value.useBrandContext,
    };
  }

  private parseGenerationConfig(): Readonly<Record<string, unknown>> {
    const value = this.generationForm.controls.generationConfigText.value.trim();
    if (!value) {
      return {};
    }

    try {
      const parsed = JSON.parse(value) as unknown;
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
        ? (parsed as Readonly<Record<string, unknown>>)
        : {};
    } catch {
      return {};
    }
  }
}
