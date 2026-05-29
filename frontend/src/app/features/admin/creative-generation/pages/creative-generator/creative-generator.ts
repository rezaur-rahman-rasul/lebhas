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
import {
  Asset,
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
  CreativeGenerationDraft,
  CreativeGenerationRequest,
  CreativeOutput,
  CreativeOutputFormat,
  CreativeType,
  DEFAULT_GENERATION_DRAFT,
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

interface CreativePreset {
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
  ],
  templateUrl: './creative-generator.html',
  styleUrl: './creative-generator.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreativeGeneratorPage implements OnDestroy {
  protected readonly store = inject(CreativeGenerationStore);
  private readonly auth = inject(CurrentUserStore);
  private readonly workspace = inject(WorkspaceStore);
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
  protected readonly selectedQuality = signal<ModelQuality>('Premium');
  protected readonly selectedVariationId = signal<string | null>(null);
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
    { label: 'Square Post 1080x1080', type: 'STATIC_IMAGE', format: 'PNG', width: 1080, height: 1080 },
    { label: 'Story 1080x1920', type: 'STORY_CREATIVE', format: 'PNG', width: 1080, height: 1920 },
    { label: 'Banner', type: 'STATIC_IMAGE', format: 'WEBP', width: 1200, height: 628 },
    { label: 'Product Ad', type: 'CAROUSEL_IMAGE', format: 'PNG', width: 1080, height: 1080 },
  ] as const;
  protected readonly tones: readonly GeneratorTone[] = ['Promotional', 'Premium', 'Friendly', 'Urgent'];
  protected readonly qualities: readonly ModelQuality[] = ['Basic', 'Premium'];
  protected readonly platformOptions = [...PLATFORM_OPTIONS, { value: 'MORE' as const, label: 'More' }] as const;
  protected readonly languageOptions = PROMPT_LANGUAGE_OPTIONS;
  protected readonly formatFileSize = formatFileSize;
  protected readonly isImageAsset = isImageAsset;

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
  protected readonly promptLength = computed(() => this.generationForm.controls.sourcePrompt.value.length);
  protected readonly creditCost = computed(() => {
    const base = this.selectedQuality() === 'Premium' ? 18 : 8;
    return this.generationForm.controls.outputFormat.value === 'MP4' || this.generationForm.controls.outputFormat.value === 'MOV'
      ? base + 20
      : base;
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
    return 1250;
  });
  protected readonly creditsTotal = computed(() => this.creditLimit()?.limit ?? 5000);
  protected readonly creditPercent = computed(() => Math.min(100, Math.round((this.creditsUsed() / this.creditsTotal()) * 100)));
  protected readonly usageCards = computed(() => [
    { label: 'This Month Usage', value: `${this.creditsUsed()} / ${this.creditsTotal()}`, icon: 'gauge' },
    { label: 'Images Generated', value: String(this.store.generationRequests().filter((item) => item.outputFormat !== 'MP4' && item.outputFormat !== 'MOV').length), icon: 'image' },
    { label: 'Posts Generated', value: String(this.store.promptHistory().length), icon: 'megaphone' },
    { label: 'Videos Generated', value: String(this.store.generationRequests().filter((item) => item.outputFormat === 'MP4' || item.outputFormat === 'MOV').length), icon: 'video' },
    { label: 'Captions Generated', value: '--', icon: 'captions' },
  ]);

  constructor() {
    void this.store.loadGeneratorContext();
    this.loadHierarchyContext();
    this.store.updateDraft(this.buildDraft());

    this.generationForm.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.store.updateDraft(this.buildDraft()));
  }

  ngOnDestroy(): void {
    this.store.stopPolling();
  }

  protected selectBrand(brandId: string): void {
    this.selectedBrandId.set(brandId);
    const product = this.productStore.items().find((item) => item.brandId === brandId);
    this.selectedProductId.set(product?.id ?? '');
    const project = this.projectStore.items().find((item) => item.productServiceId === product?.id);
    this.selectedProjectId.set(project?.id ?? '');
  }

  protected selectProduct(productId: string): void {
    this.selectedProductId.set(productId);
    const project = this.projectStore.items().find((item) => item.productServiceId === productId);
    this.selectedProjectId.set(project?.id ?? '');
  }

  protected selectPlatform(platform: PromptPlatform | 'MORE'): void {
    if (platform === 'MORE') {
      this.notifications.info('More platforms are coming soon.', 'Use Facebook, Instagram, TikTok, or LinkedIn for now.');
      return;
    }
    this.generationForm.controls.platform.setValue(platform);
  }

  protected selectLanguage(language: PromptLanguage): void {
    this.generationForm.controls.language.setValue(language);
  }

  protected selectPreset(preset: CreativePreset): void {
    this.generationForm.patchValue({
      creativeType: preset.type,
      outputFormat: preset.format,
      width: preset.width,
      height: preset.height,
      duration: null,
    });
  }

  protected toggleAsset(asset: Asset): void {
    if (!isImageAsset(asset)) {
      this.notifications.info('Select an image asset', 'JPG, PNG, and WebP product images work best for creative generation.');
      return;
    }
    this.store.selectedAssets().forEach((selected) => {
      if (selected.id !== asset.id) {
        this.store.removeSelectedAsset(selected.id);
      }
    });
    this.store.toggleAsset(asset);
  }

  protected async onAssetFileSelected(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    input.value = '';
    if (!file) {
      return;
    }

    const allowedTypes = ['image/jpeg', 'image/png', 'image/webp'];
    if (!allowedTypes.includes(file.type)) {
      this.notifications.info('Unsupported image type', 'Upload a JPG, PNG, or WebP product image.');
      return;
    }

    const result = await this.assetStore.uploadAsset({
      file,
      assetCategory: 'PRODUCT_IMAGE',
      folderId: null,
      tags: ['creative-generator'],
      metadata: { source: 'creative-generator' },
    });

    if (!result.ok) {
      this.notifications.error('Upload failed', result.message ?? 'The product image could not be uploaded.');
      return;
    }

    const uploaded = this.assetStore.selectedAsset();
    if (uploaded) {
      this.toggleAsset(uploaded);
    }
    void this.store.loadGeneratorContext();
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
    if (!this.selectedAsset()) {
      this.notifications.info('Select a product image', 'Upload or select one image before generating.');
      return;
    }
    if (this.generationForm.invalid) {
      this.generationForm.markAllAsTouched();
      this.notifications.info('Complete the campaign idea', 'Add a clear campaign idea with at least 5 characters.');
      return;
    }

    const submitted = await this.store.submitGeneration(this.buildRequest());
    if (submitted) {
      this.selectedVariationId.set(null);
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
