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
import { ActivatedRoute, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BillingModalService } from '@app/features/payments/services/billing-modal.service';
import { CreditUsagePreviewCardComponent } from '@app/features/credits/components/credit-usage-preview-card/credit-usage-preview-card';
import { InsufficientCreditCardComponent } from '@app/features/credits/components/insufficient-credit-card/insufficient-credit-card';
import { WorkspaceCreditsStore } from '@app/features/credits/state/workspaceCredits.store';
import {
  Asset,
  DEFAULT_ASSET_FILTERS,
  assetStatusLabel,
  assetStatusTone,
  formatFileSize,
  isImageAsset,
} from '@app/features/admin/assets/models/asset.models';
import { AssetService } from '@app/features/admin/assets/services/asset.service';
import { AssetStore } from '@app/features/admin/assets/state/asset.store';
import { BrandStore } from '@app/features/brands/brand.store';
import { ProductServiceStore } from '@app/features/product-services/product-service.store';
import { ProjectStore } from '@app/features/projects/project.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
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
import { AiContextBuilderService } from '../../services/ai-context-builder.service';

type GeneratorTone = 'Promotional' | 'Premium' | 'Friendly' | 'Urgent';
type ModelQuality = 'Basic' | 'Premium';
type GenerationContextPreview = Readonly<{
  brand: string;
  product: string;
  campaign: string;
  language: string;
  audience: string;
  cta: string;
  colors: readonly string[];
  objective: string;
}>;
type HelpTopicKey =
  | 'language'
  | 'creativeType'
  | 'tone'
  | 'quality'
  | 'cta'
  | 'typography'
  | 'logo'
  | 'productImage'
  | 'advanced';
type HelpTopic = Readonly<{
  title: string;
  description: string;
  bestPractice: string;
  videoUrl: string;
  docsUrl: string;
  examples: readonly string[];
}>;
type VisualCreativeFormat = 'SQUARE_POST' | 'STORY' | 'BANNER' | 'PRODUCT_AD';
type ContextDropdown = 'brand' | 'product' | 'project' | 'language';
type ProductImagePromptPlatform = Exclude<PromptPlatform, 'OTHER'>;

const PRODUCT_IMAGE_PLATFORM_OPTIONS = PLATFORM_OPTIONS.filter(
  (option): option is { readonly value: ProductImagePromptPlatform; readonly label: string } =>
    option.value !== 'OTHER',
);

type BrowserSpeechRecognition = {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  start: () => void;
  stop: () => void;
  abort: () => void;
  onresult: ((event: SpeechRecognitionResultEvent) => void) | null;
  onerror: ((event: SpeechRecognitionErrorEvent) => void) | null;
  onend: (() => void) | null;
};

type SpeechRecognitionResultEvent = {
  resultIndex: number;
  results: {
    length: number;
    [index: number]: {
      isFinal: boolean;
      [index: number]: {
        transcript: string;
      };
    };
  };
};

type SpeechRecognitionErrorEvent = {
  error: string;
};

type SpeechRecognitionConstructor = new () => BrowserSpeechRecognition;

interface CreativePreset {
  readonly id: VisualCreativeFormat;
  readonly label: string;
  readonly placement: string;
  readonly type: CreativeType;
  readonly format: CreativeOutputFormat;
  readonly width: number;
  readonly height: number;
  readonly supportedPlatforms: readonly ProductImagePromptPlatform[];
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
    ModalShellComponent,
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
  private readonly route = inject(ActivatedRoute);
  protected readonly brandStore = inject(BrandStore);
  protected readonly productStore = inject(ProductServiceStore);
  protected readonly projectStore = inject(ProjectStore);
  protected readonly assetStore = inject(AssetStore);
  private readonly assetService = inject(AssetService);
  private readonly aiContextBuilder = inject(AiContextBuilderService);

  protected readonly selectedBrandId = signal<string>('');
  protected readonly selectedProductId = signal<string>('');
  protected readonly selectedProjectId = signal<string>('');
  protected readonly openContextDropdown = signal<ContextDropdown | null>(null);
  protected readonly selectedTone = signal<GeneratorTone>('Premium');
  protected readonly selectedQuality = signal<ModelQuality>('Basic');
  protected readonly selectedVisualFormat = signal<VisualCreativeFormat>('SQUARE_POST');
  protected readonly selectedVariationId = signal<string | null>(null);
  protected readonly previewLoadingOutputId = signal<string | null>(null);
  protected readonly previewUnavailableOutputId = signal<string | null>(null);
  protected readonly maxProductImages = 3;
  protected readonly selectedProductImageFile = signal<File | null>(null);
  protected readonly localProductImagePreviewUrl = signal<string | null>(null);
  protected readonly voiceListening = signal(false);
  protected readonly voiceSupported = typeof window !== 'undefined' && Boolean(getSpeechRecognitionConstructor());
  protected readonly advancedCreativeOptionsOpen = signal(false);
  protected readonly activeHelpTopic = signal<HelpTopicKey | null>(null);
  protected readonly selectedLogoImageFile = signal<File | null>(null);
  protected readonly brandLogoAsset = signal<Asset | null>(null);
  protected readonly brandLogoLoading = signal(false);
  protected readonly selectedReferenceImageFile = signal<File | null>(null);
  protected readonly selectedMaskImageFile = signal<File | null>(null);
  protected readonly assetPickerOpen = signal(false);
  protected readonly assetPickerSearch = signal('');
  protected readonly textOnlyConfirmOpen = signal(false);
  private readonly readinessRequestKey = signal<string | null>(null);
  private readonly formRevision = signal(0);
  protected readonly generationSteps = ['Understanding brand', 'Analyzing product', 'Creating ad layout', 'Preparing output'] as const;
  protected readonly visibleGenerationSteps = computed(() =>
    this.generationModeHint() === 'TEXT_ONLY_CREATIVE'
      ? ['Understanding brand', 'Text-only mode', 'Creating ad layout', 'Preparing output'] as const
      : this.generationSteps,
  );
  protected readonly tools = [
    { title: 'Post Generator', description: 'Create platform-ready post concepts.', icon: 'megaphone', route: '/post-generator' },
    { title: 'Captions', description: 'Write short, on-brand captions.', icon: 'pencil-line', route: '/captions' },
    { title: 'Ad Copy', description: 'Draft headlines and conversion copy.', icon: 'receipt-text', route: '/ad-copy' },
    { title: 'Hashtags', description: 'Prepare discoverable hashtag sets.', icon: 'hash', route: '/hashtags' },
    { title: 'Product Video', description: 'Plan product-focused motion ads.', icon: 'video', route: '/creative-generator' },
    { title: 'Voiceover', description: 'Prepare voiceover-ready scripts.', icon: 'mic-2', route: '/ad-copy' },
  ] as const;
  protected readonly creativePresets: readonly CreativePreset[] = [
    { id: 'SQUARE_POST', label: 'Square Post', placement: 'Feed', type: 'STATIC_IMAGE', format: 'PNG', width: 1080, height: 1080, supportedPlatforms: ['FACEBOOK', 'INSTAGRAM', 'LINKEDIN'] },
    { id: 'STORY', label: 'Story / Reel', placement: 'Vertical', type: 'STORY_CREATIVE', format: 'PNG', width: 1080, height: 1920, supportedPlatforms: ['INSTAGRAM', 'TIKTOK'] },
    { id: 'BANNER', label: 'Wide Banner', placement: 'Landscape', type: 'STATIC_IMAGE', format: 'WEBP', width: 1200, height: 628, supportedPlatforms: ['FACEBOOK', 'LINKEDIN'] },
    { id: 'PRODUCT_AD', label: 'Product Ad', placement: 'Feed', type: 'CAROUSEL_IMAGE', format: 'PNG', width: 1080, height: 1080, supportedPlatforms: ['TIKTOK'] },
  ] as const;
  protected readonly tones: readonly GeneratorTone[] = ['Promotional', 'Premium', 'Friendly', 'Urgent'];
  protected readonly qualities: readonly ModelQuality[] = ['Basic', 'Premium'];
  protected readonly platformOptions = PRODUCT_IMAGE_PLATFORM_OPTIONS;
  protected readonly languageOptions = PROMPT_LANGUAGE_OPTIONS.filter((option) => option.value !== 'MIXED');
  protected readonly allowedLanguageOptions = computed(() => {
    const preference = this.selectedBrand()?.languagePreference ?? 'BOTH';
    return this.languageOptions.filter((option) => isLanguageAllowed(option.value, preference));
  });
  protected readonly formatFileSize = formatFileSize;
  protected readonly isImageAsset = isImageAsset;
  protected readonly assetStatusLabel = assetStatusLabel;
  protected readonly assetStatusTone = assetStatusTone;
  protected readonly helpTopics: Readonly<Record<HelpTopicKey, HelpTopic>> = {
    language: {
      title: 'Language',
      description: 'Controls the language direction used in the generated creative prompt.',
      bestPractice: 'Match this to the brand language preference and the audience for the campaign.',
      videoUrl: '/help/tutorials/creative-generator-language',
      docsUrl: '/docs/creative-generator/language',
      examples: ['Bangla for Bangladesh-first consumer ads.', 'English for B2B or international audiences.'],
    },
    creativeType: {
      title: 'Creative Type',
      description: 'Chooses the output shape and platform placement format.',
      bestPractice: 'Pick the placement before writing the campaign idea so the prompt fits the layout.',
      videoUrl: '/help/tutorials/creative-type',
      docsUrl: '/docs/creative-generator/creative-type',
      examples: ['Square Post for feeds.', 'Story for vertical mobile placements.', 'Banner for landscape ads.'],
    },
    tone: {
      title: 'Tone',
      description: 'Sets the commercial style and emotional direction for the prompt.',
      bestPractice: 'Use Premium for polished product ads and Urgent for limited-time offers.',
      videoUrl: '/help/tutorials/creative-tone',
      docsUrl: '/docs/creative-generator/tone',
      examples: ['Premium: refined and clean.', 'Friendly: approachable and warm.'],
    },
    quality: {
      title: 'Quality',
      description: 'Controls the AI processing level and credit cost.',
      bestPractice: 'Use Basic for drafts and Premium for final campaign assets.',
      videoUrl: '/help/tutorials/creative-quality',
      docsUrl: '/docs/creative-generator/quality',
      examples: ['Basic for fast exploration.', 'Premium for higher visual quality.'],
    },
    cta: {
      title: 'CTA',
      description: 'Controls whether a call-to-action is included in the prompt.',
      bestPractice: 'Enable for ads. Disable for raw visual assets or brand imagery.',
      videoUrl: '/help/tutorials/creative-cta',
      docsUrl: '/docs/creative-generator/cta',
      examples: ['Order Now', 'Book Today', 'Learn More'],
    },
    typography: {
      title: 'Typography',
      description: 'Controls whether text appears in the generated creative.',
      bestPractice: 'Enable for ready-to-run ads. Disable when a designer needs an image-only visual.',
      videoUrl: '/help/tutorials/creative-typography',
      docsUrl: '/docs/creative-generator/typography',
      examples: ['On: headline, offer, CTA layout.', 'Off: no headlines, captions, CTA, or readable text.'],
    },
    logo: {
      title: 'Logo',
      description: 'Controls whether the selected brand logo from the brand definition is used in generation.',
      bestPractice: 'Enable when the brand has a logo configured. Disable when you want a logo-free visual.',
      videoUrl: '/help/tutorials/creative-logo',
      docsUrl: '/docs/creative-generator/logo',
      examples: ['On: use the brand logo.', 'Off: ignore logo and omit logo placement direction.'],
    },
    productImage: {
      title: 'Product Image',
      description: 'Adds an existing product image as the primary visual reference.',
      bestPractice: 'Use a clear product image for product ads. Leave empty for concept-only generation.',
      videoUrl: '/help/tutorials/product-image',
      docsUrl: '/docs/creative-generator/product-image',
      examples: ['Pack shot for ecommerce.', 'No image for brand awareness concepts.'],
    },
    advanced: {
      title: 'Advanced Settings',
      description: 'Optional fields for structured copy, references, background, and diagnostics.',
      bestPractice: 'Keep collapsed unless you need precise copy layers or reference assets.',
      videoUrl: '/help/tutorials/advanced-settings',
      docsUrl: '/docs/creative-generator/advanced-settings',
      examples: ['Headline and offer text.', 'Reference image.', 'Background replacement prompt.'],
    },
  };

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
        validators: [Validators.required, supportedGenerationOptionValidator(PRODUCT_IMAGE_PLATFORM_OPTIONS)],
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
      includeCta: new FormControl(false, { nonNullable: true }),
      includeLogo: new FormControl(false, { nonNullable: true }),
      includeTypography: new FormControl(true, { nonNullable: true }),
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
  protected readonly shouldShowBrandLogoUpload = computed(() =>
    this.generationForm.controls.includeLogo.value &&
    Boolean(this.selectedBrandId()) &&
    !this.brandLogoLoading() &&
    !this.brandLogoAsset(),
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
  protected readonly selectedProductAssets = computed(() => this.store.selectedAssets().slice(0, this.maxProductImages));
  protected readonly selectedAsset = computed(() => this.selectedProductAssets()[0] ?? null);
  protected readonly productAssetId = computed(() => this.selectedAsset()?.id ?? '');
  protected readonly productImageRequired = computed(() => false);
  protected readonly selectedProductImageReady = computed(() => {
    const assets = this.selectedProductAssets();
    return assets.length > 0 && assets.every((asset) => isGenerationReadyAsset(asset));
  });
  protected readonly hasProductImageInput = computed(() =>
    Boolean(this.selectedProductImageFile()) || this.selectedProductAssets().length > 0,
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
    const hasReference = Boolean(this.selectedReferenceImageFile());
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
  protected readonly aiGenerationContext = computed(() => {
    this.formRevision();
    const value = this.generationForm.getRawValue();
    return this.aiContextBuilder.build({
      brand: this.selectedBrand(),
      product: this.selectedProduct(),
      campaign: this.selectedProject(),
      logoAssetId: value.includeLogo ? this.brandLogoAsset()?.id ?? null : null,
      screen: {
        platform: value.platform ? promptPlatformLabel(value.platform) : null,
        language: value.language && value.language !== 'MIXED' ? promptLanguageLabel(value.language) : null,
        creativeType: visualCreativeFormatLabel(this.selectedVisualFormat()),
        tone: this.selectedTone(),
        quality: this.selectedQuality(),
        campaignIdea: value.sourcePrompt,
        includeCta: value.includeCta,
        includeLogo: value.includeLogo,
        includeTypography: value.includeTypography,
        headline: value.headline,
        subheadline: value.subheadline,
        offerText: value.offerText,
        cta: value.cta,
        targetAudience: value.targetAudience,
        backgroundPrompt: value.backgroundPrompt,
        transparentBackground: value.transparentBackground,
        versions: value.requestedVersionCount,
        productImage: this.hasProductImageInput(),
        referenceImage: Boolean(this.selectedReferenceImageFile()),
        maskImage: Boolean(this.selectedMaskImageFile()),
      },
    });
  });
  protected readonly generationContextPreview = computed<GenerationContextPreview>(() => {
    const context = this.aiGenerationContext();
    const resolved = context.resolved;
    return {
      brand: displayContextValue(resolved.brandName),
      product: displayContextValue(resolved.productName),
      campaign: displayContextValue(resolved.campaignName),
      language: displayContextValue(resolved.language),
      audience: displayContextValue(resolved.audience),
      cta: context.screen.includeCta === false ? 'Disabled' : displayContextValue(resolved.cta),
      colors: resolved.brandColors,
      objective: displayContextValue(resolved.objective),
    };
  });
  protected readonly readinessChecklist = computed(() => {
    const readiness = this.store.campaignReadiness();
    const productImageReady = this.productImageRequired()
      ? this.selectedProductImageReady()
      : true;
    return [
      {
        label: 'Project hierarchy ready',
        ready: Boolean(this.auth.activeWorkspaceId() && this.selectedBrandId() && this.selectedProductId() && this.selectedProjectId()),
      },
      { label: 'Package ready', ready: readiness?.packageReady ?? this.packageReadyForSubmit() },
      { label: 'Credits ready', ready: this.hasSufficientCredits() },
      { label: 'Provider ready', ready: readiness?.providerReady ?? true },
      { label: 'Routing ready', ready: readiness?.routingReady ?? true },
      { label: `Image input: ${this.imageInputModeLabel()}`, ready: productImageReady },
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
        (!this.productImageRequired() || this.hasProductImageInput()) &&
        (!this.hasProductImageInput() || this.selectedProductImageReady()) &&
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
    if (!this.selectedProductId()) return 'Select a product/service.';
    if (!this.selectedProjectId()) return 'Select a campaign.';
    if (!value.platform) return 'Select a platform.';
    if (!this.resolvedCreativeFormat()) return 'Select a supported platform and format combination.';
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

  constructor() {
    void this.store.loadGeneratorContext();
    void this.loadHierarchyContext();
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
    this.stopVoicePrompt();
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
    this.selectedLogoImageFile.set(null);
    this.brandLogoAsset.set(null);
    this.selectedProductId.set('');
    this.setSelectedProject('');
    this.ensureAllowedLanguageForSelectedBrand();
    void this.refreshBrandLogoAsset();
    void this.refreshCampaignReadiness();
  }

  protected selectProduct(productId: string): void {
    this.openContextDropdown.set(null);
    this.selectedProductId.set(productId);
    this.setSelectedProject('');
    void this.refreshCampaignReadiness();
  }

  protected selectPlatform(platform: PromptPlatform): void {
    const selectedPreset = this.creativePresets.find((preset) => preset.id === this.selectedVisualFormat());
    if (selectedPreset?.supportedPlatforms.includes(platform as ProductImagePromptPlatform)) {
      this.generationForm.controls.platform.setValue(platform);
      return;
    }

    const fallbackPreset = this.creativePresets.find((preset) =>
      preset.supportedPlatforms.includes(platform as ProductImagePromptPlatform),
    );
    if (!fallbackPreset) {
      this.generationForm.controls.platform.setValue(platform);
      return;
    }

    this.selectedVisualFormat.set(fallbackPreset.id);
    this.generationForm.patchValue({
      platform,
      creativeType: fallbackPreset.type,
      outputFormat: fallbackPreset.format,
      width: fallbackPreset.width,
      height: fallbackPreset.height,
      duration: null,
    });
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
    const currentPlatform = this.generationForm.controls.platform.value;
    const platform = preset.supportedPlatforms.includes(currentPlatform as ProductImagePromptPlatform)
      ? currentPlatform
      : preset.supportedPlatforms[0];
    this.generationForm.patchValue({
      platform,
      creativeType: preset.type,
      outputFormat: preset.format,
      width: preset.width,
      height: preset.height,
      duration: null,
    });
    void this.refreshCampaignReadiness();
  }

  protected submitFromKeyboard(event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    void this.onSubmit();
  }

  protected openHelp(topic: HelpTopicKey, event?: Event): void {
    event?.preventDefault();
    event?.stopPropagation();
    this.activeHelpTopic.set(topic);
  }

  protected closeHelp(): void {
    this.activeHelpTopic.set(null);
  }

  protected activeHelp(): HelpTopic | null {
    const topic = this.activeHelpTopic();
    return topic ? this.helpTopics[topic] : null;
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
    this.selectProductAsset(asset);
    if (this.selectedProductAssets().length >= this.maxProductImages) {
      this.closeAssetPicker();
    }
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

  protected removeProductAsset(assetId: string): void {
    this.store.removeSelectedAsset(assetId);
    this.readinessRequestKey.set(null);
    void this.refreshCampaignReadiness();
  }

  protected productAssetPreviewUrl(asset: Asset): string | null {
    return asset.thumbnailUrl || asset.previewUrl || asset.publicUrl || null;
  }

  protected async onBrandLogoSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      return;
    }
    if (!this.selectedBrandId()) {
      this.notifications.info('Select a brand first', 'Choose a brand before uploading a logo.');
      return;
    }

    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/svg+xml'];
    if (!allowedTypes.includes(file.type)) {
      this.notifications.info('Unsupported logo type', 'Upload a JPG, PNG, SVG, or WebP logo.');
      return;
    }
    if (file.size <= 0 || file.size > 5 * 1024 * 1024) {
      this.notifications.info('Logo size is invalid', 'Logo file must be 5 MB or smaller.');
      return;
    }

    this.selectedLogoImageFile.set(file);
    const result = await this.assetStore.uploadAsset({
      file,
      assetCategory: 'BRAND_LOGO',
      folderId: null,
      tags: ['brand-logo'],
      metadata: { brandId: this.selectedBrandId() },
    });

    if (!result.ok) {
      this.selectedLogoImageFile.set(null);
      this.notifications.info('Logo upload failed', result.message ?? 'The brand logo could not be uploaded.');
      return;
    }

    this.selectedLogoImageFile.set(null);
    await this.refreshBrandLogoAsset();
    void this.refreshCampaignReadiness();
  }

  protected onAdvancedImageSelected(
    kind: 'reference' | 'mask',
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

    if (kind === 'reference') {
      this.selectedReferenceImageFile.set(file);
    } else {
      this.selectedMaskImageFile.set(file);
    }
    this.formRevision.update((revision) => revision + 1);
    void this.refreshCampaignReadiness();
  }

  protected removeAdvancedImage(kind: 'reference' | 'mask'): void {
    if (kind === 'reference') {
      this.selectedReferenceImageFile.set(null);
    } else {
      this.selectedMaskImageFile.set(null);
    }
    this.formRevision.update((revision) => revision + 1);
    void this.refreshCampaignReadiness();
  }

  protected async onAssetFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    if (files.length === 0) {
      return;
    }
    const remainingSlots = this.maxProductImages - this.selectedProductAssets().length;
    if (remainingSlots <= 0) {
      this.notifications.info('Maximum images selected', `You can add up to ${this.maxProductImages} product images.`);
      return;
    }
    const selectedFiles = files.slice(0, remainingSlots);
    if (files.length > remainingSlots) {
      this.notifications.info('Image limit applied', `Only ${remainingSlots} more image${remainingSlots === 1 ? '' : 's'} can be added.`);
    }
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp'];

    const projectId = this.selectedProjectId();
    if (!projectId) {
      this.selectedProductImageFile.set(null);
      this.revokeLocalProductImagePreview();
      this.notifications.info('Select a project first', 'Creative image uploads are stored in the project Asset Library on R2. Select a project before uploading an image.');
      void this.refreshCampaignReadiness();
      return;
    }

    for (const file of selectedFiles) {
      if (!allowedTypes.includes(file.type)) {
        this.notifications.info('Unsupported image type', 'Please upload a PNG, JPG, JPEG, or WEBP image under 10 MB.');
        continue;
      }
      if (file.size > 10 * 1024 * 1024) {
        this.notifications.info('Image is too large', 'Please upload a PNG, JPG, JPEG, or WEBP image under 10 MB.');
        continue;
      }
      if (file.size <= 0) {
        this.notifications.info('Empty image file', 'Choose a non-empty JPG, PNG, or WebP product image.');
        continue;
      }

      this.setLocalProductImagePreview(file);
      this.selectedProductImageFile.set(file);

      const result = await this.assetStore.uploadProjectAssetSigned(projectId, {
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
        this.selectedProductImageFile.set(null);
        this.revokeLocalProductImagePreview();
        this.notifications.info(
          'Image upload failed',
          result.message
            ? `The product image was not stored in R2: ${result.message}`
            : 'The product image was not stored in R2. Check Asset Library/R2 configuration and try again.',
        );
        continue;
      }

      const uploaded = this.assetStore.selectedAsset();
      if (uploaded) {
        this.selectedProductImageFile.set(null);
        this.revokeLocalProductImagePreview();
        this.selectProductAsset(uploaded);
      }
    }
    void this.store.loadGeneratorContext('', this.selectedProjectId() || null);
    void this.refreshCampaignReadiness();
  }

  protected async onSubmit(): Promise<void> {
    await this.submitCreative(false);
  }

  protected closeTextOnlyConfirm(): void {
    this.textOnlyConfirmOpen.set(false);
  }

  protected async confirmTextOnlyGeneration(): Promise<void> {
    this.textOnlyConfirmOpen.set(false);
    await this.submitCreative(true);
  }

  private async submitCreative(textOnlyConfirmed: boolean): Promise<void> {
    const value = this.generationForm.getRawValue();
    if (!this.selectedBrandId()) {
      this.notifications.info('Select a brand', 'Choose a brand before generating a creative.');
      return;
    }
    if (!this.selectedProductId()) {
      this.notifications.info('Select a product/service', 'Choose a product or service before generating a creative.');
      return;
    }
    if (!this.selectedProjectId()) {
      this.notifications.info('Select a campaign', 'Choose a campaign before generating a creative.');
      return;
    }
    if (this.hasProductImageInput() && !this.selectedProductImageReady()) {
      this.notifications.info('Product image is not ready', 'Wait for the upload to finish or select a ready image.');
      return;
    }
    if (!this.hasProductImageInput() && !textOnlyConfirmed) {
      this.textOnlyConfirmOpen.set(true);
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

    const projectId = this.selectedProjectId();
    const submitted = projectId && this.productAssetId()
      ? await this.store.submitCampaignCreative(projectId, this.buildCampaignCreativeRequest())
      : await this.store.submitAiCreative(this.buildAiCreativeRequest());
    if (submitted) {
      this.selectedVariationId.set(null);
      this.previewUnavailableOutputId.set(null);
      if (this.auth.activeWorkspaceId()) {
        void this.workspaceCredits.refreshAfterGeneration(this.auth.activeWorkspaceId()!);
      }
    }
  }

  protected async previewOutput(output: CreativeOutput): Promise<void> {
    this.selectedVariationId.set(output.id);
    this.previewUnavailableOutputId.set(null);
    if (output.previewUrl) {
      return;
    }

    this.previewLoadingOutputId.set(output.id);
    try {
      await this.store.openPreviewUrl(output);
    } finally {
      if (this.previewLoadingOutputId() === output.id) {
        this.previewLoadingOutputId.set(null);
      }
    }
  }

  protected markPreviewUnavailable(outputId: string): void {
    this.previewUnavailableOutputId.set(outputId);
  }

  protected retryPreviewOutput(output: CreativeOutput): void {
    this.previewUnavailableOutputId.set(null);
    void this.previewOutput(output);
  }

  protected async downloadOutput(output: CreativeOutput): Promise<void> {
    const url = await this.store.openDownloadUrl(output);
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  }

  protected async openOutputDetail(output: CreativeOutput): Promise<void> {
    await this.previewOutput(output);
    const updatedOutput = this.store.creativeOutputs().find((item) => item.id === output.id) ?? output;
    this.store.setSelectedOutput(updatedOutput);
  }

  protected closeOutputDetail(): void {
    this.store.setSelectedOutput(null);
  }

  protected toggleVoicePrompt(): void {
    if (this.voiceListening()) {
      this.stopVoicePrompt();
      return;
    }

    this.startVoicePrompt();
  }

  protected voiceLanguageLabel(): string {
    return this.generationForm.controls.language.value === 'BANGLA' ? 'Bangla' : 'English';
  }

  protected isBanglaSelected(): boolean {
    return this.generationForm.controls.language.value === 'BANGLA';
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

  private async loadHierarchyContext(): Promise<void> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await Promise.all([
      this.brandStore.load(workspaceId),
      this.productStore.load(workspaceId),
      this.projectStore.load(workspaceId),
    ]);
    this.applyOnboardingContextFromQuery();
  }

  private applyOnboardingContextFromQuery(): void {
    const params = this.route.snapshot.queryParamMap;
    const brandId = params.get('brandId') ?? '';
    const productServiceId = params.get('productServiceId') ?? '';
    const projectId = params.get('projectId') ?? '';

    if (brandId && this.brandStore.items().some((brand) => brand.id === brandId)) {
      this.selectedBrandId.set(brandId);
      this.brandStore.selectBrand(brandId);
      this.ensureAllowedLanguageForSelectedBrand();
      void this.refreshBrandLogoAsset();
    }

    if (productServiceId && this.productStore.items().some((product) => product.id === productServiceId)) {
      this.selectedProductId.set(productServiceId);
      this.productStore.selectProductService(productServiceId);
    }

    if (projectId && this.projectStore.items().some((project) => project.id === projectId)) {
      this.setSelectedProject(projectId);
    }

    void this.refreshCampaignReadiness();
  }

  private async refreshBrandLogoAsset(): Promise<void> {
    const workspaceId = this.auth.activeWorkspaceId();
    const brandId = this.selectedBrandId();
    if (!workspaceId || !brandId) {
      this.brandLogoAsset.set(null);
      return;
    }

    this.brandLogoLoading.set(true);
    try {
      const page = await firstValueFrom(
        this.assetService.listAssets(
          workspaceId,
          {
            ...DEFAULT_ASSET_FILTERS,
            assetCategory: 'BRAND_LOGO',
            status: null,
            search: '',
          },
          0,
          50,
        ),
      );
      const logo = page.items.find((asset) =>
        String(asset.metadata?.['brandId'] ?? '') === brandId &&
        (asset.status === 'READY' || asset.status === 'AVAILABLE'),
      ) ?? null;
      this.brandLogoAsset.set(logo);
    } catch {
      this.brandLogoAsset.set(null);
    } finally {
      this.brandLogoLoading.set(false);
    }
  }

  private speechRecognition: BrowserSpeechRecognition | null = null;

  private startVoicePrompt(): void {
    const Recognition = getSpeechRecognitionConstructor();
    if (!Recognition) {
      this.notifications.info('Voice input unavailable', 'Your browser does not support speech recognition.');
      return;
    }

    this.stopVoicePrompt();
    const recognition = new Recognition();
    recognition.lang = this.generationForm.controls.language.value === 'BANGLA' ? 'bn-BD' : 'en-US';
    recognition.continuous = true;
    recognition.interimResults = false;
    recognition.onresult = (event) => this.appendSpeechResults(event);
    recognition.onerror = (event) => {
      this.voiceListening.set(false);
      this.notifications.info('Voice input stopped', this.voiceErrorMessage(event.error));
    };
    recognition.onend = () => {
      this.voiceListening.set(false);
      this.speechRecognition = null;
    };

    try {
      recognition.start();
      this.speechRecognition = recognition;
      this.voiceListening.set(true);
    } catch {
      this.voiceListening.set(false);
      this.speechRecognition = null;
      this.notifications.info('Voice input unavailable', 'Microphone access could not be started.');
    }
  }

  private stopVoicePrompt(): void {
    if (!this.speechRecognition) {
      this.voiceListening.set(false);
      return;
    }

    const recognition = this.speechRecognition;
    this.speechRecognition = null;
    recognition.onend = null;
    recognition.onerror = null;
    recognition.onresult = null;
    recognition.stop();
    this.voiceListening.set(false);
  }

  private appendSpeechResults(event: SpeechRecognitionResultEvent): void {
    const transcript = Array.from({ length: event.results.length - event.resultIndex }, (_, index) => {
      const result = event.results[event.resultIndex + index];
      return result.isFinal ? result[0]?.transcript?.trim() ?? '' : '';
    }).filter(Boolean).join(' ');

    if (!transcript) {
      return;
    }

    const control = this.generationForm.controls.sourcePrompt;
    const current = control.value.trim();
    const next = current ? `${current} ${transcript}` : transcript;
    control.setValue(next.slice(0, 4000));
  }

  private voiceErrorMessage(error: string): string {
    if (error === 'not-allowed' || error === 'service-not-allowed') {
      return 'Allow microphone permission to use voice input.';
    }
    if (error === 'no-speech') {
      return 'No speech was detected. Try again.';
    }
    if (error === 'language-not-supported') {
      return 'The selected voice language is not supported by this browser.';
    }
    return 'Speech recognition ended unexpectedly.';
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
    const context = this.aiGenerationContext();
    return {
      promptHistoryId: value.promptHistoryId || null,
      sourcePrompt: limitText(context.prompt, 32000),
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
        aiGenerationContext: context,
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
    const context = this.aiGenerationContext();

    return {
      promptDraftId: value.promptHistoryId || null,
      sourcePrompt: limitText(context.prompt, 4000),
      productAssetId: this.productAssetId() || null,
      logoAssetId: value.includeLogo ? this.brandLogoAsset()?.id ?? null : null,
      creativeFormat,
      platform: value.platform,
      language,
      qualityMode: this.qualityMode(),
      requestedVersionCount: value.requestedVersionCount,
      stylePreset: toneToStylePreset(this.selectedTone()),
      backgroundStyle: null,
      headline: value.includeTypography ? context.resolved.headline : null,
      subheadline: value.includeTypography ? context.resolved.subheadline : null,
      offerText: value.includeTypography ? context.resolved.offerText : null,
      cta: value.includeCta && value.includeTypography ? context.resolved.cta : null,
      includeCta: value.includeCta,
      includeTypography: value.includeTypography,
    };
  }

  private buildAiCreativeRequest(): AiCreativeGenerateRequest {
    const value = this.generationForm.getRawValue();
    if (!this.auth.activeWorkspaceId() || !value.platform || value.language === 'MIXED' || !value.language) {
      throw new Error('AI creative payload is incomplete.');
    }

    const creativeType = mapVisualFormatToAiCreativeType(this.selectedVisualFormat());
    const outputFormat = mapOutputFormatToAi(value.outputFormat || 'PNG');
    return this.aiContextBuilder.buildCreativePayload(this.aiGenerationContext(), {
      workspaceId: this.auth.activeWorkspaceId()!,
      brandId: this.selectedBrandId(),
      productServiceId: this.selectedProductId() || null,
      campaignId: this.selectedProjectId() || null,
      promptTitlePreview: this.promptTitlePreview(),
      platform: mapPromptPlatformToAi(value.platform),
      language: mapPromptLanguageToAi(value.language),
      creativeType,
      tone: mapToneToAi(this.selectedTone()),
      modelQuality: this.selectedQuality() === 'Premium' ? 'PREMIUM' : 'BASIC',
      generationModeHint: this.generationModeHint(),
      includeCta: value.includeCta,
      includeLogo: value.includeLogo,
      includeTypography: value.includeTypography,
      versions: value.requestedVersionCount,
      size: mapVisualFormatToAiSize(creativeType),
      quality: this.selectedQuality() === 'Premium' ? 'high' : 'medium',
      outputFormat,
      background: value.transparentBackground ? 'transparent' : 'opaque',
      noHumanModel: true,
      existingAssetId: this.productAssetId() || undefined,
      logoAssetId: value.includeLogo ? this.brandLogoAsset()?.id ?? undefined : undefined,
      referenceImage: this.selectedReferenceImageFile() ?? undefined,
      maskImage: this.selectedMaskImageFile() ?? undefined,
      backgroundPrompt: value.backgroundPrompt.trim() || undefined,
    });
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
        this.selectedProductId() &&
        this.selectedProjectId() &&
        (
          this.selectedProduct()?.name ||
          this.selectedProduct()?.description ||
          this.selectedProduct()?.sellingPoints ||
          this.selectedProject()?.name ||
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

  private selectProductAsset(asset: Asset): void {
    if (this.store.selectedAssets().some((selected) => selected.id === asset.id)) {
      void this.ensureSelectedProductImagePreviewUrl(asset);
      void this.refreshCampaignReadiness();
      return;
    }
    if (this.selectedProductAssets().length >= this.maxProductImages) {
      this.notifications.info('Maximum images selected', `You can add up to ${this.maxProductImages} product images.`);
      return;
    }

    this.store.toggleAsset(asset);
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
      !this.selectedProductId() ||
      !this.selectedProjectId() ||
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
      hasLogoImage: value.includeLogo,
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

function displayContextValue(value: string | null): string {
  return value?.trim() || 'Not configured';
}

function limitText(value: string, maxLength: number): string {
  return value.length > maxLength ? value.slice(0, maxLength).trimEnd() : value;
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

function getSpeechRecognitionConstructor(): SpeechRecognitionConstructor | null {
  if (typeof window === 'undefined') {
    return null;
  }

  const speechWindow = window as Window & {
    SpeechRecognition?: SpeechRecognitionConstructor;
    webkitSpeechRecognition?: SpeechRecognitionConstructor;
  };
  return speechWindow.SpeechRecognition ?? speechWindow.webkitSpeechRecognition ?? null;
}

function isGenerationReadyAsset(asset: Asset | null): boolean {
  return asset?.status === 'READY' || asset?.status === 'AVAILABLE';
}
