import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
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
  CreativeGenerationDraft,
  CreativeGenerationRequest,
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
  protected readonly selectedTone = signal<GeneratorTone>('Premium');
  protected readonly selectedQuality = signal<ModelQuality>('Basic');
  protected readonly selectedVisualFormat = signal<VisualCreativeFormat>('SQUARE_POST');
  protected readonly selectedVariationId = signal<string | null>(null);
  protected readonly localProductImagePreviewUrl = signal<string | null>(null);
  protected readonly assetPickerOpen = signal(false);
  protected readonly assetPickerSearch = signal('');
  private readonly readinessRequestKey = signal<string | null>(null);
  private readonly formRevision = signal(0);
  protected readonly generationSteps = ['Understanding brand', 'Analyzing product', 'Creating ad layout', 'Preparing output'] as const;
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
  protected readonly platformOptions = [...PLATFORM_OPTIONS, { value: 'MORE' as const, label: 'More' }] as const;
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
        validators: [Validators.required, Validators.minLength(5), Validators.maxLength(4000)],
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
      cta: new FormControl('Shop Now', {
        nonNullable: true,
        validators: [Validators.maxLength(160)],
      }),
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
    this.projectStore.items().filter((project) => project.productServiceId === this.selectedProductId()),
  );
  protected readonly selectedProject = computed(() =>
    this.projectStore.items().find((project) => project.id === this.selectedProjectId()) ?? null,
  );
  protected readonly selectedAsset = computed(() => this.store.selectedAssets()[0] ?? null);
  protected readonly productAssetId = computed(() => this.selectedAsset()?.id ?? '');
  protected readonly selectedProductImageReady = computed(() => isGenerationReadyAsset(this.selectedAsset()));
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
  protected readonly readinessChecklist = computed(() => {
    const readiness = this.store.campaignReadiness();
    return [
      { label: 'Project hierarchy ready', ready: readiness?.workspaceReady ?? false },
      { label: 'Package ready', ready: readiness?.packageReady ?? false },
      { label: 'Credits ready', ready: readiness?.creditsReady ?? false },
      { label: 'Provider ready', ready: readiness?.providerReady ?? false },
      { label: 'Routing ready', ready: readiness?.routingReady ?? false },
      { label: 'Product image ready', ready: readiness?.productAssetReady ?? false },
    ] as const;
  });
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
        this.selectedProductId() &&
        this.selectedProjectId() &&
        value.platform &&
        this.resolvedCreativeFormat() &&
        value.language &&
        value.language !== 'MIXED' &&
        this.isSelectedLanguageAllowed() &&
        this.qualityMode() &&
        this.productAssetId() &&
        this.selectedProductImageReady() &&
        this.store.campaignReadiness()?.ready === true &&
        this.store.campaignCostPreview() !== null &&
        value.sourcePrompt.trim().length > 0 &&
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
    if (!this.selectedProductId()) return 'Select a product or service.';
    if (!this.selectedProjectId()) return 'Select a project or campaign.';
    if (!value.platform) return 'Select a platform.';
    if (!this.resolvedCreativeFormat()) return 'Selected creative type is not supported for this platform.';
    if (!value.language || value.language === 'MIXED') return 'Select English or Bangla.';
    if (!this.isSelectedLanguageAllowed()) return 'Selected language does not match the brand language preference.';
    if (!this.productAssetId()) return 'Upload or select a product image first.';
    if (!this.selectedProductImageReady()) return 'Selected product image is not ready yet.';
    if (this.store.campaignReadiness()?.ready !== true) return 'Generation setup is not ready. Check the readiness checklist.';
    if (!value.sourcePrompt.trim()) return 'Add a campaign idea or prompt.';
    if (!this.packageReadyForSubmit()) return 'No active package is assigned to this workspace.';
    if (!this.store.campaignCostPreview()) return 'Credit preview is loading from the server.';
    if (!this.hasSufficientCredits()) return `Insufficient credits. This request needs ${this.generationCreditPreview().estimatedCreditCost} credits.`;
    if (this.uploadInProgress()) return 'Wait for image upload to finish.';
    if (this.store.generationLoading()) return 'Generation request is in progress.';
    if (this.generationForm.invalid) return 'Fix the highlighted form fields.';
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
    const requestedVersionCount = costPreview?.requestedVersionCount ?? this.generationForm.controls.requestedVersionCount.value;
    const estimatedCreditCost = costPreview?.totalCreditCost ?? 0;
    const availableCredits = this.workspaceCredits.creditAccount()?.availableCredits ?? this.workspace.usage()?.creditsRemaining ?? 0;
    const remainingCreditsAfterGeneration = availableCredits - estimatedCreditCost;
    const blockingReasons = [
      ...(costPreview ? [] : ['Credit preview is not loaded yet.']),
      ...(remainingCreditsAfterGeneration < 0 ? ['Not enough credits available.'] : []),
    ];
    return {
      requestedVersionCount,
      estimatedCreditCost,
      availableCredits,
      remainingCreditsAfterGeneration,
      packageLimit: this.creditsTotal(),
      canReserveCredits: blockingReasons.length === 0,
      canQueueGeneration: blockingReasons.length === 0 && this.store.campaignReadiness()?.ready === true,
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
    if (!this.store.campaignCostPreview()) {
      return false;
    }
    const credits = this.workspace.usage()?.creditsRemaining;
    return typeof credits !== 'number' || credits >= this.generationCreditPreview().estimatedCreditCost;
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

  protected selectBrand(brandId: string): void {
    this.selectedBrandId.set(brandId);
    const product = this.productStore.items().find((item) => item.brandId === brandId);
    this.selectedProductId.set(product?.id ?? '');
    const project = this.projectStore.items().find((item) => item.productServiceId === product?.id);
    this.setSelectedProject(project?.id ?? '');
    this.ensureAllowedLanguageForSelectedBrand();
    void this.refreshCampaignReadiness();
  }

  protected selectProduct(productId: string): void {
    this.selectedProductId.set(productId);
    const project = this.projectStore.items().find((item) => item.productServiceId === productId);
    this.setSelectedProject(project?.id ?? '');
    void this.refreshCampaignReadiness();
  }

  protected selectPlatform(platform: PromptPlatform | 'MORE'): void {
    if (platform === 'MORE') {
      this.notifications.info('More platforms are coming soon.', 'Use Facebook, Instagram, TikTok, or LinkedIn for now.');
      return;
    }
    this.generationForm.controls.platform.setValue(platform);
  }

  protected selectLanguage(language: PromptLanguage): void {
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
    this.readinessRequestKey.set(null);
    this.revokeLocalProductImagePreview();
  }

  protected async onAssetFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      return;
    }
    const projectId = this.selectedProjectId();
    if (!projectId) {
      this.notifications.info('Select a project or campaign', 'Choose the project before uploading a product image.');
      return;
    }

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      this.notifications.info('Unsupported image type', 'Upload a JPG, PNG, or WebP product image.');
      return;
    }
    if (file.size <= 0) {
      this.notifications.info('Empty image file', 'Choose a non-empty JPG, PNG, or WebP product image.');
      return;
    }

    this.setLocalProductImagePreview(file);

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
      this.revokeLocalProductImagePreview();
      this.notifications.error('Upload failed', result.message ?? 'The product image could not be uploaded.');
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
    if (!this.selectedBrandId()) {
      this.notifications.info('Select a brand', 'Choose a brand before generating a creative.');
      return;
    }
    if (!this.selectedProductId()) {
      this.notifications.info('Select a product or service', 'Choose a product/service before generating a creative.');
      return;
    }
    if (!this.selectedProjectId()) {
      this.notifications.info('Select a project or campaign', 'Choose a project/campaign before generating a creative.');
      return;
    }
    if (!this.productAssetId()) {
      this.notifications.info('Select a product image', 'Upload or select one image before generating.');
      return;
    }
    if (!this.selectedProductImageReady()) {
      this.notifications.info('Product image is not ready', 'Wait for the upload to finish or select a ready image.');
      return;
    }
    if (!this.resolvedCreativeFormat()) {
      this.notifications.info('Unsupported creative type', 'Choose a creative type supported by the selected platform.');
      return;
    }
    if (this.generationForm.invalid) {
      this.generationForm.markAllAsTouched();
      this.notifications.info('Complete the campaign idea', 'Add a clear campaign idea with at least 5 characters.');
      return;
    }

    const payload = this.buildCampaignCreativeRequest();
    const readiness = await this.store.checkCampaignCreativeReadiness(
      this.selectedProjectId(),
      payload.productAssetId,
      payload.qualityMode,
      payload.requestedVersionCount,
    );

    if (!readiness) {
      return;
    }

    if (!readiness.ready) {
      this.notifications.error(
        'Creative generation is not ready',
        readiness.messages.join(' ') || 'Check package, credits, provider routing, and selected product image.',
      );
      return;
    }

    const submitted = await this.store.submitCampaignCreative(this.selectedProjectId(), payload);
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
    this.notifications.info('Approval handoff', 'Open Generated Versions to submit completed creatives for approval.');
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
    ]).then(() => {
      const brand = this.brandStore.items()[0];
      if (brand && !this.selectedBrandId()) {
        this.selectBrand(brand.id);
      }
    });
  }

  protected setSelectedProject(projectId: string): void {
    const currentProjectId = this.selectedProjectId();
    this.selectedProjectId.set(projectId);

    if (currentProjectId !== projectId) {
      this.store.clearSelectedAssets();
      this.revokeLocalProductImagePreview();
    }

    void this.store.loadGeneratorContext('', projectId || null);
    void this.refreshCampaignReadiness();
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
      cta: value.cta.trim() || 'Shop Now',
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
      this.store.clearCampaignCostPreview();
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
    const projectId = this.selectedProjectId();
    const value = this.generationForm.getRawValue();
    if (
      !projectId ||
      !this.productAssetId() ||
      !this.selectedProductImageReady() ||
      !this.resolvedCreativeFormat() ||
      !value.platform ||
      !value.language ||
      value.language === 'MIXED' ||
      value.sourcePrompt.trim().length < 5
    ) {
      this.store.clearCampaignCostPreview();
      return;
    }

    await this.store.previewCampaignCreativeCost(projectId, this.buildCampaignCreativeRequest());
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


