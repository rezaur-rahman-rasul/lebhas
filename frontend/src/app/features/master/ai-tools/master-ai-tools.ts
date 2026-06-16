import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { SectionHeaderComponent } from '@app/shared/components/section-header/section-header';
import { AiProviderView } from '../provider-settings/models/provider-credit-exchange.models';
import { ProviderSettingsStore } from '../provider-settings/state/providerSettings.store';
import { CreativePipelineLayerView, CreativePipelineView } from './creative-pipeline.models';
import { CreativePipelineStore } from './creative-pipeline.store';

type MasterAiToolMode = 'tools' | 'layers' | 'routing';

interface ToolBlueprint {
  readonly name: string;
  readonly code: string;
  readonly category: string;
  readonly basicCost: number;
  readonly premiumCost: number;
  readonly plans: string;
  readonly providerPolicy: string;
}

interface LayerBlueprint {
  readonly order: number;
  readonly name: string;
  readonly type: string;
  readonly qualityMode: string;
  readonly cost: string;
  readonly timeout: string;
}

const TOOL_BLUEPRINTS: readonly ToolBlueprint[] = [
  { name: 'Product Image Creative', code: 'product-image-creative', category: 'Image', basicCost: 8, premiumCost: 18, plans: 'Growth, Pro', providerPolicy: 'Quality weighted' },
  { name: 'Post Generator', code: 'post-generator', category: 'Text', basicCost: 2, premiumCost: 5, plans: 'Starter+', providerPolicy: 'Lowest stable cost' },
  { name: 'Captions', code: 'captions', category: 'Text', basicCost: 1, premiumCost: 3, plans: 'Starter+', providerPolicy: 'Balanced' },
  { name: 'Ad Copy', code: 'ad-copy', category: 'Text', basicCost: 3, premiumCost: 7, plans: 'Growth, Pro', providerPolicy: 'Conversion quality' },
  { name: 'Hashtags', code: 'hashtags', category: 'Text', basicCost: 1, premiumCost: 2, plans: 'Starter+', providerPolicy: 'Fastest' },
  { name: 'Product Video', code: 'product-video', category: 'Video', basicCost: 24, premiumCost: 48, plans: 'Pro', providerPolicy: 'Cost capped' },
  { name: 'Voiceover', code: 'voiceover', category: 'Audio', basicCost: 6, premiumCost: 14, plans: 'Growth, Pro', providerPolicy: 'Fallback required' },
  { name: 'Motion Creative', code: 'motion-creative', category: 'Motion', basicCost: 14, premiumCost: 32, plans: 'Pro', providerPolicy: 'Quality weighted' },
];

const LAYER_BLUEPRINTS: readonly LayerBlueprint[] = [
  { order: 1, name: 'Prompt Understanding', type: 'INPUT_UNDERSTANDING', qualityMode: 'Basic + Premium', cost: 'Low', timeout: '8s' },
  { order: 2, name: 'Brand Context Validation', type: 'BRAND_RULES', qualityMode: 'Required', cost: 'Low', timeout: '6s' },
  { order: 3, name: 'Asset Analysis', type: 'PRODUCT_ANALYSIS', qualityMode: 'Premium aware', cost: 'Medium', timeout: '18s' },
  { order: 4, name: 'Creative Composition', type: 'MODEL_GENERATION', qualityMode: 'Provider routed', cost: 'High', timeout: '45s' },
  { order: 5, name: 'Quality Scoring', type: 'QUALITY_CONTROL', qualityMode: 'Always on', cost: 'Medium', timeout: '12s' },
  { order: 6, name: 'Output Preparation', type: 'EXPORT', qualityMode: 'Format aware', cost: 'Low', timeout: '10s' },
];

@Component({
  selector: 'app-master-ai-tools',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    RouterLinkActive,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    IconComponent,
    SectionHeaderComponent,
  ],
  templateUrl: './master-ai-tools.html',
  styleUrl: './master-ai-tools.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MasterAiToolsPage {
  private readonly route = inject(ActivatedRoute);
  private readonly formBuilder = inject(FormBuilder).nonNullable;
  private readonly routeData = toSignal(this.route.data, { initialValue: this.route.snapshot.data });
  protected readonly permissions = inject(PermissionStore);
  protected readonly pipelines = inject(CreativePipelineStore);
  protected readonly providerSettings = inject(ProviderSettingsStore);

  protected readonly selectedPipelineId = signal<string | null>(null);
  protected readonly selectedLayerId = signal<string | null>(null);
  protected readonly toolBlueprints = TOOL_BLUEPRINTS;
  protected readonly layerBlueprints = LAYER_BLUEPRINTS;

  protected readonly mode = computed<MasterAiToolMode>(
    () => (this.routeData()['mode'] as MasterAiToolMode | undefined) ?? 'tools',
  );
  protected readonly title = computed(() => {
    switch (this.mode()) {
      case 'layers':
        return 'Creative Layers';
      case 'routing':
        return 'Provider Routing Policy';
      default:
        return 'Creative Tools';
    }
  });
  protected readonly description = computed(() => {
    switch (this.mode()) {
      case 'layers':
        return 'Manage generation pipeline layers, cost impact, quality mode, fallback and retry behavior.';
      case 'routing':
        return 'Configure provider routing rules by tool, quality mode, cost ceiling, timeout and circuit breaker.';
      default:
        return 'Manage user-facing creative tools, credit costs, package access and provider policy.';
    }
  });
  protected readonly pipelinesList = this.pipelines.pipelines;
  protected readonly backendTools = this.pipelines.tools;
  protected readonly backendLayers = this.pipelines.standaloneLayers;
  protected readonly routingPolicies = this.pipelines.routingPolicies;
  protected readonly providers = computed(() =>
    this.providerSettings.providers().filter((provider) => this.isAiProvider(provider)),
  );
  protected readonly activeProviders = computed(() =>
    this.providers().filter((provider) => provider.active),
  );
  protected readonly selectedPipeline = computed<CreativePipelineView | null>(
    () => this.pipelinesList().find((pipeline) => pipeline.id === this.selectedPipelineId()) ?? this.pipelinesList()[0] ?? null,
  );
  protected readonly selectedLayer = computed<CreativePipelineLayerView | null>(() => {
    const pipeline = this.selectedPipeline();
    return pipeline?.layers.find((layer) => layer.id === this.selectedLayerId()) ?? pipeline?.layers[0] ?? null;
  });
  protected readonly orderedLayers = computed(() =>
    [...(this.selectedPipeline()?.layers ?? this.backendLayers())].sort((left, right) => left.sortOrder - right.sortOrder),
  );
  protected readonly totalLayerCost = computed(() => {
    const total = this.orderedLayers().reduce((sum, layer) => {
      const maxCost = layer.costPolicies.find((policy) => policy.enabled)?.maxCostPerRun;
      return sum + (typeof maxCost === 'number' ? maxCost : 0);
    }, 0);
    return total > 0 ? total.toFixed(2) : '--';
  });
  protected readonly canSaveRouting = computed(
    () => Boolean(this.selectedPipeline() && this.selectedLayer() && this.activeProviders().length > 0),
  );

  protected readonly routingForm = this.formBuilder.group({
    tool: [''],
    qualityMode: ['Premium', Validators.required],
    primaryProvider: ['', Validators.required],
    fallbackProvider: [''],
    maxCost: [1.5, [Validators.required, Validators.min(0)]],
    timeout: [30, [Validators.required, Validators.min(1)]],
    retryCount: [2, [Validators.required, Validators.min(0)]],
    circuitBreakerEnabled: [true],
  });

  constructor() {
    void this.pipelines.load();
    void this.providerSettings.loadProviders();

    effect(() => {
      const pipeline = this.selectedPipeline();
      const layer = this.selectedLayer();
      const provider = this.activeProviders()[0] ?? this.providers()[0] ?? null;
      const fallback = this.activeProviders()[1] ?? null;

      this.selectedPipelineId.set(this.selectedPipelineId() ?? pipeline?.id ?? null);
      this.selectedLayerId.set(this.selectedLayerId() ?? layer?.id ?? null);
      this.routingForm.patchValue(
        {
          tool: pipeline?.id ?? '',
          primaryProvider: provider?.id ?? '',
          fallbackProvider: fallback?.id ?? '',
        },
        { emitEvent: false },
      );
    });
  }

  protected selectPipeline(event: Event): void {
    const pipelineId = (event.target as HTMLSelectElement).value || null;
    this.selectedPipelineId.set(pipelineId);
    this.selectedLayerId.set(null);
  }

  protected selectLayer(layerId: string): void {
    this.selectedLayerId.set(layerId);
  }

  protected providerName(providerId: string | null | undefined): string {
    return this.providerLabel(this.providers().find((provider) => provider.id === providerId) ?? null);
  }

  protected providerLabel(provider: AiProviderView | null | undefined): string {
    return provider?.displayName || provider?.providerName || provider?.providerCode || 'Provider pending';
  }

  protected layerProvider(layer: CreativePipelineLayerView): string {
    return this.providerName(layer.toolMappings.find((mapping) => mapping.enabled)?.providerId);
  }

  protected fallbackProvider(layer: CreativePipelineLayerView): string {
    return this.providerName(layer.toolMappings.find((mapping) => mapping.fallbackEligible)?.providerId);
  }

  protected layerCost(layer: CreativePipelineLayerView): string {
    const policy = layer.costPolicies.find((item) => item.enabled);
    return policy?.maxCostPerRun ? `${policy.currency} ${policy.maxCostPerRun}` : '--';
  }

  protected layerQuality(layer: CreativePipelineLayerView): string {
    const policy = layer.qualityPolicies.find((item) => item.enabled);
    return policy?.minQualityScore ? `Min ${policy.minQualityScore}` : 'Standard';
  }

  protected layerTimeout(layer: CreativePipelineLayerView): string {
    const timeout = layer.configuration['timeoutSeconds'] ?? layer.configuration['timeout'];
    return typeof timeout === 'number' || typeof timeout === 'string' ? `${timeout}s` : '--';
  }

  protected layerRetry(layer: CreativePipelineLayerView): string {
    return layer.retryable ? 'Retry enabled' : 'No retry';
  }

  protected pipelineForTool(tool: ToolBlueprint): CreativePipelineView | null {
    const normalize = (value: string) => value.toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();
    const toolCode = normalize(tool.code);
    const toolName = normalize(tool.name);

    return (
      this.pipelinesList().find((pipeline) => {
        const pipelineText = normalize(`${pipeline.pipelineCode} ${pipeline.pipelineName}`);
        return pipelineText.includes(toolCode) || pipelineText.includes(toolName);
      }) ?? null
    );
  }

  protected toolStatus(tool: ToolBlueprint): 'brand' | 'neutral' {
    return this.pipelineForTool(tool)?.active ? 'brand' : 'neutral';
  }

  protected saveRoutingPolicy(): void {
    const pipeline = this.selectedPipeline();
    const layer = this.selectedLayer();
    if (!pipeline || !layer || this.routingForm.invalid || !this.canSaveRouting()) {
      this.routingForm.markAllAsTouched();
      return;
    }

    const value = this.routingForm.getRawValue();
    void this.pipelines.configureRoutingPolicy(pipeline.id, layer.id, {
      policyCode: `routing-${layer.layerCode}-${String(value.qualityMode).toLowerCase()}`,
      routingStrategy: 'BALANCED',
      priorityOrder: 1,
      enabled: true,
      conditions: {
        qualityMode: value.qualityMode,
        circuitBreakerEnabled: value.circuitBreakerEnabled,
      },
      rules: {
        primaryProviderId: value.primaryProvider,
        fallbackProviderId: value.fallbackProvider || null,
        maxCost: value.maxCost,
        timeoutSeconds: value.timeout,
        retryCount: value.retryCount,
      },
    });
  }

  protected endpointUnavailableTitle(): string {
    switch (this.mode()) {
      case 'layers':
        return 'Creative layer API is not available yet';
      case 'routing':
        return 'Provider routing policy API is not available yet';
      default:
        return 'Creative tool configuration API is not available yet';
    }
  }

  protected loadErrorTitle(): string {
    switch (this.mode()) {
      case 'layers':
        return 'Creative layers could not load';
      case 'routing':
        return 'Provider routing policy could not load';
      default:
        return 'Creative tools could not load';
    }
  }

  private isAiProvider(provider: AiProviderView): boolean {
    const category = String(provider.providerCategory ?? provider.category ?? provider.providerType ?? '').toUpperCase();
    return category === 'AI' || provider.supportsText || provider.supportsImage || provider.supportsVideo || provider.supportsVoice;
  }
}
