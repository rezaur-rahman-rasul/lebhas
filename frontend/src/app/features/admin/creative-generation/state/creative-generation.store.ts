import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { normalizeHttpError } from '@app/core/api/http-error';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { Asset, DEFAULT_ASSET_FILTERS } from '@app/features/admin/assets/models/asset.models';
import { AssetService } from '@app/features/admin/assets/services/asset.service';
import { BrandProfile } from '@app/features/admin/workspace/models/brand-profile.models';
import { BrandProfileService } from '@app/features/admin/workspace/services/brand-profile.service';
import {
  DEFAULT_PROMPT_HISTORY_FILTERS,
  DEFAULT_PROMPT_HISTORY_PAGINATION,
  PromptHistory,
} from '@app/features/admin/prompts/models/prompt.models';
import { PromptService } from '@app/features/admin/prompts/services/prompt.service';
import {
  CreateCreativeGenerationRequest,
  CreateCampaignCreativeRequest,
  AiCreativeGenerateRequest,
  AiCreativeResponse,
  CreativeCreditPreviewRequest,
  CreativeProgressResponse,
  CreativeGenerationDraft,
  CreativeGenerationFilter,
  CreativeGenerationPagination,
  CreativeGenerationRequest,
  CreativeGenerationStatus,
  CreativeOutputFormat,
  CreativePipelineLayerRun,
  CreativePipelineRun,
  CreativeOutput,
  CreativeType,
  DEFAULT_CREATIVE_GENERATION_FILTERS,
  DEFAULT_CREATIVE_GENERATION_PAGINATION,
  DEFAULT_GENERATION_DRAFT,
  GenerationJob,
  ImageCreativeCostPreview,
  ImageCreativeQualityMode,
  ProductImageCreativeReadiness,
  isTerminalGenerationStatus,
} from '../models/creative-generation.models';
import {
  CreativeGenerationService,
  mapGenerationJobsFromRequest,
} from '../services/creative-generation.service';

const POLL_INTERVAL_MS = 3000;
const MAX_POLL_ATTEMPTS = 40;

@Injectable({ providedIn: 'root' })
export class CreativeGenerationStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly generationService = inject(CreativeGenerationService);
  private readonly assetService = inject(AssetService);
  private readonly brandProfileService = inject(BrandProfileService);
  private readonly promptService = inject(PromptService);
  private readonly workspace = inject(WorkspaceStore);

  private readonly generationRequestsSignal = signal<readonly CreativeGenerationRequest[]>([]);
  private readonly selectedRequestSignal = signal<CreativeGenerationRequest | null>(null);
  private readonly generationJobsSignal = signal<readonly GenerationJob[]>([]);
  private readonly creativeOutputsSignal = signal<readonly CreativeOutput[]>([]);
  private readonly selectedOutputSignal = signal<CreativeOutput | null>(null);
  private readonly currentStatusSignal = signal<CreativeGenerationStatus | null>(null);
  private readonly generationLoadingSignal = signal(false);
  private readonly generationErrorSignal = signal<string | null>(null);
  private readonly generationFormDraftSignal = signal<CreativeGenerationDraft>(
    DEFAULT_GENERATION_DRAFT,
  );
  private readonly generationFiltersSignal = signal<CreativeGenerationFilter>(
    DEFAULT_CREATIVE_GENERATION_FILTERS,
  );
  private readonly generationPaginationSignal = signal<CreativeGenerationPagination>(
    DEFAULT_CREATIVE_GENERATION_PAGINATION,
  );
  private readonly promptHistorySignal = signal<readonly PromptHistory[]>([]);
  private readonly availableAssetsSignal = signal<readonly Asset[]>([]);
  private readonly selectedAssetsSignal = signal<readonly Asset[]>([]);
  private readonly brandProfileSignal = signal<BrandProfile | null>(null);
  private readonly campaignReadinessSignal = signal<ProductImageCreativeReadiness | null>(null);
  private readonly campaignCostPreviewSignal = signal<ImageCreativeCostPreview | null>(null);
  private readonly campaignPipelineSignal = signal<CreativePipelineRun | null>(null);

  private pollingTimer: ReturnType<typeof setInterval> | null = null;
  private pollingAttempts = 0;
  private pollingRequestId: string | null = null;
  private pollingMode: 'legacy' | 'ai-progress' = 'legacy';

  readonly generationRequests = this.generationRequestsSignal.asReadonly();
  readonly selectedRequest = this.selectedRequestSignal.asReadonly();
  readonly generationJobs = this.generationJobsSignal.asReadonly();
  readonly creativeOutputs = this.creativeOutputsSignal.asReadonly();
  readonly selectedOutput = this.selectedOutputSignal.asReadonly();
  readonly currentStatus = this.currentStatusSignal.asReadonly();
  readonly generationLoading = this.generationLoadingSignal.asReadonly();
  readonly generationError = this.generationErrorSignal.asReadonly();
  readonly generationFormDraft = this.generationFormDraftSignal.asReadonly();
  readonly generationFilters = this.generationFiltersSignal.asReadonly();
  readonly generationPagination = this.generationPaginationSignal.asReadonly();
  readonly promptHistory = this.promptHistorySignal.asReadonly();
  readonly availableAssets = this.availableAssetsSignal.asReadonly();
  readonly selectedAssets = this.selectedAssetsSignal.asReadonly();
  readonly brandProfile = this.brandProfileSignal.asReadonly();
  readonly campaignReadiness = this.campaignReadinessSignal.asReadonly();
  readonly campaignCostPreview = this.campaignCostPreviewSignal.asReadonly();
  readonly campaignPipeline = this.campaignPipelineSignal.asReadonly();

  readonly hasGenerationRequests = computed(() => this.generationRequestsSignal().length > 0);
  readonly hasOutputs = computed(() => this.creativeOutputsSignal().length > 0);
  readonly completedOutputs = computed(() =>
    this.creativeOutputsSignal().filter((output) => output.status === 'COMPLETED'),
  );
  readonly failedRequests = computed(() =>
    this.generationRequestsSignal().filter((request) => request.status === 'FAILED'),
  );
  readonly activeGenerationJobs = computed(() =>
    this.generationJobsSignal().filter(
      (job) => job.status === 'QUEUED' || job.status === 'PROCESSING',
    ),
  );
  readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));
  readonly canSubmitGeneration = computed(() => {
    const draft = this.generationFormDraftSignal();
    return (
      this.canGenerate() &&
      draft.sourcePrompt.trim().length >= 5 &&
      Boolean(draft.creativeType && draft.platform && draft.campaignObjective && draft.outputFormat && draft.language)
    );
  });
  readonly canRetryGeneration = computed(() => {
    const request = this.selectedRequestSignal();
    return this.canGenerate() && Boolean(request && (request.status === 'FAILED' || request.status === 'CANCELLED'));
  });
  readonly canCancelGeneration = computed(() => {
    const request = this.selectedRequestSignal();
    return this.canGenerate() && Boolean(request && (request.status === 'QUEUED' || request.status === 'PROCESSING'));
  });
  readonly canViewGenerations = computed(() => this.canGenerate());
  readonly canDownloadOutputs = computed(() => this.hasPermission('CREATIVE_DOWNLOAD'));

  async loadGeneratorContext(assetSearch = '', projectId: string | null = null, showLoader = true): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    const loadContext = async () => {
      await Promise.all([
        this.fetchPromptHistory(workspaceId),
        this.fetchAvailableAssets(workspaceId, assetSearch, projectId),
        this.fetchBrandProfile(workspaceId),
        this.fetchGenerationRequests(workspaceId),
      ]);
    };

    if (showLoader) {
      await this.runLoader(loadContext);
      return;
    }

    await loadContext();
  }

  async loadGenerationRequests(filters?: CreativeGenerationFilter, page?: number): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    if (filters) {
      this.generationFiltersSignal.set(filters);
    }

    if (page !== undefined) {
      this.generationPaginationSignal.update((current) => ({ ...current, page }));
    }

    await this.runLoader(async () => {
      await this.fetchGenerationRequests(workspaceId);
    });
  }

  async loadRequestDetail(requestId: string): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.runLoader(async () => {
      const request = await firstValueFrom(
        this.generationService.getGeneration(workspaceId, requestId, this.requestContext()),
      );
      this.setSelectedRequest(request);
      this.generationRequestsSignal.update((items) => upsertRequest(items, request));
      await this.fetchOutputs(workspaceId, request.id);
    });
  }

  async loadOutputDetail(outputId: string): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.runLoader(async () => {
      const output = await firstValueFrom(
        this.generationService.getOutput(workspaceId, outputId, this.requestContext()),
      );
      this.selectedOutputSignal.set(output);
      this.creativeOutputsSignal.update((outputs) => upsertOutput(outputs, output));

      if (!this.selectedRequestSignal() || this.selectedRequestSignal()?.id !== output.requestId) {
        const request = await firstValueFrom(
          this.generationService.getGeneration(workspaceId, output.requestId, this.requestContext()),
        );
        this.setSelectedRequest(request);
      }
    });
  }

  async searchAssets(search: string): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.runLoader(async () => {
      await this.fetchAvailableAssets(workspaceId, search, null);
    });
  }

  updateDraft(draft: CreativeGenerationDraft): void {
    this.generationFormDraftSignal.set(draft);
  }

  selectPromptHistory(historyId: string | null): PromptHistory | null {
    const history = this.promptHistorySignal().find((item) => item.id === historyId) ?? null;
    this.generationFormDraftSignal.update((draft) => ({
      ...draft,
      promptHistoryId: history?.id ?? null,
      sourcePrompt: history?.sourcePrompt ?? draft.sourcePrompt,
      enhancedPrompt: history?.enhancedPrompt ?? draft.enhancedPrompt,
      platform: history?.platform ?? draft.platform,
      campaignObjective: history?.campaignObjective ?? draft.campaignObjective,
      language: history?.language ?? draft.language,
    }));
    return history;
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

  clearSelectedAssets(): void {
    this.selectedAssetsSignal.set([]);
    this.campaignReadinessSignal.set(null);
    this.campaignCostPreviewSignal.set(null);
  }

  setSelectedOutput(output: CreativeOutput | null): void {
    this.selectedOutputSignal.set(output);
  }

  clearSelectedRequest(): void {
    this.setSelectedRequest(null);
    this.creativeOutputsSignal.set([]);
  }

  async submitGeneration(payload: CreateCreativeGenerationRequest): Promise<boolean> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return false;
    }

    try {
      this.generationLoadingSignal.set(true);
      this.generationErrorSignal.set(null);
      const request = await firstValueFrom(
        this.generationService.submitGeneration(workspaceId, payload, this.requestContext()),
      );
      this.generationRequestsSignal.update((items) => upsertRequest(items, request));
      this.setSelectedRequest(request);
      this.creativeOutputsSignal.set([]);
      this.notifications.success('Generation queued', 'The creative request is now being processed.');
      this.startPolling(request.id);
      return true;
    } catch (error) {
      this.handleFailure(error);
      return false;
    } finally {
      this.generationLoadingSignal.set(false);
    }
  }

  async submitCampaignCreative(projectId: string, payload: CreateCampaignCreativeRequest): Promise<boolean> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return false;
    }

    try {
      this.generationLoadingSignal.set(true);
      this.generationErrorSignal.set(null);
      const result = await firstValueFrom(
        this.generationService.submitCampaignCreative(workspaceId, projectId, payload, this.requestContext()),
      );
      const request = mapCampaignResultToGenerationRequest(result.generation, payload);
      this.campaignPipelineSignal.set(result.pipeline);
      const outputs = await this.hydrateOutputAssetUrls(
        mapCampaignGeneratedVersionsToOutputs(result.generatedVersions, request, payload),
      );
      this.generationRequestsSignal.update((items) => upsertRequest(items, request));
      this.setSelectedRequest(request);
      this.creativeOutputsSignal.set(outputs);
      this.selectedOutputSignal.set(outputs[0] ?? null);
      this.generationErrorSignal.set(null);
      this.notifications.success(
        outputs.length > 0 ? 'Generation completed' : 'Generation queued',
        outputs.length > 0
          ? `${outputs.length} creative version${outputs.length === 1 ? '' : 's'} created.`
          : 'The campaign creative request is now being processed.',
      );
      await this.workspace.refreshActiveContext();
      if (outputs.length === 0) {
        this.startPolling(request.id);
      }
      return true;
    } catch (error) {
      this.handleFailure(error);
      return false;
    } finally {
      this.generationLoadingSignal.set(false);
    }
  }

  async submitAiCreative(payload: AiCreativeGenerateRequest): Promise<boolean> {
    try {
      this.generationLoadingSignal.set(true);
      this.generationErrorSignal.set(null);
      this.campaignPipelineSignal.set(mapAiProgressToPipeline({
        creativeId: 'planning',
        status: 'PLANNING',
        currentLayerKey: 'UNDERSTANDING_BRAND',
        currentLayerLabel: 'Understanding brand',
        layers: defaultAiProgressLayers(),
      }));

      const result = await firstValueFrom(
        this.generationService.generateCreative(payload, this.requestContext()),
      );
      const request = mapAiCreativeResponseToGenerationRequest(result, payload);
      const output = mapAiCreativeResponseToOutput(result, request, payload);

      this.generationRequestsSignal.update((items) => upsertRequest(items, request));
      this.setSelectedRequest(request);
      this.creativeOutputsSignal.set(output ? [output] : []);
      this.selectedOutputSignal.set(output);
      this.generationErrorSignal.set(result.errorMessage ?? null);

      await this.refreshAiProgress(result.creativeId);
      await this.workspace.refreshActiveContext();

      if (result.status === 'COMPLETED') {
        this.notifications.success('Generation completed', 'The creative preview is ready.');
      } else if (result.status === 'FAILED') {
        this.generationErrorSignal.set(result.errorMessage ?? 'Creative generation failed. Please try again.');
        this.notifications.error('Generation failed', this.generationErrorSignal() ?? 'Creative generation failed.');
      } else {
        this.notifications.success('Generation started', 'The creative request is now being processed.');
        this.startAiProgressPolling(result.creativeId);
      }

      return result.status !== 'FAILED';
    } catch (error) {
      this.handleFailure(error);
      return false;
    } finally {
      this.generationLoadingSignal.set(false);
    }
  }

  async checkCampaignCreativeReadiness(
    projectId: string,
    productAssetId: string | null,
    qualityMode: ImageCreativeQualityMode,
    requestedVersionCount: number,
  ): Promise<ProductImageCreativeReadiness | null> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return null;
    }

    try {
      const readiness = await firstValueFrom(
        this.generationService.getCampaignCreativeReadiness(
          workspaceId,
          projectId,
          productAssetId,
          qualityMode,
          requestedVersionCount,
          this.requestContext(),
        ),
      );
      this.campaignReadinessSignal.set(readiness);

      if (!readiness.ready) {
        const message = readiness.messages.length > 0
          ? readiness.messages.join(' ')
          : 'Creative generation setup is not ready.';
        this.generationErrorSignal.set(message);
      } else {
        this.generationErrorSignal.set(null);
      }

      return readiness;
    } catch (error) {
      const message = this.mapError(error);
      this.generationErrorSignal.set(message);
      this.notifications.error('Generation readiness failed', message);
      return null;
    }
  }

  async previewCampaignCreativeCost(
    projectId: string,
    payload: CreateCampaignCreativeRequest,
  ): Promise<ImageCreativeCostPreview | null> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return null;
    }

    try {
      const preview = await firstValueFrom(
        this.generationService.previewCampaignCreativeCost(
          workspaceId,
          projectId,
          payload,
          this.requestContext(),
        ),
      );
      this.campaignCostPreviewSignal.set(preview);
      return preview;
    } catch (error) {
      const message = this.mapError(error);
      this.campaignCostPreviewSignal.set(null);
      this.generationErrorSignal.set(message);
      return null;
    }
  }

  clearCampaignCostPreview(): void {
    this.campaignCostPreviewSignal.set(null);
  }

  async previewAiCreativeCost(payload: CreativeCreditPreviewRequest): Promise<ImageCreativeCostPreview | null> {
    try {
      const preview = await firstValueFrom(
        this.generationService.getCreditPreview(payload, this.requestContext()),
      );
      const mapped: ImageCreativeCostPreview = {
        toolCode: `AI_CREATIVE:${preview.creditStatus}:${preview.blockGeneration ? 'BLOCKED' : 'READY'}:${preview.message ?? ''}`,
        qualityMode: payload.modelQuality,
        requestedVersionCount: Math.max(1, Number(preview.requestedVersions ?? payload.versions)),
        unitCreditCost: null,
        totalCreditCost: null,
      };
      this.campaignCostPreviewSignal.set(mapped);
      this.generationErrorSignal.set(null);
      return mapped;
    } catch {
      this.campaignCostPreviewSignal.set(null);
      return null;
    }
  }

  async retrySelectedGeneration(): Promise<void> {
    const request = this.selectedRequestSignal();
    const workspaceId = this.resolveWorkspaceId();
    if (!request || !workspaceId) {
      return;
    }

    try {
      this.generationLoadingSignal.set(true);
      this.generationErrorSignal.set(null);
      const retried = await firstValueFrom(
        this.generationService.retryGeneration(workspaceId, request.id, this.requestContext()),
      );
      this.generationRequestsSignal.update((items) => upsertRequest(items, retried));
      this.setSelectedRequest(retried);
      this.notifications.success('Retry queued', 'The creative generation retry has started.');
      this.startPolling(retried.id);
    } catch (error) {
      this.handleFailure(error);
    } finally {
      this.generationLoadingSignal.set(false);
    }
  }

  async cancelSelectedGeneration(): Promise<void> {
    const request = this.selectedRequestSignal();
    const workspaceId = this.resolveWorkspaceId();
    if (!request || !workspaceId) {
      return;
    }

    try {
      this.generationLoadingSignal.set(true);
      this.generationErrorSignal.set(null);
      const cancelled = await firstValueFrom(
        this.generationService.cancelGeneration(workspaceId, request.id, this.requestContext()),
      );
      this.generationRequestsSignal.update((items) => upsertRequest(items, cancelled));
      this.setSelectedRequest(cancelled);
      this.stopPolling();
      this.notifications.info('Generation cancelled', 'The request is no longer active.');
    } catch (error) {
      this.handleFailure(error);
    } finally {
      this.generationLoadingSignal.set(false);
    }
  }

  async refreshSelectedRequest(): Promise<void> {
    const request = this.selectedRequestSignal();
    const workspaceId = this.resolveWorkspaceId();
    if (!request || !workspaceId) {
      return;
    }

    const latest = await firstValueFrom(
      this.generationService.getGeneration(workspaceId, request.id, this.requestContext()),
    );
    this.generationRequestsSignal.update((items) => upsertRequest(items, latest));
    this.setSelectedRequest(latest);

    if (latest.status === 'COMPLETED') {
      await this.fetchOutputs(workspaceId, latest.id);
    }
    await this.fetchCampaignPipeline(workspaceId, latest.id);
  }

  async sendSelectedForApproval(): Promise<boolean> {
    const request = this.selectedRequestSignal();
    if (!request || request.status !== 'COMPLETED') {
      this.notifications.info('Creative is not ready', 'Only completed creatives can be sent for approval.');
      return false;
    }

    try {
      await firstValueFrom(
        this.generationService.sendForApproval(request.id, this.requestContext()),
      );
      this.notifications.success('Sent for approval', 'The creative was submitted to the approval queue.');
      return true;
    } catch (error) {
      const message = this.mapError(error);
      this.generationErrorSignal.set(message);
      this.notifications.error('Approval submission failed', message);
      return false;
    }
  }

  async openPreviewUrl(output: CreativeOutput): Promise<string | null> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return null;
    }

    if (output.previewUrl) {
      this.generationErrorSignal.set(null);
      return output.previewUrl;
    }

    try {
      const response = output.generatedAssetId
        ? await firstValueFrom(
            this.assetService.getPreviewUrl(workspaceId, output.generatedAssetId, this.requestContext()),
          )
        : await firstValueFrom(
            this.generationService.getPreviewUrl(workspaceId, output.id, this.requestContext()),
          );
      this.patchOutput(output.id, { previewUrl: response.url });
      this.generationErrorSignal.set(null);
      return response.url;
    } catch (error) {
      this.handleFailure(error);
      return null;
    }
  }

  async openDownloadUrl(output: CreativeOutput): Promise<string | null> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return null;
    }

    if (output.downloadUrl) {
      this.generationErrorSignal.set(null);
      return output.downloadUrl;
    }

    try {
      const response = output.generatedAssetId
        ? await firstValueFrom(
            this.assetService.getDownloadUrl(workspaceId, output.generatedAssetId, this.requestContext()),
          )
        : await firstValueFrom(
            this.generationService.getDownloadUrl(workspaceId, output.id, this.requestContext()),
          );
      this.patchOutput(output.id, { downloadUrl: response.url });
      this.generationErrorSignal.set(null);
      return response.url;
    } catch (error) {
      this.handleFailure(error);
      return null;
    }
  }

  startPolling(requestId: string): void {
    this.stopPolling();
    this.pollingMode = 'legacy';
    this.pollingRequestId = requestId;
    this.pollingAttempts = 0;
    this.pollingTimer = setInterval(() => {
      void this.pollOnce();
    }, POLL_INTERVAL_MS);
  }

  startAiProgressPolling(creativeId: string): void {
    this.stopPolling();
    this.pollingMode = 'ai-progress';
    this.pollingRequestId = creativeId;
    this.pollingAttempts = 0;
    this.pollingTimer = setInterval(() => {
      void this.pollOnce();
    }, 2000);
  }

  stopPolling(): void {
    if (this.pollingTimer) {
      clearInterval(this.pollingTimer);
    }
    this.pollingTimer = null;
    this.pollingRequestId = null;
    this.pollingAttempts = 0;
  }

  private async pollOnce(): Promise<void> {
    const requestId = this.pollingRequestId;
    const workspaceId = this.auth.activeWorkspaceId();
    if (!requestId || !workspaceId) {
      this.stopPolling();
      return;
    }

    this.pollingAttempts += 1;
    if (this.pollingAttempts > MAX_POLL_ATTEMPTS) {
      this.stopPolling();
      return;
    }

    try {
      if (this.pollingMode === 'ai-progress') {
        const progress = await this.refreshAiProgress(requestId);
        if (!progress || progress.status === 'COMPLETED' || progress.status === 'FAILED') {
          this.stopPolling();
        }
        return;
      }

      const latest = await firstValueFrom(
        this.generationService.getGeneration(workspaceId, requestId, this.requestContext()),
      );
      this.generationRequestsSignal.update((items) => upsertRequest(items, latest));
      this.setSelectedRequest(latest);

      if (latest.status === 'COMPLETED') {
        await this.fetchOutputs(workspaceId, latest.id);
      }
      await this.fetchCampaignPipeline(workspaceId, latest.id);

      if (isTerminalGenerationStatus(latest.status)) {
        this.stopPolling();
      }
    } catch (error) {
      this.generationErrorSignal.set(this.mapError(error));
      this.stopPolling();
    }
  }

  private async fetchGenerationRequests(workspaceId: string): Promise<void> {
    const result = await firstValueFrom(
      this.generationService.listGenerations(
        workspaceId,
        this.generationFiltersSignal(),
        this.generationPaginationSignal().page,
        this.generationPaginationSignal().size,
        this.requestContext(),
      ),
    );
    this.generationRequestsSignal.set(result.items);
    this.generationPaginationSignal.set(result.pagination);
  }

  private async fetchOutputs(workspaceId: string, requestId: string): Promise<void> {
    const outputs = await firstValueFrom(
      this.generationService.listOutputs(workspaceId, requestId, this.requestContext()),
    );
    this.creativeOutputsSignal.set(await this.hydrateOutputAssetUrls(outputs));
    this.generationErrorSignal.set(null);
  }

  private async fetchCampaignPipeline(workspaceId: string, requestId: string): Promise<void> {
    try {
      const pipeline = await firstValueFrom(
        this.generationService.getCampaignCreativePipeline(workspaceId, requestId, this.requestContext()),
      );
      this.campaignPipelineSignal.set(pipeline);
    } catch {
      this.campaignPipelineSignal.set(null);
    }
  }

  private async refreshAiProgress(creativeId: string): Promise<CreativeProgressResponse | null> {
    try {
      const progress = await firstValueFrom(
        this.generationService.getCreativeProgress(creativeId, this.requestContext()),
      );
      this.campaignPipelineSignal.set(mapAiProgressToPipeline(progress));

      const selected = this.selectedRequestSignal();
      if (selected?.id === creativeId) {
        const status = mapAiStatusToGenerationStatus(progress.status);
        this.generationRequestsSignal.update((items) => upsertRequest(items, { ...selected, status, updatedAt: new Date().toISOString() }));
        this.setSelectedRequest({ ...selected, status, updatedAt: new Date().toISOString() });
      }

      return progress;
    } catch {
      return null;
    }
  }

  private async hydrateOutputAssetUrls(outputs: readonly CreativeOutput[]): Promise<readonly CreativeOutput[]> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId || outputs.length === 0) {
      return outputs;
    }

    return Promise.all(
      outputs.map(async (output) => {
        if (!output.generatedAssetId || output.previewUrl) {
          return output;
        }

        const [preview, download] = await Promise.all([
          firstValueFrom(
            this.assetService.getPreviewUrl(workspaceId, output.generatedAssetId, this.requestContext()),
          ).catch(() => null),
          firstValueFrom(
            this.assetService.getDownloadUrl(workspaceId, output.generatedAssetId, this.requestContext()),
          ).catch(() => null),
        ]);

        return {
          ...output,
          previewUrl: preview?.url ?? output.previewUrl,
          downloadUrl: download?.url ?? output.downloadUrl,
        };
      }),
    );
  }

  private patchOutput(outputId: string, patch: Partial<CreativeOutput>): void {
    this.creativeOutputsSignal.update((outputs) =>
      outputs.map((output) => (output.id === outputId ? { ...output, ...patch } : output)),
    );

    const selected = this.selectedOutputSignal();
    if (selected?.id === outputId) {
      this.selectedOutputSignal.set({ ...selected, ...patch });
    }
  }

  private async fetchPromptHistory(workspaceId: string): Promise<void> {
    try {
      const result = await firstValueFrom(
        this.promptService.listHistory(
          workspaceId,
          DEFAULT_PROMPT_HISTORY_FILTERS,
          0,
          DEFAULT_PROMPT_HISTORY_PAGINATION.size,
          this.requestContext(),
        ),
      );
      this.promptHistorySignal.set(result.items);
    } catch {
      this.promptHistorySignal.set([]);
    }
  }

  private async fetchAvailableAssets(workspaceId: string, search: string, projectId: string | null): Promise<void> {
    try {
      const trimmedSearch = search.trim();
      const [productImages, referenceImages] = await Promise.all([
        firstValueFrom(
          this.assetService.listAssets(
            workspaceId,
            {
              ...DEFAULT_ASSET_FILTERS,
              assetCategory: 'PRODUCT_IMAGE',
              status: null,
              search: trimmedSearch,
            },
            0,
            20,
            this.requestContext(),
          ),
        ),
        firstValueFrom(
          this.assetService.listAssets(
            workspaceId,
            {
              ...DEFAULT_ASSET_FILTERS,
              assetCategory: 'REFERENCE_IMAGE',
              status: null,
              search: trimmedSearch,
            },
            0,
            20,
            this.requestContext(),
          ),
        ),
      ]);
      this.availableAssetsSignal.set(
        [...productImages.items, ...referenceImages.items]
          .filter((asset) => asset.status === 'READY' || asset.status === 'AVAILABLE')
          .filter((asset) => !asset.projectId || !projectId || asset.projectId === projectId),
      );
    } catch {
      this.availableAssetsSignal.set([]);
    }
  }

  private async fetchBrandProfile(workspaceId: string): Promise<void> {
    try {
      const profile = await firstValueFrom(
        this.brandProfileService.getBrandProfile(workspaceId, this.requestContext()),
      );
      this.brandProfileSignal.set(profile);
    } catch {
      this.brandProfileSignal.set(null);
    }
  }

  private setSelectedRequest(request: CreativeGenerationRequest | null): void {
    this.selectedRequestSignal.set(request);
    this.currentStatusSignal.set(request?.status ?? null);
    this.generationJobsSignal.set(request ? mapGenerationJobsFromRequest(request) : []);
  }

  private runLoader(operation: () => Promise<void>): Promise<void> {
    return (async () => {
      try {
        this.generationLoadingSignal.set(true);
        this.generationErrorSignal.set(null);
        await operation();
      } catch (error) {
        this.generationErrorSignal.set(this.mapError(error));
      } finally {
        this.generationLoadingSignal.set(false);
      }
    })();
  }

  private resolveWorkspaceId(): string | null {
    const workspaceId = this.auth.activeWorkspaceId();
    if (workspaceId) {
      return workspaceId;
    }

    this.generationErrorSignal.set('Select a workspace before opening creative generation.');
    return null;
  }

  private canGenerate(): boolean {
    return this.hasPermission('CREATIVE_GENERATE');
  }

  private hasPermission(permission: 'CREATIVE_GENERATE' | 'CREATIVE_DOWNLOAD'): boolean {
    return this.auth.permissions().includes(permission);
  }

  private handleFailure(error: unknown): void {
    const message = this.mapError(error);
    this.generationErrorSignal.set(message);
    this.notifications.error('Generation failed', message);
  }

  private mapError(error: unknown): string {
    const normalized = normalizeHttpError(error);
    const detailedMessage = normalized.errors
      .map((item) => [item.field, item.message].filter(Boolean).join(': '))
      .filter((message) => message.trim().length > 0)
      .join(' ');

    if (normalized.status === 403) {
      return 'You do not have access to creative generation in this workspace.';
    }

    if (normalized.status === 404) {
      return 'Creative generation data could not be found.';
    }

    if (detailedMessage) {
      return detailedMessage;
    }

    if (normalized.status >= 500 && normalized.message && normalized.message !== 'We could not load this data. Please try again.') {
      return normalized.message;
    }

    if (normalized.status >= 500 || normalized.message === 'Unexpected server error') {
      return 'Creative generation is unavailable right now.';
    }

    return normalized.message;
  }

  private requestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}

function upsertRequest(
  requests: readonly CreativeGenerationRequest[],
  request: CreativeGenerationRequest,
): readonly CreativeGenerationRequest[] {
  const withoutCurrent = requests.filter((item) => item.id !== request.id);
  return [request, ...withoutCurrent].sort(
    (left, right) => Date.parse(right.updatedAt) - Date.parse(left.updatedAt),
  );
}

function upsertOutput(
  outputs: readonly CreativeOutput[],
  output: CreativeOutput,
): readonly CreativeOutput[] {
  const withoutCurrent = outputs.filter((item) => item.id !== output.id);
  return [output, ...withoutCurrent];
}

function mapCampaignResultToGenerationRequest(
  generation: {
    readonly creativeRequestId: string;
    readonly workspaceId: string;
    readonly productAssetId: string;
    readonly platform: CreateCampaignCreativeRequest['platform'];
    readonly language: CreateCampaignCreativeRequest['language'];
    readonly request: Readonly<Record<string, unknown>> | null;
    readonly status: string;
    readonly createdAt: string;
  },
  payload: CreateCampaignCreativeRequest,
): CreativeGenerationRequest {
  const createdAt = generation.createdAt;
  return {
    id: generation.creativeRequestId,
    workspaceId: generation.workspaceId,
    userId: '',
    promptHistoryId: payload.promptDraftId,
    sourcePrompt: payload.sourcePrompt,
    enhancedPrompt: null,
    platform: generation.platform,
    campaignObjective: null,
    creativeType: 'STATIC_IMAGE',
    outputFormat: 'PNG',
    language: generation.language,
    brandContextSnapshot: {},
    assetContextSnapshot: [{ assetId: generation.productAssetId }],
    generationConfig: {
      ...(generation.request ?? {}),
      imageCreativeStatus: generation.status,
      productAssetId: generation.productAssetId,
      creativeFormat: payload.creativeFormat,
      qualityMode: payload.qualityMode,
      requestedVersionCount: payload.requestedVersionCount,
      stylePreset: payload.stylePreset,
      backgroundStyle: payload.backgroundStyle,
      cta: payload.cta,
    },
    status: generation.status === 'FAILED' ? 'FAILED' : generation.status === 'COMPLETED' ? 'COMPLETED' : 'QUEUED',
    aiProvider: null,
    aiModel: null,
    requestedAt: createdAt,
    startedAt: null,
    completedAt: null,
    failedAt: generation.status === 'FAILED' ? createdAt : null,
    errorMessage: null,
    createdAt,
    updatedAt: createdAt,
  };
}

function mapCampaignGeneratedVersionsToOutputs(
  versions: readonly {
    readonly id: string;
    readonly workspaceId: string;
    readonly creativeRequestId: string;
    readonly assetId: string | null;
    readonly previewUrl?: string | null;
    readonly thumbnailUrl?: string | null;
    readonly versionNumber: number;
    readonly versionName: string;
    readonly generationStatus: string;
    readonly status: string;
    readonly createdAt: string;
    readonly updatedAt: string;
  }[],
  request: CreativeGenerationRequest,
  payload: CreateCampaignCreativeRequest,
): readonly CreativeOutput[] {
  return versions.map((version) => ({
    id: version.id,
    workspaceId: version.workspaceId,
    requestId: version.creativeRequestId,
    generatedAssetId: version.assetId,
    creativeType: request.creativeType,
    platform: payload.platform,
    outputFormat: 'PNG',
    width: null,
    height: null,
    duration: null,
    fileSize: null,
    previewUrl: version.previewUrl ?? version.thumbnailUrl ?? null,
    downloadUrl: null,
    caption: null,
    headline: version.versionName || `Generated creative ${version.versionNumber}`,
    ctaText: payload.cta ?? null,
    metadata: {
      generationStatus: version.generationStatus,
      imageCreativeFormat: payload.creativeFormat,
      qualityMode: payload.qualityMode,
      stylePreset: payload.stylePreset,
    },
    status: version.generationStatus === 'FAILED' ? 'FAILED' : 'COMPLETED',
    createdAt: version.createdAt,
    updatedAt: version.updatedAt,
  }));
}

function mapAiCreativeResponseToGenerationRequest(
  response: AiCreativeResponse,
  payload: AiCreativeGenerateRequest,
): CreativeGenerationRequest {
  const createdAt = response.createdAt || new Date().toISOString();
  const updatedAt = response.completedAt ?? createdAt;
  return {
    id: response.creativeId,
    workspaceId: response.workspaceId,
    userId: '',
    promptHistoryId: null,
    sourcePrompt: payload.campaignIdea ?? payload.headline ?? null,
    enhancedPrompt: null,
    platform: response.platform === 'OTHER' ? null : response.platform,
    campaignObjective: null,
    creativeType: mapAiCreativeType(response.creativeType),
    outputFormat: mapAiOutputFormat(response.outputFormat),
    language: mapAiLanguage(response.language ?? payload.language),
    brandContextSnapshot: {},
    assetContextSnapshot: payload.existingAssetId ? [{ assetId: payload.existingAssetId }] : [],
    generationConfig: {
      aiCreative: true,
      generationMode: response.generationMode,
      requestedVersions: response.requestedVersions,
      generatedVersionNo: response.generatedVersionNo ?? null,
      provider: response.provider,
      model: response.model,
      size: response.size,
      quality: response.quality,
      background: response.background,
      r2ObjectKey: response.r2ObjectKey ?? null,
      creditUsed: response.creditUsed ?? null,
      costEstimate: response.costEstimate ?? null,
    },
    status: mapAiStatusToGenerationStatus(response.status),
    aiProvider: response.provider,
    aiModel: response.model,
    requestedAt: createdAt,
    startedAt: createdAt,
    completedAt: response.completedAt ?? (response.status === 'COMPLETED' ? updatedAt : null),
    failedAt: response.status === 'FAILED' ? updatedAt : null,
    errorMessage: response.errorMessage ?? null,
    createdAt,
    updatedAt,
  };
}

function mapAiCreativeResponseToOutput(
  response: AiCreativeResponse,
  request: CreativeGenerationRequest,
  payload: AiCreativeGenerateRequest,
): CreativeOutput | null {
  if (!response.fileUrl && response.status !== 'COMPLETED') {
    return null;
  }

  const createdAt = response.createdAt || new Date().toISOString();
  const updatedAt = response.completedAt ?? createdAt;
  const [width, height] = response.size.split('x').map((item) => Number(item));
  return {
    id: response.creativeId,
    workspaceId: response.workspaceId,
    requestId: response.creativeId,
    generatedAssetId: null,
    creativeType: request.creativeType,
    platform: request.platform,
    outputFormat: request.outputFormat,
    width: Number.isFinite(width) ? width : null,
    height: Number.isFinite(height) ? height : null,
    duration: null,
    fileSize: null,
    previewUrl: response.fileUrl ?? response.thumbnailUrl ?? null,
    downloadUrl: response.fileUrl ?? null,
    caption: payload.campaignIdea ?? null,
    headline: payload.headline ?? payload.campaignIdea ?? 'Generated creative',
    ctaText: payload.cta ?? null,
    metadata: {
      aiCreative: true,
      provider: response.provider,
      model: response.model,
      quality: response.quality,
      background: response.background,
      generationMode: response.generationMode,
      creditUsed: response.creditUsed ?? null,
    },
    status: mapAiStatusToGenerationStatus(response.status),
    createdAt,
    updatedAt,
  };
}

function mapAiProgressToPipeline(progress: CreativeProgressResponse): CreativePipelineRun {
  const timestamp = new Date().toISOString();
  return {
    creativeRequestId: progress.creativeId,
    pipelineRunId: `${progress.creativeId}:ai-progress`,
    status: mapAiStatusToPipelineStatus(progress.status),
    strategy: 'OPENAI_IMAGE_API',
    primaryProviderCode: 'OPENAI',
    planJson: {},
    estimatedCreditCost: 0,
    actualCreditCost: null,
    failureReason: progress.layers.find((layer) => layer.status === 'FAILED')?.errorMessage ?? null,
    createdAt: timestamp,
    updatedAt: timestamp,
    completedAt: progress.status === 'COMPLETED' || progress.status === 'FAILED' ? timestamp : null,
    layers: progress.layers.map((layer) => ({
      id: `${progress.creativeId}:${layer.sequenceNo}:${layer.layerKey}`,
      sequence: layer.sequenceNo,
      layerType: mapAiLayerKey(layer.layerKey),
      providerCode: providerForAiLayer(layer.layerKey),
      modelCode: null,
      status: mapAiLayerStatusToPipelineStatus(layer.status),
      inputJson: {},
      outputJson: {},
      inputAssetIds: [],
      outputAssetIds: [],
      estimatedCost: 0,
      actualCost: null,
      startedAt: layer.startedAt ?? null,
      completedAt: layer.completedAt ?? null,
      failureReason: layer.errorMessage ?? null,
    })),
  };
}

function defaultAiProgressLayers(): CreativeProgressResponse['layers'] {
  return [
    { layerKey: 'UNDERSTANDING_BRAND', label: 'Understanding brand', sequenceNo: 1, status: 'PROCESSING' },
    { layerKey: 'BUILDING_PROMPT', label: 'Creating ad layout', sequenceNo: 2, status: 'PLANNED' },
    { layerKey: 'CALLING_OPENAI_GENERATION', label: 'Generating creative', sequenceNo: 3, status: 'PLANNED' },
    { layerKey: 'UPLOADING_TO_R2', label: 'Preparing output', sequenceNo: 4, status: 'PLANNED' },
  ];
}

function mapAiStatusToGenerationStatus(status: AiCreativeResponse['status']): CreativeGenerationStatus {
  switch (status) {
    case 'COMPLETED':
      return 'COMPLETED';
    case 'FAILED':
      return 'FAILED';
    case 'REQUESTED':
    case 'PLANNING':
      return 'QUEUED';
    case 'STARTED':
    case 'PROCESSING':
      return 'PROCESSING';
  }
}

function mapAiStatusToPipelineStatus(status: AiCreativeResponse['status']): CreativePipelineRun['status'] {
  switch (status) {
    case 'COMPLETED':
      return 'COMPLETED';
    case 'FAILED':
      return 'FAILED';
    case 'REQUESTED':
    case 'PLANNING':
      return 'PLANNED';
    case 'STARTED':
    case 'PROCESSING':
      return 'PROCESSING';
  }
}

function mapAiLayerStatusToPipelineStatus(status: CreativeProgressResponse['layers'][number]['status']): CreativePipelineRun['status'] {
  switch (status) {
    case 'COMPLETED':
      return 'COMPLETED';
    case 'FAILED':
      return 'FAILED';
    case 'PROCESSING':
      return 'PROCESSING';
    case 'PLANNED':
      return 'PLANNED';
  }
}

function mapAiLayerKey(layerKey: string): CreativePipelineLayerRun['layerType'] {
  const mapping: Readonly<Record<string, CreativePipelineLayerRun['layerType']>> = {
    UNDERSTANDING_BRAND: 'IMAGE_ANALYSIS',
    ANALYZING_PRODUCT_IMAGE: 'IMAGE_ANALYSIS',
    ANALYZING_PRODUCT: 'IMAGE_ANALYSIS',
    BUILDING_PROMPT: 'PROMPT_GENERATION',
    CALLING_OPENAI_GENERATION: 'IMAGE_GENERATION',
    CALLING_OPENAI_EDIT: 'IMAGE_GENERATION',
    DECODING_IMAGE: 'IMAGE_EXPORT',
    UPLOADING_TO_R2: 'IMAGE_EXPORT',
    COMPLETED: 'INTERNAL_SAVE',
  };
  return mapping[layerKey] ?? layerKey;
}

function providerForAiLayer(layerKey: string): string {
  return layerKey === 'UPLOADING_TO_R2' || layerKey === 'COMPLETED' ? 'INTERNAL' : 'OPENAI';
}

function mapAiCreativeType(type: AiCreativeResponse['creativeType']): CreativeType {
  switch (type) {
    case 'STORY':
      return 'STORY_CREATIVE';
    case 'PRODUCT_AD':
      return 'CAROUSEL_IMAGE';
    case 'SQUARE_POST':
    case 'BANNER':
      return 'STATIC_IMAGE';
  }
}

function mapAiOutputFormat(format: AiCreativeResponse['outputFormat']): CreativeOutputFormat {
  switch (format) {
    case 'jpeg':
      return 'JPG';
    case 'webp':
      return 'WEBP';
    case 'png':
      return 'PNG';
  }
}

function mapAiLanguage(language: string | null | undefined): CreativeGenerationRequest['language'] {
  if (!language) {
    return 'ENGLISH';
  }
  const normalized = language.toLowerCase();
  if (normalized === 'bn' || normalized === 'bangla' || normalized === 'bengali') {
    return 'BANGLA';
  }
  if (normalized === 'en' || normalized === 'english') {
    return 'ENGLISH';
  }
  return null;
}
