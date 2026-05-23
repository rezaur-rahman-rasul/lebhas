import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import {
  CreativeGenerationFilter,
  CreativeGenerationPagination,
  CreativeGenerationRequest,
  CreativeGenerationStatus,
  CreativeOutput,
  CreativeOutputFormat,
  CreativeOutputUrl,
  CreativeType,
  DEFAULT_CREATIVE_GENERATION_PAGINATION,
  GenerationJob,
  GenerationJobType,
  CreateCreativeGenerationRequest,
} from '../models/creative-generation.models';
import {
  CampaignObjective,
  PromptLanguage,
  PromptPlatform,
} from '@app/features/admin/prompts/models/prompt.models';

interface PagedResultDto<T> {
  readonly items: readonly T[];
  readonly totalItems: number;
  readonly totalPages: number;
  readonly page: number;
  readonly size: number;
  readonly first: boolean;
  readonly last: boolean;
}

interface CreativeGenerationRequestDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly userId: string;
  readonly promptHistoryId: string | null;
  readonly sourcePrompt: string | null;
  readonly enhancedPrompt: string | null;
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly creativeType: CreativeType;
  readonly outputFormat: CreativeOutputFormat;
  readonly language: PromptLanguage | null;
  readonly brandContextSnapshot: Readonly<Record<string, unknown>> | null;
  readonly assetContextSnapshot: readonly Readonly<Record<string, unknown>>[] | null;
  readonly generationConfig: Readonly<Record<string, unknown>> | null;
  readonly status: CreativeGenerationStatus;
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
  readonly requestedAt: string | null;
  readonly startedAt: string | null;
  readonly completedAt: string | null;
  readonly failedAt: string | null;
  readonly errorMessage: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly jobs?: readonly GenerationJobDto[] | null;
}

interface GenerationJobDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly requestId: string;
  readonly jobType: GenerationJobType;
  readonly status: CreativeGenerationStatus;
  readonly providerJobId: string | null;
  readonly attemptCount: number;
  readonly maxAttempts: number;
  readonly queueName: string | null;
  readonly startedAt: string | null;
  readonly completedAt: string | null;
  readonly failedAt: string | null;
  readonly errorMessage: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface CreativeOutputDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly requestId: string;
  readonly generatedAssetId: string | null;
  readonly creativeType: CreativeType;
  readonly platform: PromptPlatform | null;
  readonly outputFormat: CreativeOutputFormat;
  readonly width: number | null;
  readonly height: number | null;
  readonly duration: number | null;
  readonly fileSize: number | null;
  readonly previewUrl: string | null;
  readonly downloadUrl: string | null;
  readonly caption: string | null;
  readonly headline: string | null;
  readonly ctaText: string | null;
  readonly metadata: Readonly<Record<string, unknown>> | null;
  readonly status: CreativeGenerationStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface CreativeOutputUrlDto {
  readonly url: string;
  readonly expiresAt: string | null;
}

interface CreateCreativeGenerationRequestDto {
  readonly promptHistoryId: string | null;
  readonly sourcePrompt: string;
  readonly enhancedPrompt: string | null;
  readonly assetIds: readonly string[];
  readonly creativeType: CreativeType;
  readonly platform: PromptPlatform;
  readonly campaignObjective: CampaignObjective;
  readonly outputFormat: CreativeOutputFormat;
  readonly language: PromptLanguage;
  readonly width: number | null;
  readonly height: number | null;
  readonly duration: number | null;
  readonly generationConfig: Readonly<Record<string, unknown>>;
  readonly useBrandContext: boolean;
}

@Injectable({ providedIn: 'root' })
export class CreativeGenerationService {
  private readonly api = inject(ApiService);

  submitGeneration(
    workspaceId: string,
    payload: CreateCreativeGenerationRequest,
    context?: HttpContext,
  ) {
    return this.api
      .post<CreativeGenerationRequestDto, CreateCreativeGenerationRequestDto>(
        `/api/v1/workspaces/${workspaceId}/creative-generations`,
        mapCreateRequest(payload),
        { context },
      )
      .pipe(map(({ data }) => mapGenerationRequest(data)));
  }

  listGenerations(
    workspaceId: string,
    filters: CreativeGenerationFilter,
    page: number,
    size: number,
    context?: HttpContext,
  ) {
    return this.api
      .get<PagedResultDto<CreativeGenerationRequestDto>>(
        `/api/v1/workspaces/${workspaceId}/creative-generations`,
        {
          params: {
            userId: filters.userId,
            status: filters.status,
            creativeType: filters.creativeType,
            platform: filters.platform,
            page,
            size,
          },
          context,
        },
      )
      .pipe(
        map(({ data }) => ({
          items: data.items.map(mapGenerationRequest),
          pagination: mapPagination(data),
        })),
      );
  }

  getGeneration(workspaceId: string, requestId: string, context?: HttpContext) {
    return this.api
      .get<CreativeGenerationRequestDto>(
        `/api/v1/workspaces/${workspaceId}/creative-generations/${requestId}`,
        { context },
      )
      .pipe(map(({ data }) => mapGenerationRequest(data)));
  }

  listOutputs(workspaceId: string, requestId: string, context?: HttpContext) {
    return this.api
      .get<readonly CreativeOutputDto[]>(
        `/api/v1/workspaces/${workspaceId}/creative-generations/${requestId}/outputs`,
        { context },
      )
      .pipe(map(({ data }) => data.map(mapCreativeOutput)));
  }

  retryGeneration(workspaceId: string, requestId: string, context?: HttpContext) {
    return this.api
      .post<CreativeGenerationRequestDto, Record<string, never>>(
        `/api/v1/workspaces/${workspaceId}/creative-generations/${requestId}/retry`,
        {},
        { context },
      )
      .pipe(map(({ data }) => mapGenerationRequest(data)));
  }

  cancelGeneration(workspaceId: string, requestId: string, context?: HttpContext) {
    return this.api
      .post<CreativeGenerationRequestDto, Record<string, never>>(
        `/api/v1/workspaces/${workspaceId}/creative-generations/${requestId}/cancel`,
        {},
        { context },
      )
      .pipe(map(({ data }) => mapGenerationRequest(data)));
  }

  getOutput(workspaceId: string, outputId: string, context?: HttpContext) {
    return this.api
      .get<CreativeOutputDto>(`/api/v1/workspaces/${workspaceId}/creative-outputs/${outputId}`, {
        context,
      })
      .pipe(map(({ data }) => mapCreativeOutput(data)));
  }

  getPreviewUrl(workspaceId: string, outputId: string, context?: HttpContext) {
    return this.api
      .get<CreativeOutputUrlDto>(
        `/api/v1/workspaces/${workspaceId}/creative-outputs/${outputId}/preview-url`,
        { context },
      )
      .pipe(map(({ data }) => mapOutputUrl(data)));
  }

  getDownloadUrl(workspaceId: string, outputId: string, context?: HttpContext) {
    return this.api
      .get<CreativeOutputUrlDto>(
        `/api/v1/workspaces/${workspaceId}/creative-outputs/${outputId}/download-url`,
        { context },
      )
      .pipe(map(({ data }) => mapOutputUrl(data)));
  }
}

export function mapGenerationRequest(source: CreativeGenerationRequestDto): CreativeGenerationRequest {
  return {
    id: source.id,
    workspaceId: source.workspaceId,
    userId: source.userId,
    promptHistoryId: source.promptHistoryId,
    sourcePrompt: source.sourcePrompt,
    enhancedPrompt: source.enhancedPrompt,
    platform: source.platform,
    campaignObjective: source.campaignObjective,
    creativeType: source.creativeType,
    outputFormat: source.outputFormat,
    language: source.language,
    brandContextSnapshot: source.brandContextSnapshot ?? {},
    assetContextSnapshot: source.assetContextSnapshot ?? [],
    generationConfig: source.generationConfig ?? {},
    status: source.status,
    aiProvider: source.aiProvider,
    aiModel: source.aiModel,
    requestedAt: source.requestedAt,
    startedAt: source.startedAt,
    completedAt: source.completedAt,
    failedAt: source.failedAt,
    errorMessage: source.errorMessage,
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
  };
}

export function mapGenerationJobsFromRequest(source: CreativeGenerationRequest): readonly GenerationJob[] {
  const configuredAttemptCount = source.generationConfig['attemptCount'];
  const attemptCount =
    typeof configuredAttemptCount === 'number' && Number.isFinite(configuredAttemptCount)
      ? configuredAttemptCount
      : source.status === 'DRAFT'
        ? 0
        : 1;

  return [
    {
      id: `${source.id}:generation-job`,
      workspaceId: source.workspaceId,
      requestId: source.id,
      jobType: source.creativeType === 'STATIC_IMAGE' || source.creativeType === 'CAROUSEL_IMAGE' || source.creativeType === 'STORY_CREATIVE'
        ? 'IMAGE_GENERATION'
        : 'VIDEO_GENERATION',
      status: source.status,
      providerJobId: null,
      attemptCount,
      maxAttempts: 3,
      queueName: source.creativeType === 'STATIC_IMAGE' ? 'image-generation' : 'creative-generation',
      startedAt: source.startedAt,
      completedAt: source.completedAt,
      failedAt: source.failedAt,
      errorMessage: source.errorMessage,
      createdAt: source.createdAt,
      updatedAt: source.updatedAt,
    },
  ];
}

function mapCreativeOutput(source: CreativeOutputDto): CreativeOutput {
  return {
    id: source.id,
    workspaceId: source.workspaceId,
    requestId: source.requestId,
    generatedAssetId: source.generatedAssetId,
    creativeType: source.creativeType,
    platform: source.platform,
    outputFormat: source.outputFormat,
    width: source.width,
    height: source.height,
    duration: source.duration,
    fileSize: source.fileSize,
    previewUrl: source.previewUrl,
    downloadUrl: source.downloadUrl,
    caption: source.caption,
    headline: source.headline,
    ctaText: source.ctaText,
    metadata: source.metadata ?? {},
    status: source.status,
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
  };
}

function mapPagination(source: PagedResultDto<CreativeGenerationRequestDto>): CreativeGenerationPagination {
  return {
    page: source.page ?? DEFAULT_CREATIVE_GENERATION_PAGINATION.page,
    size: source.size ?? DEFAULT_CREATIVE_GENERATION_PAGINATION.size,
    totalItems: source.totalItems ?? DEFAULT_CREATIVE_GENERATION_PAGINATION.totalItems,
    totalPages: source.totalPages ?? DEFAULT_CREATIVE_GENERATION_PAGINATION.totalPages,
    first: source.first ?? DEFAULT_CREATIVE_GENERATION_PAGINATION.first,
    last: source.last ?? DEFAULT_CREATIVE_GENERATION_PAGINATION.last,
  };
}

function mapCreateRequest(
  payload: CreateCreativeGenerationRequest,
): CreateCreativeGenerationRequestDto {
  return {
    promptHistoryId: payload.promptHistoryId,
    sourcePrompt: payload.sourcePrompt,
    enhancedPrompt: payload.enhancedPrompt,
    assetIds: payload.assetIds,
    creativeType: payload.creativeType,
    platform: payload.platform,
    campaignObjective: payload.campaignObjective,
    outputFormat: payload.outputFormat,
    language: payload.language,
    width: payload.width,
    height: payload.height,
    duration: payload.duration,
    generationConfig: payload.generationConfig,
    useBrandContext: payload.useBrandContext,
  };
}

function mapOutputUrl(source: CreativeOutputUrlDto): CreativeOutputUrl {
  return {
    url: source.url,
    expiresAt: source.expiresAt,
  };
}
