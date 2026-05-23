import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { normalizeHttpError } from '@app/core/api/http-error';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
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
  CreativeGenerationDraft,
  CreativeGenerationFilter,
  CreativeGenerationPagination,
  CreativeGenerationRequest,
  CreativeGenerationStatus,
  CreativeOutput,
  DEFAULT_CREATIVE_GENERATION_FILTERS,
  DEFAULT_CREATIVE_GENERATION_PAGINATION,
  DEFAULT_GENERATION_DRAFT,
  GenerationJob,
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

  private pollingTimer: ReturnType<typeof setInterval> | null = null;
  private pollingAttempts = 0;
  private pollingRequestId: string | null = null;

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

  async loadGeneratorContext(assetSearch = ''): Promise<void> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return;
    }

    await this.runLoader(async () => {
      await Promise.all([
        this.fetchPromptHistory(workspaceId),
        this.fetchAvailableAssets(workspaceId, assetSearch),
        this.fetchBrandProfile(workspaceId),
        this.fetchGenerationRequests(workspaceId),
      ]);
    });
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
      await this.fetchAvailableAssets(workspaceId, search);
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
  }

  async openPreviewUrl(output: CreativeOutput): Promise<string | null> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return null;
    }

    try {
      const response = await firstValueFrom(
        this.generationService.getPreviewUrl(workspaceId, output.id, this.requestContext()),
      );
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

    try {
      const response = await firstValueFrom(
        this.generationService.getDownloadUrl(workspaceId, output.id, this.requestContext()),
      );
      return response.url;
    } catch (error) {
      this.handleFailure(error);
      return null;
    }
  }

  startPolling(requestId: string): void {
    this.stopPolling();
    this.pollingRequestId = requestId;
    this.pollingAttempts = 0;
    this.pollingTimer = setInterval(() => {
      void this.pollOnce();
    }, POLL_INTERVAL_MS);
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
      const latest = await firstValueFrom(
        this.generationService.getGeneration(workspaceId, requestId, this.requestContext()),
      );
      this.generationRequestsSignal.update((items) => upsertRequest(items, latest));
      this.setSelectedRequest(latest);

      if (latest.status === 'COMPLETED') {
        await this.fetchOutputs(workspaceId, latest.id);
      }

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
    this.creativeOutputsSignal.set(outputs);
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

  private async fetchAvailableAssets(workspaceId: string, search: string): Promise<void> {
    try {
      const result = await firstValueFrom(
        this.assetService.listAssets(
          workspaceId,
          {
            ...DEFAULT_ASSET_FILTERS,
            search: search.trim(),
          },
          0,
          12,
          this.requestContext(),
        ),
      );
      this.availableAssetsSignal.set(result.items.filter((asset) => asset.status === 'READY'));
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
    this.generationErrorSignal.set(this.mapError(error));
  }

  private mapError(error: unknown): string {
    const normalized = normalizeHttpError(error);

    if (normalized.status === 403) {
      return 'You do not have access to creative generation in this workspace.';
    }

    if (normalized.status === 404) {
      return 'Creative generation data could not be found.';
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
