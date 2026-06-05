import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  HostListener,
  OnDestroy,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BillingModalService } from '@app/features/payments/services/billing-modal.service';
import { CreditUsagePreviewCardComponent } from '@app/features/credits/components/credit-usage-preview-card/credit-usage-preview-card';
import { InsufficientCreditCardComponent } from '@app/features/credits/components/insufficient-credit-card/insufficient-credit-card';
import { WorkspaceCreditsStore } from '@app/features/credits/state/workspaceCredits.store';
import {
  Asset,
  assetStatusLabel,
  assetStatusTone,
  formatFileSize,
  isImageAsset,
} from '@app/features/admin/assets/models/asset.models';
import { AssetStore } from '@app/features/admin/assets/state/asset.store';
import { BrandStore } from '@app/features/brands/brand.store';
import { ProductServiceStore } from '@app/features/product-services/product-service.store';
import { ProjectStore } from '@app/features/projects/project.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import { CreativeOutputDrawer } from '../../components/creative-output-drawer/creative-output-drawer';
import {
  CreateCreativeGenerationRequest,
  CreateCampaignCreativeRequest,
  AiCreativeGenerateRequest,
  AiCreativePlatform,
  AiCreativeSize,
  AiCreativeType,
  CreativeGenerationDraft,
  CreativeGenerationRequest,
  CreativePipelineLayerRun,
  CreativeOutput,
  CreativeOutputFormat,
  CreativeType,
  DEFAULT_GENERATION_DRAFT,
  ImageCreativeFormat,
  ImageCreativeQualityMode,
  creativeGenerationStatusLabel,
  creativeGenerationStatusTone,
  creativeTypeLabel,
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
  promptLanguageLabel,
  promptPlatformLabel,
} from '@app/features/admin/prompts/models/prompt.models';
import {
  CREATIVE_OUTPUT_FORMAT_OPTIONS,
  CREATIVE_TYPE_OPTIONS,
} from '../../models/creative-generation.models';

type GeneratorTone = 'Promotional' | 'Premium' | 'Friendly' | 'Urgent';
type ModelQuality = 'Basic' | 'Premium';
type VisualCreativeFormat = 'SQUARE_POST' | 'STORY' | 'BANNER' | 'PRODUCT_AD';
type ContextDropdown = 'brand' | 'product' | 'project' | 'language';

interface CreativePreset {
  readonly id: VisualCreativeFormat;
  readonly label: string;
  readonly type: CreativeType;
  readonly format: CreativeOutputFormat;
  readonly width: number;
  readonly height: number;
}

@Component({
  selector: 'app-creative-generator-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    IconComponent,
    PageHeaderComponent,
    CreativeOutputDrawer,
    CreditUsagePreviewCardComponent,
    InsufficientCreditCardComponent,
  ],
  templateUrl: './creative-generator.html',
  styleUrl: './creative-generator.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreativeGeneratorPage implements OnDestroy {
  protected readonly store = inject(CreativeGenerationStore);
  private readonly auth = inject(CurrentUserStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly workspaceCredits = inject(WorkspaceCreditsStore);
  private readonly billingModal = inject(BillingModalService);
  private readonly notifications = inject(NotificationStateService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly brandStore = inject(BrandStore);
  protected readonly productStore = inject(ProductServiceStore);
  protected readonly projectStore = inject(ProjectStore);
  protected readonly assetStore = inject(AssetStore);

  protected readonly selectedBrandId = signal<string>('');
  protected readonly selectedProductId = signal<string>('');
  protected readonly selectedProjectId = signal<string>('');
  protected readonly openContextDropdown = signal<ContextDropdown | null>(null);
  protected readonly selectedTone = signal<GeneratorTone>('Premium');
  protected readonly selectedQuality = signal<ModelQuality>('Basic');
  protected readonly selectedVisualFormat = signal<VisualCreativeFormat>('SQUARE_POST');
  protected readonly selectedVariationId = signal<string | null>(null);
  protected readonly selectedProductImageFile = signal<File | null>(null);
  protected readonly localProductImagePreviewUrl = signal<string | null>(null);
  protected readonly advancedCreativeOptionsOpen = signal(false);
  protected readonly selectedLogoImageFile = signal<File | null>(null);
  protected readonly selectedReferenceImageFile = signal<File | null>(null);
  protected readonly selectedMaskImageFile = signal<File | null>(null);
  protected readonly assetPickerOpen = signal(false);
  protected readonly assetPickerSearch = signal('');
  private readonly readinessRequestKey = signal<string | null>(null);
  private readonly formRevision = signal(0);
  protected readonly generationSteps = ['Understanding brand', 'Analyzing product', 'Creating ad layout', 'Preparing output'] as const;
  protected readonly visibleGenerationSteps = computed(() =>
    this.generationModeHint() === 'TEXT_ONLY_CREATIVE'
      ? ['Understanding brand', 'Text-only mode', 'Creating ad layout', 'Preparing output'] as const
      : this.generationSteps,
  );
  protected readonly fallbackPipelineSteps = computed(() => {
    const textOnly = this.generationModeHint() === 'TEXT_ONLY_CREATIVE';
    return [
      { label: 'Understanding brand', helper: 'Waiting for backend plan', status: 'PLANNED' },
      {
        label: textOnly ? 'Product analysis skipped' : 'Analyzing product',
        helper: textOnly ? 'Text-only mode does not require a product image' : 'Waiting for backend plan',
        status: textOnly ? 'SKIPPED' : 'PLANNED',
      },
      { label: 'Creating ad layout', helper: 'Waiting for backend plan', status: 'PLANNED' },
      { label: 'Preparing output', helper: 'Waiting for backend plan', status: 'PLANNED' },
    ] as const;
  });
  protected readonly tools = [
    { title: 'Post Generator', description: 'Create platform-ready post concepts.', icon: 'megaphone', route: '/post-generator' },
    { title: 'Captions', description: 'Write short, on-brand captions.', icon: 'pencil-line', route: '/captions' },
    { title: 'Ad Copy', description: 'Draft headlines and conversion copy.', icon: 'receipt-text', route: '/ad-copy' },
    { title: 'Hashtags', description: 'Prepare discoverable hashtag sets.', icon: 'hash', route: '/hashtags' },
    { title: 'Product Video', description: 'Plan product-focused motion ads.', icon: 'video', route: '/creative-generator' },
    { title: 'Voiceover', description: 'Prepare voiceover-ready scripts.', icon: 'mic-2', route: '/ad-copy' },
  ] as const;
  protected readonly creativePresets: readonly CreativePreset[] = [
    { id: 'SQUARE_POST', label: 'Square Post 1080x1080', type: 'STATIC_IMAGE', format: 'PNG', width: 1080, height: 1080 },
    { id: 'STORY', label: 'Story 1080x1920', type: 'STORY_CREATIVE', format: 'PNG', width: 1080, height: 1920 },
    { id: 'BANNER', label: 'Banner', type: 'STATIC_IMAGE', format: 'WEBP', width: 1200, height: 628 },
    { id: 'PRODUCT_AD', label: 'Product Ad', type: 'CAROUSEL_IMAGE', format: 'PNG', width: 1080, height: 1080 },
  ] as const;
  protected readonly tones: readonly GeneratorTone[] = ['Promotional', 'Premium', 'Friendly', 'Urgent'];
  protected readonly qualities: readonly ModelQuality[] = ['Basic', 'Premium'];
  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly languageOptions = PROMPT_LANGUAGE_OPTIONS.filter((option) => option.value !== 'MIXED');
  protected readonly allowedLanguageOptions = computed(() => {
    const preference = this.selectedBrand()?.languagePreference ?? 'BOTH';
    return this.languageOptions.filter((option) => isLanguageAllowed(option.value, preference));
  });
  protected readonly formatFileSize = formatFileSize;
  protected readonly isImageAsset = isImageAsset;
  protected readonly assetStatusLabel = assetStatusLabel;
  protected readonly assetStatusTone = assetStatusTone;

  protected readonly generationForm = new FormGroup(
    {
      promptHistoryId: new FormControl('', { nonNullable: true }),
      sourcePrompt: new FormControl(DEFAULT_GENERATION_DRAFT.sourcePrompt, {
        nonNullable: true,
        validators: [Validators.maxLength(4000)],
      }),
      enhancedPrompt: new FormControl(DEFAULT_GENERATION_DRAFT.enhancedPrompt, {
        nonNullable: true,
        validators: [Validators.maxLength(32000)],
      }),
      creativeType: new FormControl<CreativeType | ''>(DEFAULT_GENERATION_DRAFT.creativeType ?? '', {
        nonNullable: true,
        validators: [Validators.required, supportedGenerationOptionValidator(CREATIVE_TYPE_OPTIONS)],
      }),
      platform: new FormControl<PromptPlatform | ''>(DEFAULT_GENERATION_DRAFT.platform ?? '', {
        nonNullable: true,
        validators: [Validators.required, supportedGenerationOptionValidator(PLATFORM_OPTIONS)],
      }),
      campaignObjective: new FormControl<CampaignObjective | ''>(
        DEFAULT_GENERATION_DRAFT.campaignObjective ?? '',
        {
          nonNullable: true,
          validators: [Validators.required, supportedGenerationOptionValidator(CAMPAIGN_OBJECTIVE_OPTIONS)],
        },
      ),
      outputFormat: new FormControl<CreativeOutputFormat | ''>(DEFAULT_GENERATION_DRAFT.outputFormat ?? '', {
        nonNullable: true,
        validators: [Validators.required, supportedGenerationOptionValidator(CREATIVE_OUTPUT_FORMAT_OPTIONS)],
      }),
      language: new FormControl<PromptLanguage | ''>(DEFAULT_GENERATION_DRAFT.language ?? '', {
        nonNullable: true,
        validators: [Validators.required, supportedGenerationOptionValidator(PROMPT_LANGUAGE_OPTIONS)],
      }),
      width: new FormControl<number | null>(DEFAULT_GENERATION_DRAFT.width),
      height: new FormControl<number | null>(DEFAULT_GENERATION_DRAFT.height),
      duration: new FormControl<number | null>(DEFAULT_GENERATION_DRAFT.duration),
      requestedVersionCount: new FormControl(1, {
        nonNullable: true,
        validators: [Validators.required, Validators.min(1), Validators.max(20)],
      }),
      cta: new FormControl('', {
        nonNullable: true,
        validators: [Validators.maxLength(40)],
      }),
      headline: new FormControl('', {
        nonNullable: true,
        validators: [Validators.maxLength(80)],
      }),
      subheadline: new FormControl('', {
        nonNullable: true,
        validators: [Validators.maxLength(120)],
      }),
      offerText: new FormControl('', {
        nonNullable: true,
        validators: [Validators.maxLength(80)],
      }),
      targetAudience: new FormControl('', {
        nonNullable: true,
        validators: [Validators.maxLength(150)],
      }),
      backgroundPrompt: new FormControl('', {
        nonNullable: true,
        validators: [Validators.maxLength(500)],
      }),
      transparentBackground: new FormControl(false, { nonNullable: true }),
      useBrandContext: new FormControl(DEFAULT_GENERATION_DRAFT.useBrandContext, { nonNullable: true }),
      generationConfigText: new FormControl('{}', {
        nonNullable: true,
        validators: [generationConfigJsonValidator()],
      }),
    },
    { validators: [creativeGenerationFormValidator()] },
  );

  protected readonly workspaceLabel = computed(
    () => this.auth.currentUser()?.workspaceName ?? this.auth.activeWorkspaceId() ?? 'Workspace not selected',
  );
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'ADMIN');
  protected readonly roleTone = computed(() =>
    this.auth.currentRole() === 'MASTER' ? 'red' : this.auth.currentRole() === 'CREW' ? 'blue' : 'brand',
  );
  protected readonly selectedBrand = computed(() =>
    this.brandStore.items().find((brand) => brand.id === this.selectedBrandId()) ?? null,
  );
  protected readonly filteredProducts = computed(() =>
    this.productStore.items().filter((product) => product.brandId === this.selectedBrandId()),
  );
  protected readonly selectedProduct = computed(() =>
    this.productStore.items().find((product) => product.id === this.selectedProductId()) ?? null,
  );
  protected readonly filteredProjects = computed(() =>
    this.projectStore.items().filter((project) => {
      const productId = this.selectedProductId();
      if (productId) {
        return project.productServiceId === productId;
      }
      return Boolean(this.selectedBrandId()) && project.brandId === this.selectedBrandId();
    }),
  );
  protected readonly selectedProject = computed(() =>
    this.projectStore.items().find((project) => project.id === this.selectedProjectId()) ?? null,
  );
  protected readonly selectedAsset = computed(() => this.store.selectedAssets()[0] ?? null);
  protected readonly productAssetId = computed(() => this.selectedAsset()?.id ?? '');
  protected readonly productImageRequired = computed(() => false);
  protected readonly selectedProductImageReady = computed(() =>
    Boolean(this.selectedProductImageFile()) || isGenerationReadyAsset(this.selectedAsset()),
  );
  protected readonly hasProductImageInput = computed(() =>
    Boolean(this.selectedProductImageFile()) || Boolean(this.productAssetId()),
  );
  protected readonly selectedProductImagePreviewUrl = computed(() => {
    const asset = this.selectedAsset();
    return (
      this.localProductImagePreviewUrl() ||
      asset?.thumbnailUrl ||
      asset?.previewUrl ||
      asset?.publicUrl ||
      null
    );
  });
  protected readonly imageInputModeLabel = computed(() =>
    generationModeHintLabel(this.generationModeHint()),
  );
  protected readonly generationModeHint = computed<NonNullable<AiCreativeGenerateRequest['generationModeHint']>>(() => {
    this.formRevision();
    const value = this.generationForm.getRawValue();
    const hasPrimaryImage = this.hasProductImageInput();
    const hasReference = Boolean(this.selectedLogoImageFile() || this.selectedReferenceImageFile());
    const hasMaskReplacement = Boolean(this.selectedMaskImageFile() && value.backgroundPrompt.trim());
    if (value.transparentBackground) {
      return 'TRANSPARENT_ASSET';
    }
    if (hasMaskReplacement) {
      return 'BACKGROUND_REPLACEMENT';
    }
    if (hasPrimaryImage && hasReference) {
      return 'MULTI_REFERENCE_CREATIVE';
    }
    return hasPrimaryImage ? 'PRODUCT_IMAGE_CREATIVE' : 'TEXT_ONLY_CREATIVE';
  });
  protected readonly promptTitlePreview = computed(() => {
    this.formRevision();
    const brandName = this.selectedBrand()?.name?.trim();
    const productName = this.selectedProduct()?.name?.trim();
    const campaignName = this.selectedProject()?.name?.trim();
    const creativeType = visualCreativeFormatLabel(this.selectedVisualFormat());
    const platform = promptPlatformLabel(this.generationForm.controls.platform.value || null);
    if (!brandName) {
      return `Select brand - ${creativeType} - ${platform}`;
    }
    const parts = productName && campaignName
      ? [brandName, productName, campaignName, creativeType, platform]
      : campaignName
        ? [brandName, campaignName, creativeType, platform]
        : [brandName, creativeType, platform];
    return parts.filter((part) => part && part !== 'Not selected').join(' - ');
  });
  protected readonly readinessChecklist = computed(() => {
    const readiness = this.store.campaignReadiness();
    const productImageReady = this.productImageRequired()
      ? this.selectedProductImageReady()
      : true;
    return [
      { label: 'Project hierarchy ready', ready: Boolean(this.auth.activeWorkspaceId() && this.selectedBrandId()) },
      { label: 'Package ready', ready: readiness?.packageReady ?? this.packageReadyForSubmit() },
      { label: 'Credits ready', ready: this.hasSufficientCredits() },
      { label: 'Provider ready', ready: readiness?.providerReady ?? true },
      { label: 'Routing ready', ready: readiness?.routingReady ?? true },
      { label: `Image input: ${this.imageInputModeLabel()}`, ready: productImageReady },
    ] as const;
  });
  protected readonly pipelineLayers = computed(() => this.store.campaignPipeline()?.layers ?? []);
  protected readonly currentPipelineLayer = computed(() =>
    this.pipelineLayers().find((layer) => layer.status === 'PROCESSING') ??
    this.pipelineLayers().find((layer) => layer.status === 'PLANNED' || layer.status === 'QUEUED') ??
    null,
  );
  protected readonly promptLength = computed(() => {
    this.formRevision();
    return this.generationForm.controls.sourcePrompt.value.length;
  });
  protected readonly resolvedCreativeFormat = computed(() =>
    {
      this.formRevision();
      return resolveCreativeFormat(
        this.generationForm.controls.platform.value || null,
        this.selectedVisualFormat(),
      );
    },
  );
  protected readonly uploadInProgress = computed(() => this.assetStore.assetLoading());
  protected readonly assetPickerAssets = computed(() =>
    this.store.availableAssets().filter((asset) => isImageAsset(asset) && isGenerationReadyAsset(asset)),
  );
  protected readonly canSubmitCampaignCreative = computed(() => {
    this.formRevision();
    const value = this.generationForm.getRawValue();
    return Boolean(
        this.auth.activeWorkspaceId() &&
        this.selectedBrandId() &&
        value.platform &&
        value.language &&
        value.language !== 'MIXED' &&
        this.isSelectedLanguageAllowed() &&
        this.qualityMode() &&
        (!this.productImageRequired() || this.hasProductImageInput()) &&
        (!this.productImageRequired() || this.selectedProductImageReady()) &&
        this.generationSetupReady() &&
        (value.sourcePrompt.trim().length > 0 || this.hasEnoughCreativeContext()) &&
        this.packageReadyForSubmit() &&
        this.hasSufficientCredits() &&
        this.generationForm.valid &&
        !this.uploadInProgress() &&
        !this.store.generationLoading(),
    );
  });
  protected readonly submitDisabledReason = computed(() => {
    this.formRevision();
    const value = this.generationForm.getRawValue();
    if (!this.auth.activeWorkspaceId()) return 'Workspace is not loaded.';
    if (this.workspace.loading()) return 'Workspace package and credits are loading.';
    if (this.workspace.error()) return 'Credits or package could not be loaded. Please refresh or contact support.';
    if (!this.selectedBrandId()) return 'Select a brand.';
    if (!value.platform) return 'Select a platform.';
    if (!value.language || value.language === 'MIXED') return 'Select English or Bangla.';
    if (!this.isSelectedLanguageAllowed()) return 'Selected language does not match the brand language preference.';
    if (this.hasProductImageInput() && !this.selectedProductImageReady()) return 'Selected product image is not ready yet.';
    if (!this.generationSetupReady()) return 'Generation setup is not ready. Check the readiness checklist.';
    if (!value.sourcePrompt.trim() && !this.hasEnoughCreativeContext()) return 'Add campaign idea or select enough context to generate.';
    if (!this.packageReadyForSubmit()) return 'No active package is assigned to this workspace.';
    if (!this.hasSufficientCredits()) return this.generationCreditPreview().blockingReasons[0] ?? 'Credit availability is not ready.';
    if (this.uploadInProgress()) return 'Wait for image upload to finish.';
    if (this.store.generationLoading()) return 'Generation request is in progress.';
    if (this.generationForm.invalid) return this.formValidationReason() ?? 'Fix the highlighted form fields.';
    return null;
  });
  protected readonly creditCost = computed(() => {
    const base = this.selectedQuality() === 'Premium' ? 18 : 8;
    return this.generationForm.controls.outputFormat.value === 'MP4' || this.generationForm.controls.outputFormat.value === 'MOV'
      ? base + 20
      : base;
  });
  protected readonly generationCreditPreview = computed(() => {
    this.formRevision();
    const costPreview = this.store.campaignCostPreview();
    const availability = parseAvailabilityPreview(costPreview?.toolCode ?? null);
    const requestedVersionCount = costPreview?.requestedVersionCount ?? this.generationForm.controls.requestedVersionCount.value;
    const estimatedCreditCost = costPreview ? costPreview.totalCreditCost : null;
    const availableCredits = this.workspaceCredits.creditAccount()?.availableCredits ?? this.workspace.usage()?.creditsRemaining ?? 0;
    const remainingCreditsAfterGeneration = estimatedCreditCost === null ? null : availableCredits - estimatedCreditCost;
    const blockingReasons = availability.blockGeneration ? [availability.message || 'Credit availability blocks generation.'] : [];
    const availabilityLoaded = Boolean(costPreview);
    return {
      requestedVersionCount,
      estimatedCreditCost,
      availableCredits,
      remainingCreditsAfterGeneration,
      creditStatus: availability.status,
      message: availability.message,
      packageLimit: this.creditsTotal(),
      canReserveCredits: availabilityLoaded && blockingReasons.length === 0,
      canQueueGeneration: availabilityLoaded && blockingReasons.length === 0 && this.store.campaignReadiness()?.ready !== false,
      blockingReasons,
    };
  });
  protected readonly packageReady = computed(() => Boolean(this.workspace.subscription()));
  protected readonly creditsLoaded = computed(() => typeof this.workspace.usage()?.creditsRemaining === 'number');
  protected readonly packageReadyForSubmit = computed(() =>
    this.workspace.workspaceContext() === null ||
      Boolean(this.workspace.subscription()) ||
      Boolean(this.workspace.featurePolicy()),
  );
  protected readonly hasSufficientCredits = computed(() => {
    return this.generationCreditPreview().canQueueGeneration;
  });
  protected readonly creditPreviewAvailable = computed(() =>
    Boolean(this.store.campaignCostPreview()) ||
      this.generationForm.controls.sourcePrompt.value.trim().length > 0 ||
      this.hasEnoughCreativeContext(),
  );
  protected readonly generationSetupReady = computed(() => {
    const readiness = this.store.campaignReadiness();
    if (!readiness) {
      return true;
    }
    return (
      readiness.packageReady !== false &&
      readiness.creditsReady !== false &&
      readiness.providerReady !== false &&
      readiness.routingReady !== false
    );
  });
  protected readonly currentOutput = computed(() => {
    const selectedId = this.selectedVariationId();
    return this.store.creativeOutputs().find((output) => output.id === selectedId) ?? this.store.creativeOutputs()[0] ?? null;
  });
  protected readonly recentGenerations = computed(() => this.store.generationRequests().slice(0, 4));
  protected readonly creditsRemaining = computed(() => this.workspace.usage()?.creditsRemaining ?? null);
  protected readonly creditLimit = computed(() => this.workspace.featureLimit('credits') ?? this.workspace.featureLimit('credit'));
  protected readonly creditsUsed = computed(() => {
    const limit = this.creditLimit();
    if (typeof limit?.used === 'number') {
      return limit.used;
    }
    if (typeof limit?.limit === 'number' && typeof this.creditsRemaining() === 'number') {
      return Math.max(0, limit.limit - this.creditsRemaining()!);
    }
    return null;
  });
  protected readonly creditsTotal = computed(() => this.creditLimit()?.limit ?? null);
  protected readonly creditUsageLabel = computed(() => {
    const used = this.creditsUsed();
    const total = this.creditsTotal();
    return typeof used === 'number' && typeof total === 'number' ? `${used} / ${total}` : '--';
  });
  protected readonly creditPercent = computed(() => {
    const used = this.creditsUsed();
    const total = this.creditsTotal();
    if (typeof used !== 'number' || typeof total !== 'number' || total <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((used / total) * 100));
  });
  protected readonly usageCards = computed(() => [
    { label: 'This Month Usage', value: this.creditUsageLabel(), icon: 'gauge' },
    { label: 'Images Generated', value: String(this.store.generationRequests().filter((item) => item.outputFormat !== 'MP4' && item.outputFormat !== 'MOV').length), icon: 'image' },
    { label: 'Posts Generated', value: String(this.store.promptHistory().length), icon: 'megaphone' },
    { label: 'Videos Generated', value: String(this.store.generationRequests().filter((item) => item.outputFormat === 'MP4' || item.outputFormat === 'MOV').length), icon: 'video' },
    { label: 'Captions Generated', value: '--', icon: 'captions' },
  ]);

  protected layerTypeLabel(layerType: string | null | undefined): string {
    const labels: Record<string, string> = {
      IMAGE_ANALYSIS: 'Image analysis',
      BACKGROUND_REMOVAL: 'Background removal',
      IMAGE_CLEANUP: 'Cleanup',
      PROMPT_GENERATION: 'Prompt generation',
      IMAGE_GENERATION: 'Image generation',
      TEXT_OVERLAY: 'Text overlay',
      VISION_QUALITY_CHECK: 'Quality check',
      IMAGE_RESIZE: 'Resize',
      IMAGE_EXPORT: 'Export',
      INTERNAL_SAVE: 'Save',
    };
    if (!layerType) {
      return 'Processing';
    }
    return labels[layerType] ?? layerType.replaceAll('_', ' ').toLowerCase();
  }

  protected layerStatusTone(layer: CreativePipelineLayerRun): 'neutral' | 'brand' | 'blue' | 'red' {
    if (layer.status === 'COMPLETED') return 'brand';
    if (layer.status === 'PROCESSING') return 'blue';
    if (layer.status === 'FAILED') return 'red';
    return 'neutral';
  }

  constructor() {
    void this.store.loadGeneratorContext();
    this.loadHierarchyContext();
    this.store.updateDraft(this.buildDraft());
    if (this.auth.activeWorkspaceId()) {
      void this.workspaceCredits.loadAccount(this.auth.activeWorkspaceId()!);
    }

    this.generationForm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        this.formRevision.update((revision) => revision + 1);
        this.store.updateDraft(this.buildDraft());
        void this.refreshCampaignReadiness();
      });
  }

  ngOnDestroy(): void {
    this.store.stopPolling();
    this.revokeLocalProductImagePreview();
  }

  @HostListener('document:click')
  protected closeContextDropdown(): void {
    this.openContextDropdown.set(null);
  }

  @HostListener('document:keydown.escape')
  protected closeContextDropdownOnEscape(): void {
    this.openContextDropdown.set(null);
  }

  protected toggleContextDropdown(kind: ContextDropdown, disabled = false): void {
    if (disabled) {
      return;
    }
    this.openContextDropdown.update((openKind) => openKind === kind ? null : kind);
  }

  protected selectBrand(brandId: string): void {
    this.openContextDropdown.set(null);
    this.selectedBrandId.set(brandId);
    this.selectedProductId.set('');
    this.setSelectedProject('');
    this.ensureAllowedLanguageForSelectedBrand();
    void this.refreshCampaignReadiness();
  }

  protected selectProduct(productId: string): void {
    this.openContextDropdown.set(null);
    this.selectedProductId.set(productId);
    this.setSelectedProject('');
    void this.refreshCampaignReadiness();
  }

  protected selectPlatform(platform: PromptPlatform): void {
    this.generationForm.controls.platform.setValue(platform);
  }

  protected selectLanguage(language: PromptLanguage): void {
    this.openContextDropdown.set(null);
    if (!isLanguageAllowed(language, this.selectedBrand()?.languagePreference ?? 'BOTH')) {
      this.notifications.info('Language unavailable for this brand', 'Update the brand language preference or choose an allowed language.');
      this.ensureAllowedLanguageForSelectedBrand();
      return;
    }
    this.generationForm.controls.language.setValue(language);
  }

  protected selectPreset(preset: CreativePreset): void {
    this.selectedVisualFormat.set(preset.id);
    this.generationForm.patchValue({
      creativeType: preset.type,
      outputFormat: preset.format,
      width: preset.width,
      height: preset.height,
      duration: null,
    });
    void this.refreshCampaignReadiness();
  }

  protected selectQuality(quality: ModelQuality): void {
    this.selectedQuality.set(quality);
    void this.refreshCampaignReadiness();
  }

  protected toggleAsset(asset: Asset): void {
    if (!isImageAsset(asset)) {
      this.notifications.info('Select an image asset', 'JPG, PNG, and WebP product images work best for creative generation.');
      return;
    }
    this.selectedProductImageFile.set(null);
    this.revokeLocalProductImagePreview();
    this.selectSingleProductAsset(asset);
    this.closeAssetPicker();
  }

  protected async openAssetPicker(): Promise<void> {
    if (!this.auth.activeWorkspaceId()) {
      this.notifications.info('Workspace required', 'Select a workspace before choosing an image.');
      return;
    }
    this.assetPickerOpen.set(true);
    await this.store.loadGeneratorContext(this.assetPickerSearch(), this.selectedProjectId() || null);
  }

  protected closeAssetPicker(): void {
    this.assetPickerOpen.set(false);
  }

  protected async searchAssetPicker(search: string): Promise<void> {
    this.assetPickerSearch.set(search);
    await this.store.loadGeneratorContext(search, this.selectedProjectId() || null);
  }

  protected removeSelectedProductImage(): void {
    this.store.clearSelectedAssets();
    this.selectedProductImageFile.set(null);
    this.readinessRequestKey.set(null);
    this.revokeLocalProductImagePreview();
    void this.refreshCampaignReadiness();
  }

  protected onAdvancedImageSelected(
    kind: 'logo' | 'reference' | 'mask',
    event: Event,
  ): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      return;
    }
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      this.notifications.info('Unsupported image type', 'Please upload a PNG, JPG, JPEG, or WEBP image under 10 MB.');
      return;
    }
    if (file.size <= 0 || file.size > 10 * 1024 * 1024) {
      this.notifications.info('Image size is invalid', 'Please upload an image under 10 MB.');
      return;
    }
    if (kind === 'mask' && file.type !== 'image/png') {
      this.notifications.info('PNG mask required', 'Mask images should be PNG for background replacement.');
      return;
    }

    if (kind === 'logo') {
      this.selectedLogoImageFile.set(file);
    } else if (kind === 'reference') {
      this.selectedReferenceImageFile.set(file);
    } else {
      this.selectedMaskImageFile.set(file);
    }
    this.formRevision.update((revision) => revision + 1);
    void this.refreshCampaignReadiness();
  }

  protected removeAdvancedImage(kind: 'logo' | 'reference' | 'mask'): void {
    if (kind === 'logo') {
      this.selectedLogoImageFile.set(null);
    } else if (kind === 'reference') {
      this.selectedReferenceImageFile.set(null);
    } else {
      this.selectedMaskImageFile.set(null);
    }
    this.formRevision.update((revision) => revision + 1);
    void this.refreshCampaignReadiness();
  }

  protected async onAssetFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      return;
    }
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      this.notifications.info('Unsupported image type', 'Please upload a PNG, JPG, JPEG, or WEBP image under 10 MB.');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.notifications.info('Image is too large', 'Please upload a PNG, JPG, JPEG, or WEBP image under 10 MB.');
      return;
    }
    if (file.size <= 0) {
      this.notifications.info('Empty image file', 'Choose a non-empty JPG, PNG, or WebP product image.');
      return;
    }

    this.setLocalProductImagePreview(file);
    this.selectedProductImageFile.set(file);

    const projectId = this.selectedProjectId();
    if (!projectId) {
      this.store.clearSelectedAssets();
      void this.refreshCampaignReadiness();
      return;
    }

    const result = await this.assetStore.uploadProjectAsset(projectId, {
      file,
      assetCategory: 'PRODUCT_IMAGE',
      folderId: null,
      tags: ['creative-generator', 'product-image'],
      metadata: {
        source: 'creative-generator',
        purpose: 'creative-generation',
        brandId: this.selectedBrandId(),
        productId: this.selectedProductId(),
        projectId,
      },
    });

    if (!result.ok) {
      this.store.clearSelectedAssets();
      this.notifications.info(
        'Image selected for generation',
        result.message
          ? `Library upload failed: ${result.message} The image will still be sent directly with this generation.`
          : 'The image will be sent directly with this generation, but it was not saved to the asset library.',
      );
      void this.refreshCampaignReadiness();
      return;
    }

    const uploaded = this.assetStore.selectedAsset();
    if (uploaded) {
      this.selectSingleProductAsset(uploaded);
    }
    void this.store.loadGeneratorContext('', this.selectedProjectId() || null);
    void this.refreshCampaignReadiness();
  }

  protected async onSubmit(): Promise<void> {
    const value = this.generationForm.getRawValue();
    if (!this.selectedBrandId()) {
      this.notifications.info('Select a brand', 'Choose a brand before generating a creative.');
      return;
    }
    if (this.hasProductImageInput() && !this.selectedProductImageReady()) {
      this.notifications.info('Product image is not ready', 'Wait for the upload to finish or select a ready image.');
      return;
    }
    if (!value.sourcePrompt.trim() && !this.hasEnoughCreativeContext()) {
      this.notifications.info('Add campaign idea', 'Add a campaign idea or select product/campaign context with enough metadata.');
      return;
    }
    if (this.generationForm.invalid) {
      this.generationForm.markAllAsTouched();
      this.notifications.info('Fix highlighted fields', this.formValidationReason() ?? 'Review the highlighted fields before generating.');
      return;
    }

    const submitted = await this.store.submitAiCreative(this.buildAiCreativeRequest());
    if (submitted) {
      this.selectedVariationId.set(null);
      if (this.auth.activeWorkspaceId()) {
        void this.workspaceCredits.refreshAfterGeneration(this.auth.activeWorkspaceId()!);
      }
    }
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

  protected async copyJobId(): Promise<void> {
    const id = this.store.selectedRequest()?.id;
    if (!id) {
      return;
    }
    await navigator.clipboard?.writeText(id);
    this.notifications.success('Job ID copied', 'The generation job ID is ready to paste.');
  }

  protected buyCredits(): void {
    this.billingModal.show();
  }

  protected reduceGeneratedVersions(): void {
    const current = this.generationForm.controls.requestedVersionCount.value;
    this.generationForm.controls.requestedVersionCount.setValue(Math.max(1, current - 1));
  }

  protected sendForApproval(): void {
    void this.store.sendSelectedForApproval();
  }

  protected formatTime(value: string | null): string {
    if (!value) {
      return '--';
    }
    const timestamp = Date.parse(value);
    if (!Number.isFinite(timestamp)) {
      return '--';
    }
    const minutes = Math.max(0, Math.round((Date.now() - timestamp) / 60000));
    if (minutes < 1) {
      return 'Just now';
    }
    if (minutes < 60) {
      return `${minutes}m ago`;
    }
    const hours = Math.round(minutes / 60);
    return hours < 24 ? `${hours}h ago` : `${Math.round(hours / 24)}d ago`;
  }

  protected requestStatusLabel(): string {
    const request = this.store.selectedRequest();
    return request ? creativeGenerationStatusLabel(request.status) : 'Waiting';
  }

  protected requestStatusTone(): 'brand' | 'blue' | 'red' | 'neutral' {
    const request = this.store.selectedRequest();
    return request ? creativeGenerationStatusTone(request.status) : 'neutral';
  }

  protected creativeTypeLabel(type: CreativeType): string {
    return creativeTypeLabel(type);
  }

  protected creativeGenerationStatusLabel(status: CreativeGenerationRequest['status']): string {
    return creativeGenerationStatusLabel(status);
  }

  protected creativeGenerationStatusTone(status: CreativeGenerationRequest['status']): 'brand' | 'blue' | 'red' | 'neutral' {
    return creativeGenerationStatusTone(status);
  }

  protected platformLabel(platform: PromptPlatform | null): string {
    return promptPlatformLabel(platform);
  }

  protected languageLabel(language: PromptLanguage | ''): string {
    return promptLanguageLabel(language || null);
  }

  protected isSelectedAsset(assetId: string): boolean {
    return this.selectedAsset()?.id === assetId;
  }

  protected isSelectedOutput(outputId: string): boolean {
    return this.currentOutput()?.id === outputId;
  }

  private loadHierarchyContext(): void {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return;
    }

    void Promise.all([
      this.brandStore.load(workspaceId),
      this.productStore.load(workspaceId),
      this.projectStore.load(workspaceId),
    ]);
  }

  protected setSelectedProject(projectId: string): void {
    this.openContextDropdown.set(null);
    const currentProjectId = this.selectedProjectId();
    this.selectedProjectId.set(projectId);

    if (currentProjectId !== projectId) {
      if (this.store.selectedAssets().length > 0 || this.selectedProductImageFile()) {
        this.store.clearSelectedAssets();
        this.revokeLocalProductImagePreview();
      }
      this.readinessRequestKey.set(null);
    }
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
      generationConfig: {
        ...this.parseGenerationConfig(),
        brandId: this.selectedBrandId(),
        productServiceId: this.selectedProductId(),
        projectId: this.selectedProjectId(),
        tone: this.selectedTone(),
        modelQuality: this.selectedQuality(),
        estimatedCreditCost: this.creditCost(),
      },
      useBrandContext: value.useBrandContext,
    };
  }

  private buildCampaignCreativeRequest(): CreateCampaignCreativeRequest {
    const value = this.generationForm.getRawValue();
    const creativeFormat = this.resolvedCreativeFormat();
    if (!creativeFormat || value.language === 'MIXED' || !value.language || !value.platform) {
      throw new Error('Campaign creative payload is incomplete.');
    }
    const language: Exclude<PromptLanguage, 'MIXED'> = value.language;

    return {
      promptDraftId: value.promptHistoryId || null,
      sourcePrompt: value.sourcePrompt.trim(),
      productAssetId: this.productAssetId(),
      creativeFormat,
      platform: value.platform,
      language,
      qualityMode: this.qualityMode(),
      requestedVersionCount: value.requestedVersionCount,
      stylePreset: toneToStylePreset(this.selectedTone()),
      backgroundStyle: null,
      cta: value.cta.trim() || null,
    };
  }

  private buildAiCreativeRequest(): AiCreativeGenerateRequest {
    const value = this.generationForm.getRawValue();
    if (!this.auth.activeWorkspaceId() || !value.platform || value.language === 'MIXED' || !value.language) {
      throw new Error('AI creative payload is incomplete.');
    }

    const creativeType = mapVisualFormatToAiCreativeType(this.selectedVisualFormat());
    const outputFormat = mapOutputFormatToAi(value.outputFormat || 'PNG');

    return {
      workspaceId: this.auth.activeWorkspaceId()!,
      brandId: this.selectedBrandId(),
      productServiceId: this.selectedProductId() || undefined,
      campaignId: this.selectedProjectId() || undefined,
      promptTitlePreview: this.promptTitlePreview(),
      platform: mapPromptPlatformToAi(value.platform),
      language: mapPromptLanguageToAi(value.language),
      creativeType,
      tone: mapToneToAi(this.selectedTone()),
      modelQuality: this.selectedQuality() === 'Premium' ? 'PREMIUM' : 'BASIC',
      generationModeHint: this.generationModeHint(),
      campaignIdea: value.sourcePrompt.trim(),
      headline: value.headline.trim() || undefined,
      subheadline: value.subheadline.trim() || undefined,
      offerText: value.offerText.trim() || undefined,
      targetAudience: value.targetAudience.trim() || (this.selectedProduct()?.targetAudience ?? undefined),
      productDescription: this.selectedProduct()?.description ?? undefined,
      campaignObjective: this.selectedProject()?.campaignObjective ?? String(value.campaignObjective || ''),
      cta: value.cta.trim() || undefined,
      versions: value.requestedVersionCount,
      size: mapVisualFormatToAiSize(creativeType),
      quality: this.selectedQuality() === 'Premium' ? 'high' : 'medium',
      outputFormat,
      background: value.transparentBackground ? 'transparent' : 'opaque',
      noHumanModel: true,
      productImage: this.selectedProductImageFile() ?? undefined,
      existingAssetId: this.productAssetId() || undefined,
      logoImage: this.selectedLogoImageFile() ?? undefined,
      referenceImage: this.selectedReferenceImageFile() ?? undefined,
      maskImage: this.selectedMaskImageFile() ?? undefined,
      backgroundPrompt: value.backgroundPrompt.trim() || undefined,
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

  private qualityMode(): ImageCreativeQualityMode {
    return this.selectedQuality() === 'Premium' ? 'PREMIUM' : 'BASIC';
  }

  private hasEnoughCreativeContext(): boolean {
    return Boolean(
      this.selectedBrandId() &&
        (
          this.selectedProduct()?.description ||
          this.selectedProduct()?.sellingPoints ||
          this.selectedProject()?.campaignObjective ||
          this.selectedProject()?.description
        ),
    );
  }

  private formValidationReason(): string | null {
    const controls = this.generationForm.controls;
    if (controls.sourcePrompt.errors?.['maxlength']) {
      return 'Campaign idea must be 4000 characters or fewer.';
    }
    if (controls.cta.errors?.['maxlength']) {
      return 'CTA must be 40 characters or fewer.';
    }
    if (controls.headline.errors?.['maxlength']) {
      return 'Headline must be 80 characters or fewer.';
    }
    if (controls.subheadline.errors?.['maxlength']) {
      return 'Subheadline must be 120 characters or fewer.';
    }
    if (controls.offerText.errors?.['maxlength']) {
      return 'Offer text must be 80 characters or fewer.';
    }
    if (controls.targetAudience.errors?.['maxlength']) {
      return 'Target audience must be 150 characters or fewer.';
    }
    if (controls.backgroundPrompt.errors?.['maxlength']) {
      return 'Background prompt must be 500 characters or fewer.';
    }
    if (controls.generationConfigText.errors) {
      return 'Advanced generation config must be a valid JSON object.';
    }
    if (this.generationForm.errors?.['unsupportedOutputFormat']) {
      return 'Selected output format is not supported for this creative type.';
    }
    return null;
  }

  private selectSingleProductAsset(asset: Asset): void {
    this.store.selectedAssets().forEach((selected) => this.store.removeSelectedAsset(selected.id));
    if (!this.store.selectedAssets().some((selected) => selected.id === asset.id)) {
      this.store.toggleAsset(asset);
    }
    void this.ensureSelectedProductImagePreviewUrl(asset);
    void this.refreshCampaignReadiness();
  }

  private async ensureSelectedProductImagePreviewUrl(asset: Asset): Promise<void> {
    if (asset.previewUrl || asset.thumbnailUrl || asset.publicUrl) {
      return;
    }

    const preview = await this.assetStore.getPreviewUrl(asset.id);
    const url = preview?.url ?? null;

    if (url) {
      this.store.removeSelectedAsset(asset.id);
      this.store.toggleAsset({ ...asset, previewUrl: url });
    }
  }

  private async refreshCampaignReadiness(): Promise<void> {
    const projectId = this.selectedProjectId();
    const productAssetId = this.productAssetId();
    const value = this.generationForm.getRawValue();
    const key = [
      projectId,
      productAssetId,
      this.qualityMode(),
      value.requestedVersionCount,
      value.platform,
      value.language,
      this.resolvedCreativeFormat(),
    ].join(':');

    if (!projectId || !productAssetId || !this.selectedProductImageReady()) {
      this.readinessRequestKey.set(null);
      await this.refreshCampaignCostPreview();
      return;
    }

    if (this.readinessRequestKey() === key) {
      await this.refreshCampaignCostPreview();
      return;
    }

    this.readinessRequestKey.set(key);
    await this.store.checkCampaignCreativeReadiness(
      projectId,
      productAssetId,
      this.qualityMode(),
      value.requestedVersionCount,
    );
    await this.refreshCampaignCostPreview();
  }

  private async refreshCampaignCostPreview(): Promise<void> {
    const value = this.generationForm.getRawValue();
    if (
      !this.auth.activeWorkspaceId() ||
      !this.selectedBrandId() ||
      !value.platform ||
      !value.language ||
      value.language === 'MIXED' ||
      (!value.sourcePrompt.trim() && !this.hasEnoughCreativeContext())
    ) {
      this.store.clearCampaignCostPreview();
      return;
    }

    const creativeType = mapVisualFormatToAiCreativeType(this.selectedVisualFormat());
    const outputFormat = mapOutputFormatToAi(value.outputFormat || 'PNG');
    await this.store.previewAiCreativeCost({
      workspaceId: this.auth.activeWorkspaceId()!,
      brandId: this.selectedBrandId(),
      productServiceId: this.selectedProductId() || undefined,
      campaignId: this.selectedProjectId() || undefined,
      platform: mapPromptPlatformToAi(value.platform),
      creativeType,
      modelQuality: this.selectedQuality() === 'Premium' ? 'PREMIUM' : 'BASIC',
      versions: value.requestedVersionCount,
      hasProductImage: this.hasProductImageInput(),
      hasLogoImage: Boolean(this.selectedLogoImageFile()),
      hasReferenceImage: Boolean(this.selectedReferenceImageFile()),
      hasMaskImage: Boolean(this.selectedMaskImageFile()),
      outputFormat,
      background: value.transparentBackground ? 'transparent' : 'opaque',
      size: mapVisualFormatToAiSize(creativeType),
    });
  }

  private setLocalProductImagePreview(file: File): void {
    this.revokeLocalProductImagePreview();
    this.localProductImagePreviewUrl.set(URL.createObjectURL(file));
  }

  private revokeLocalProductImagePreview(): void {
    const url = this.localProductImagePreviewUrl();
    if (url) {
      URL.revokeObjectURL(url);
      this.localProductImagePreviewUrl.set(null);
    }
  }

  private ensureAllowedLanguageForSelectedBrand(): void {
    const preference = this.selectedBrand()?.languagePreference ?? 'BOTH';
    const current = this.generationForm.controls.language.value;
    if (current && current !== 'MIXED' && isLanguageAllowed(current, preference)) {
      return;
    }

    const fallback = preference === 'BANGLA' ? 'BANGLA' : 'ENGLISH';
    this.generationForm.controls.language.setValue(fallback);
  }

  private isSelectedLanguageAllowed(): boolean {
    const language = this.generationForm.controls.language.value;
    return Boolean(
      language &&
        language !== 'MIXED' &&
        isLanguageAllowed(language, this.selectedBrand()?.languagePreference ?? 'BOTH'),
    );
  }
}

function toneToStylePreset(tone: GeneratorTone): string {
  switch (tone) {
    case 'Promotional':
      return 'promotional';
    case 'Premium':
      return 'premium';
    case 'Friendly':
      return 'friendly';
    case 'Urgent':
      return 'urgent';
  }
}

function mapToneToAi(tone: GeneratorTone): AiCreativeGenerateRequest['tone'] {
  switch (tone) {
    case 'Promotional':
      return 'PROMOTIONAL';
    case 'Premium':
      return 'PREMIUM';
    case 'Friendly':
      return 'FRIENDLY';
    case 'Urgent':
      return 'URGENT';
  }
}

function mapPromptPlatformToAi(platform: PromptPlatform): AiCreativePlatform {
  switch (platform) {
    case 'FACEBOOK':
    case 'INSTAGRAM':
    case 'TIKTOK':
    case 'LINKEDIN':
      return platform;
    case 'OTHER':
      return 'OTHER';
    default:
      return 'OTHER';
  }
}

function mapPromptLanguageToAi(language: Exclude<PromptLanguage, 'MIXED'>): string {
  return language === 'BANGLA' ? 'bn' : 'en';
}

function mapVisualFormatToAiCreativeType(format: VisualCreativeFormat): AiCreativeType {
  return format;
}

function mapVisualFormatToAiSize(type: AiCreativeType): AiCreativeSize {
  switch (type) {
    case 'STORY':
      return '1024x1536';
    case 'BANNER':
      return '1536x1024';
    case 'SQUARE_POST':
    case 'PRODUCT_AD':
      return '1024x1024';
  }
}

function mapOutputFormatToAi(format: CreativeOutputFormat): AiCreativeGenerateRequest['outputFormat'] {
  switch (format) {
    case 'WEBP':
      return 'webp';
    case 'JPG':
      return 'jpeg';
    case 'PNG':
    default:
      return 'png';
  }
}

function visualCreativeFormatLabel(format: VisualCreativeFormat): string {
  switch (format) {
    case 'SQUARE_POST':
      return 'Square Post';
    case 'STORY':
      return 'Story';
    case 'BANNER':
      return 'Banner';
    case 'PRODUCT_AD':
      return 'Product Ad';
  }
}

function generationModeHintLabel(mode: NonNullable<AiCreativeGenerateRequest['generationModeHint']>): string {
  switch (mode) {
    case 'PRODUCT_IMAGE_CREATIVE':
    case 'PRODUCT_IMAGE_TO_CREATIVE':
      return 'Product-image creative';
    case 'MULTI_REFERENCE_CREATIVE':
    case 'MULTI_REFERENCE':
      return 'Multi-reference creative';
    case 'BACKGROUND_REPLACEMENT':
    case 'BACKGROUND_REPLACE':
      return 'Background replacement';
    case 'TRANSPARENT_ASSET':
      return 'Transparent asset';
    case 'TEXT_ONLY_CREATIVE':
    case 'TEXT_TO_CREATIVE':
    default:
      return 'Text-only creative';
  }
}

function parseAvailabilityPreview(toolCode: string | null): {
  status: 'READY' | 'MAY_BE_INSUFFICIENT' | 'UNAVAILABLE' | null;
  blockGeneration: boolean;
  message: string | null;
} {
  if (!toolCode?.startsWith('AI_CREATIVE:')) {
    return { status: null, blockGeneration: false, message: null };
  }
  const [, status, blocked, ...messageParts] = toolCode.split(':');
  const validStatus = status === 'READY' || status === 'MAY_BE_INSUFFICIENT' || status === 'UNAVAILABLE'
    ? status
    : null;
  return {
    status: validStatus,
    blockGeneration: blocked === 'BLOCKED',
    message: messageParts.join(':') || null,
  };
}

function resolveCreativeFormat(
  platform: PromptPlatform | null,
  selectedVisualFormat: VisualCreativeFormat,
): ImageCreativeFormat | null {
  if (!platform) {
    return null;
  }

  const key = `${platform}:${selectedVisualFormat}`;
  const mapping: Readonly<Record<string, ImageCreativeFormat>> = {
    'FACEBOOK:SQUARE_POST': 'FACEBOOK_SQUARE',
    'FACEBOOK:BANNER': 'FACEBOOK_BANNER',
    'INSTAGRAM:SQUARE_POST': 'INSTAGRAM_POST',
    'INSTAGRAM:STORY': 'INSTAGRAM_STORY',
    'TIKTOK:STORY': 'TIKTOK_VERTICAL',
    'TIKTOK:PRODUCT_AD': 'TIKTOK_PRODUCT_AD',
    'LINKEDIN:SQUARE_POST': 'LINKEDIN_POST',
    'LINKEDIN:BANNER': 'LINKEDIN_BANNER',
  };

  return mapping[key] ?? null;
}

function isLanguageAllowed(
  language: PromptLanguage,
  preference: 'BANGLA' | 'ENGLISH' | 'BOTH',
): boolean {
  if (language === 'MIXED') {
    return false;
  }
  return preference === 'BOTH' || preference === language;
}

function isGenerationReadyAsset(asset: Asset | null): boolean {
  return asset?.status === 'READY' || asset?.status === 'AVAILABLE';
}


