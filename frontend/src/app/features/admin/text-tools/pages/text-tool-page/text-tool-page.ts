import { HttpContext } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { normalizeHttpError } from '@app/core/api/http-error';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import {
  CAMPAIGN_OBJECTIVE_OPTIONS,
  CampaignObjective,
  PLATFORM_OPTIONS,
  PROMPT_LANGUAGE_OPTIONS,
  PROMPT_TONE_OPTIONS,
  PromptLanguage,
  PromptPlatform,
  PromptTone,
  campaignObjectiveLabel,
  promptLanguageLabel,
  promptPlatformLabel,
} from '@app/features/admin/prompts/models/prompt.models';
import { BrandStore } from '@app/features/brands/brand.store';
import { ProductServiceStore } from '@app/features/product-services/product-service.store';
import { ProjectStore } from '@app/features/projects/project.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  TextToolHistoryItem,
  TextToolKind,
  TextToolQualityMode,
  TextToolRequest,
  TextToolResult,
} from '../../models/text-tool.models';
import { TextToolService } from '../../services/text-tool.service';

type TextToolField =
  | 'postGoal'
  | 'sourceIdea'
  | 'qualityMode'
  | 'captionStyle'
  | 'campaignObjective'
  | 'targetAudience'
  | 'offerDetails'
  | 'industry'
  | 'campaignTheme'
  | 'hashtagCount';

interface ToolConfig {
  readonly kind: TextToolKind;
  readonly title: string;
  readonly subtitle: string;
  readonly icon: string;
  readonly buttonLabel: string;
  readonly outputTitle: string;
  readonly fields: readonly TextToolField[];
  readonly creditCost: number;
  readonly tips: readonly string[];
}

const TOOL_CONFIGS: Record<TextToolKind, ToolConfig> = {
  post: {
    kind: 'post',
    title: 'Post Generator',
    subtitle: 'Turn a campaign idea into a clear social post with a headline and call to action.',
    icon: 'megaphone',
    buttonLabel: 'Generate Post',
    outputTitle: 'Generated post',
    fields: ['postGoal', 'sourceIdea', 'qualityMode'],
    creditCost: 4,
    tips: ['Keep the idea specific.', 'Use the campaign goal to shape the CTA.', 'Premium mode is best for final campaign copy.'],
  },
  caption: {
    kind: 'caption',
    title: 'Captions',
    subtitle: 'Create readable captions for social posts without needing writing training.',
    icon: 'pencil-line',
    buttonLabel: 'Generate Captions',
    outputTitle: 'Generated captions',
    fields: ['captionStyle', 'sourceIdea'],
    creditCost: 3,
    tips: ['Mention the product benefit.', 'Pick a style that matches the platform.', 'Short captions usually perform better on mobile.'],
  },
  'ads-copy': {
    kind: 'ads-copy',
    title: 'Ad Copy',
    subtitle: 'Draft primary text, headline, description, and CTA for a campaign.',
    icon: 'receipt-text',
    buttonLabel: 'Generate Ad Copy',
    outputTitle: 'Generated ad copy',
    fields: ['campaignObjective', 'targetAudience', 'offerDetails'],
    creditCost: 5,
    tips: ['Lead with the offer or outcome.', 'Audience context helps reduce generic copy.', 'Keep the CTA direct and easy to act on.'],
  },
  hashtags: {
    kind: 'hashtags',
    title: 'Hashtags',
    subtitle: 'Prepare grouped hashtags for discovery, Bangladesh reach, and campaign reuse.',
    icon: 'hash',
    buttonLabel: 'Generate Hashtags',
    outputTitle: 'Recommended hashtags',
    fields: ['industry', 'campaignTheme', 'hashtagCount'],
    creditCost: 2,
    tips: ['Mix broad and campaign-specific tags.', 'Use local tags only when they match the audience.', 'Avoid using too many unrelated tags.'],
  },
};

@Component({
  selector: 'app-text-tool-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    IconComponent,
    PageHeaderComponent,
  ],
  templateUrl: './text-tool-page.html',
  styleUrl: './text-tool-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TextToolPage {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly auth = inject(CurrentUserStore);
  private readonly workspace = inject(WorkspaceStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly service = inject(TextToolService);
  protected readonly brandStore = inject(BrandStore);
  protected readonly productStore = inject(ProductServiceStore);
  protected readonly projectStore = inject(ProjectStore);

  protected readonly config = TOOL_CONFIGS[this.route.snapshot.data['tool'] as TextToolKind] ?? TOOL_CONFIGS.post;
  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly languageOptions = PROMPT_LANGUAGE_OPTIONS;
  protected readonly toneOptions = PROMPT_TONE_OPTIONS;
  protected readonly objectiveOptions = CAMPAIGN_OBJECTIVE_OPTIONS;
  protected readonly postGoalOptions = ['Awareness', 'Engagement', 'Sales', 'Traffic', 'Leads'] as const;
  protected readonly captionStyleOptions = ['Short and clean', 'Story style', 'Question-led', 'Premium', 'Offer-focused'] as const;
  protected readonly qualityModes: readonly TextToolQualityMode[] = ['Basic', 'Premium'];

  protected readonly selectedBrandId = signal('');
  protected readonly selectedProductId = signal('');
  protected readonly selectedProjectId = signal('');
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly result = signal<TextToolResult | null>(null);
  protected readonly recentOutputs = signal<readonly TextToolHistoryItem[]>([]);

  protected readonly form = new FormGroup({
    platform: new FormControl<PromptPlatform>('INSTAGRAM', { nonNullable: true }),
    language: new FormControl<PromptLanguage>('ENGLISH', { nonNullable: true }),
    tone: new FormControl<PromptTone>('FRIENDLY', { nonNullable: true }),
    postGoal: new FormControl('Sales', { nonNullable: true }),
    sourceIdea: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(5000)],
    }),
    qualityMode: new FormControl<TextToolQualityMode>('Premium', { nonNullable: true }),
    captionStyle: new FormControl('Short and clean', { nonNullable: true }),
    campaignObjective: new FormControl<CampaignObjective>('SALES', { nonNullable: true }),
    targetAudience: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(180)] }),
    offerDetails: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(180)] }),
    industry: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(120)] }),
    campaignTheme: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(160)] }),
    hashtagCount: new FormControl(18, { nonNullable: true, validators: [Validators.min(3), Validators.max(40)] }),
  });

  protected readonly workspaceLabel = computed(
    () => this.auth.currentUser()?.workspaceName ?? this.auth.activeWorkspaceId() ?? 'Workspace not selected',
  );
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'ADMIN');
  protected readonly roleTone = computed(() =>
    this.auth.currentRole() === 'MASTER' ? 'red' : this.auth.currentRole() === 'CREW' ? 'blue' : 'brand',
  );
  protected readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));
  protected readonly filteredProducts = computed(() =>
    this.productStore.items().filter((product) => product.brandId === this.selectedBrandId()),
  );
  protected readonly filteredProjects = computed(() =>
    this.projectStore.items().filter((project) => project.productServiceId === this.selectedProductId()),
  );
  protected readonly selectedBrand = computed(() =>
    this.brandStore.items().find((brand) => brand.id === this.selectedBrandId()) ?? null,
  );
  protected readonly selectedProduct = computed(() =>
    this.productStore.items().find((product) => product.id === this.selectedProductId()) ?? null,
  );
  protected readonly selectedProject = computed(() =>
    this.projectStore.items().find((project) => project.id === this.selectedProjectId()) ?? null,
  );
  protected readonly creditsRemaining = computed(() => this.workspace.usage()?.creditsRemaining ?? null);
  protected readonly canGenerate = computed(() =>
    Boolean(this.selectedBrandId() && this.selectedProductId() && this.selectedProjectId()) &&
    this.form.valid &&
    !this.loading(),
  );
  protected readonly currentCreditCost = computed(() =>
    this.form.controls.qualityMode.value === 'Premium' && this.config.kind === 'post'
      ? this.config.creditCost + 2
      : this.config.creditCost,
  );
  protected readonly sourceIdeaLength = computed(() => this.form.controls.sourceIdea.value.length);

  constructor() {
    this.loadHierarchyContext();
    void this.loadHistory();
  }

  protected hasField(field: TextToolField): boolean {
    return this.config.fields.includes(field);
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

  protected async generate(): Promise<void> {
    if (!this.canGenerate()) {
      this.form.markAllAsTouched();
      this.error.set('Select brand, product, project, and complete the required idea fields.');
      return;
    }

    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      this.error.set('Select a workspace before using content tools.');
      return;
    }

    try {
      this.loading.set(true);
      this.error.set(null);
      const result = await firstValueFrom(
        this.service.generate(workspaceId, this.config.kind, this.buildPayload(), this.requestContext()),
      );
      this.result.set(result);
      this.recentOutputs.update((items) => [
        {
          id: result.id ?? `${result.tool}-${Date.now()}`,
          tool: result.tool,
          title: result.title,
          preview: result.sections[0]?.body ?? 'Generated output',
          createdAt: result.createdAt,
        },
        ...items,
      ].slice(0, 6));
      this.notifications.success(`${this.config.title} ready`, 'Your generated text is available to copy.');
    } catch (error) {
      this.error.set(this.mapError(error));
    } finally {
      this.loading.set(false);
    }
  }

  protected async copyText(value: string, label = 'Text'): Promise<void> {
    if (!value.trim()) {
      return;
    }

    await navigator.clipboard?.writeText(value);
    this.notifications.success(`${label} copied`, 'Ready to paste into your campaign workflow.');
  }

  protected async copyAll(): Promise<void> {
    const output = this.result();
    if (!output) {
      return;
    }

    await this.copyText(output.sections.map((section) => `${section.title}\n${section.body}`).join('\n\n'), 'Output');
  }

  protected async saveTemplate(): Promise<void> {
    const workspaceId = this.auth.activeWorkspaceId();
    const resultId = this.result()?.id;
    if (!workspaceId || !resultId) {
      this.error.set('Generate and save a backend result before saving it as a template.');
      return;
    }

    try {
      await firstValueFrom(this.service.saveTemplate(workspaceId, resultId, this.requestContext()));
      this.notifications.success('Template saved', 'This output is ready to reuse.');
    } catch (error) {
      this.error.set(this.mapError(error));
    }
  }

  protected sendToCreativeGenerator(): void {
    const output = this.result();
    void this.router.navigate(['/creative-generator'], {
      state: output ? { textToolOutput: output } : undefined,
    });
  }

  protected formatTime(value: string | null): string {
    if (!value) {
      return 'Recently';
    }

    const timestamp = Date.parse(value);
    if (!Number.isFinite(timestamp)) {
      return 'Recently';
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

  protected platformLabel(value: PromptPlatform): string {
    return promptPlatformLabel(value);
  }

  protected languageLabel(value: PromptLanguage): string {
    return promptLanguageLabel(value);
  }

  protected objectiveLabel(value: CampaignObjective): string {
    return campaignObjectiveLabel(value);
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

    const brand = this.brandStore.items()[0];
    if (brand && !this.selectedBrandId()) {
      this.selectBrand(brand.id);
    }
  }

  private async loadHistory(): Promise<void> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return;
    }

    try {
      const items = await firstValueFrom(
        this.service.history(workspaceId, this.config.kind, this.requestContext()),
      );
      this.recentOutputs.set(items);
    } catch {
      this.recentOutputs.set([]);
    }
  }

  private buildPayload(): TextToolRequest {
    const value = this.form.getRawValue();
    const brand = this.selectedBrand();
    const product = this.selectedProduct();
    const project = this.selectedProject();

    return {
      brandId: this.selectedBrandId(),
      brandName: brand?.name ?? '',
      productServiceId: this.selectedProductId(),
      productServiceName: product?.name ?? '',
      projectId: this.selectedProjectId(),
      projectName: project?.name ?? '',
      platform: value.platform,
      language: value.language,
      tone: value.tone,
      sourceIdea: value.sourceIdea.trim(),
      qualityMode: value.qualityMode,
      postGoal: value.postGoal,
      captionStyle: value.captionStyle,
      campaignObjective: value.campaignObjective,
      targetAudience: value.targetAudience.trim() || null,
      offerDetails: value.offerDetails.trim() || null,
      industry: value.industry.trim() || null,
      campaignTheme: value.campaignTheme.trim() || null,
      hashtagCount: value.hashtagCount,
    };
  }

  private mapError(error: unknown): string {
    const normalized = normalizeHttpError(error);

    if (normalized.status === 404) {
      return 'This text generation API is not available yet. The screen is ready and will use it when the backend endpoint is added.';
    }
    if (normalized.status === 403) {
      return 'You do not have access to this content tool in the current workspace.';
    }
    if (normalized.status >= 500 || normalized.message === 'Unexpected server error') {
      return 'Text generation is unavailable right now.';
    }

    return normalized.message;
  }

  private requestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}
