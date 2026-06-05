import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import {
  CreativeGenerationFilter,
  CreativeGenerationPagination,
  CreativeGenerationRequest,
  CreativeGenerationStatus,
  CreativePipelineRun,
  CreativePipelineStatus,
  CreativePipelineLayerType,
  CreativeOutput,
  CreativeOutputFormat,
  CreativeOutputUrl,
  CreativeType,
  CreateCampaignCreativeRequest,
  ProductImageCreativeReadiness,
  DEFAULT_CREATIVE_GENERATION_PAGINATION,
  GenerationJob,
  GenerationJobType,
  CreateCreativeGenerationRequest,
  ImageCreativeCostPreview,
  ImageCreativeFormat,
  ImageCreativeQualityMode,
  AiCreativeGenerateRequest,
  AiCreativeResponse,
  CreativeCreditPreviewRequest,
  CreativeCreditPreviewResponse,
  CreativeProgressResponse,
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

interface CreateCampaignCreativeRequestDto {
  readonly promptDraftId: string | null;
  readonly sourcePrompt: string;
  readonly productAssetId: string;
  readonly creativeFormat: ImageCreativeFormat;
  readonly platform: PromptPlatform;
  readonly language: Exclude<PromptLanguage, 'MIXED'>;
  readonly qualityMode: ImageCreativeQualityMode;
  readonly requestedVersionCount: number;
  readonly stylePreset: string | null;
  readonly backgroundStyle: string | null;
  readonly cta: string | null;
}

interface ImageCreativeGenerationDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly projectId: string;
  readonly creativeRequestId: string;
  readonly brandId: string | null;
  readonly productServiceId: string | null;
  readonly productAssetId: string;
  readonly toolCode: string | null;
  readonly creativeFormat: ImageCreativeFormat;
  readonly platform: PromptPlatform;
  readonly language: Exclude<PromptLanguage, 'MIXED'>;
  readonly qualityMode: ImageCreativeQualityMode;
  readonly requestedVersionCount: number;
  readonly creditCost: number | string | null;
  readonly creditReservationId: string | null;
  readonly status: string;
  readonly failureReason: string | null;
  readonly generatedVersionIds: readonly string[] | null;
  readonly request: Readonly<Record<string, unknown>> | null;
  readonly createdAt: string;
}

interface ImageCreativeCostPreviewDto {
  readonly toolCode: string;
  readonly qualityMode: ImageCreativeQualityMode;
  readonly requestedVersionCount: number;
  readonly unitCreditCost: number | string | null;
  readonly totalCreditCost: number | string | null;
}

interface GeneratedVersionDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly creativeRequestId: string;
  readonly projectCampaignId: string;
  readonly versionNumber: number;
  readonly versionName: string;
  readonly storageFileId: string | null;
  readonly assetId: string | null;
  readonly previewUrl: string | null;
  readonly thumbnailUrl: string | null;
  readonly generationStatus: string;
  readonly approvalStatus: string;
  readonly status: string;
  readonly createdAt: string;
  readonly updatedAt: string;
}

interface CampaignCreativeResultDto {
  readonly generation: ImageCreativeGenerationDto;
  readonly generatedVersions: readonly GeneratedVersionDto[];
  readonly pipeline: CreativePipelineRunDto | null;
}

interface CreativePipelineLayerRunDto {
  readonly id: string;
  readonly sequence: number;
  readonly layerType: CreativePipelineLayerType;
  readonly providerCode: string;
  readonly modelCode: string | null;
  readonly status: CreativePipelineStatus;
  readonly inputJson: Readonly<Record<string, unknown>> | null;
  readonly outputJson: Readonly<Record<string, unknown>> | null;
  readonly inputAssetIds: readonly string[] | null;
  readonly outputAssetIds: readonly string[] | null;
  readonly estimatedCost: number | string | null;
  readonly actualCost: number | string | null;
  readonly startedAt: string | null;
  readonly completedAt: string | null;
  readonly failureReason: string | null;
}

interface CreativePipelineRunDto {
  readonly creativeRequestId: string;
  readonly pipelineRunId: string;
  readonly status: CreativePipelineStatus;
  readonly strategy: string;
  readonly primaryProviderCode: string;
  readonly planJson: Readonly<Record<string, unknown>> | null;
  readonly estimatedCreditCost: number | string | null;
  readonly actualCreditCost: number | string | null;
  readonly failureReason: string | null;
  readonly createdAt: string | null;
  readonly updatedAt: string | null;
  readonly completedAt: string | null;
  readonly layers: readonly CreativePipelineLayerRunDto[] | null;
}

interface ProductImageCreativeReadinessDto {
  readonly ready: boolean;
  readonly workspaceReady: boolean;
  readonly packageReady: boolean;
  readonly creditsReady: boolean;
  readonly providerReady: boolean;
  readonly routingReady: boolean;
  readonly productAssetReady: boolean;
  readonly messages: readonly (string | ProductImageCreativeReadinessMessageDto)[] | null;
  readonly readinessMessages?: readonly ProductImageCreativeReadinessMessageDto[] | null;
}

interface ProductImageCreativeReadinessMessageDto {
  readonly code: string | null;
  readonly message: string;
}

type AiCreativeResponseDto = AiCreativeResponse;
type CreativeProgressResponseDto = CreativeProgressResponse;
type CreativeCreditPreviewResponseDto = CreativeCreditPreviewResponse;

@Injectable({ providedIn: 'root' })
export class CreativeGenerationService {
  private readonly api = inject(ApiService);

  generateCreative(
    payload: AiCreativeGenerateRequest,
    context?: HttpContext,
  ) {
    if (!hasAiCreativeFiles(payload)) {
      return this.api
        .post<AiCreativeResponseDto, AiCreativeGenerateRequest>(
          '/api/v1/ai/creatives/generate',
          withoutUndefinedAiCreativeFields(payload),
          { context },
        )
        .pipe(map(({ data }) => data));
    }

    return this.api
      .post<AiCreativeResponseDto, FormData>(
        '/api/v1/ai/creatives/generate',
        mapAiCreativeGenerateFormData(payload),
        { context },
      )
      .pipe(map(({ data }) => data));
  }

  getCreativeProgress(creativeId: string, context?: HttpContext) {
    return this.api
      .get<CreativeProgressResponseDto>(
        `/api/v1/ai/creatives/${creativeId}/progress`,
        { context },
      )
      .pipe(map(({ data }) => data));
  }

  getRecentGenerations(workspaceId: string, context?: HttpContext) {
    return this.api
      .get<readonly AiCreativeResponseDto[]>(
        '/api/v1/ai/creatives/recent',
        {
          params: { workspaceId },
          context,
        },
      )
      .pipe(map(({ data }) => data));
  }

  getGeneratedVariations(creativeId: string, context?: HttpContext) {
    return this.api
      .get<readonly AiCreativeResponseDto[]>(
        `/api/v1/ai/creatives/${creativeId}/variations`,
        { context },
      )
      .pipe(map(({ data }) => data));
  }

  getCreditPreview(payload: CreativeCreditPreviewRequest, context?: HttpContext) {
    return this.api
      .post<CreativeCreditPreviewResponseDto, CreativeCreditPreviewRequest>(
        '/api/v1/ai/creatives/credit-preview',
        payload,
        { context },
      )
      .pipe(map(({ data }) => data));
  }

  sendForApproval(creativeId: string, context?: HttpContext) {
    return this.api
      .post<void, Record<string, never>>(
        `/api/v1/approvals/creatives/${creativeId}`,
        {},
        { context },
      )
      .pipe(map(() => undefined));
  }

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

  submitCampaignCreative(
    workspaceId: string,
    projectId: string,
    payload: CreateCampaignCreativeRequest,
    context?: HttpContext,
  ) {
    return this.api
      .post<CampaignCreativeResultDto, CreateCampaignCreativeRequestDto>(
        `/api/v1/workspaces/${workspaceId}/projects/${projectId}/image-creatives/generate`,
        mapCampaignCreativeRequest(payload),
        { context },
      )
      .pipe(map(({ data }) => ({
        generation: data.generation,
        generatedVersions: data.generatedVersions,
        pipeline: data.pipeline ? mapCreativePipelineRun(data.pipeline) : null,
      })));
  }

  getCampaignCreativeReadiness(
    workspaceId: string,
    projectId: string,
    productAssetId: string | null,
    qualityMode: ImageCreativeQualityMode,
    requestedVersionCount: number,
    context?: HttpContext,
  ) {
    return this.api
      .get<ProductImageCreativeReadinessDto>(
        `/api/v1/workspaces/${workspaceId}/projects/${projectId}/image-creatives/readiness`,
        {
          params: {
            productAssetId,
            qualityMode,
            requestedVersionCount,
          },
          context,
        },
      )
      .pipe(map(({ data }) => mapProductImageCreativeReadiness(data)));
  }

  getCampaignCreativePipeline(workspaceId: string, requestId: string, context?: HttpContext) {
    return this.api
      .get<CreativePipelineRunDto>(
        `/api/v1/workspaces/${workspaceId}/creative-generator/requests/${requestId}/pipeline`,
        { context },
      )
      .pipe(map(({ data }) => mapCreativePipelineRun(data)));
  }

  previewCampaignCreativeCost(
    workspaceId: string,
    projectId: string,
    payload: CreateCampaignCreativeRequest,
    context?: HttpContext,
  ) {
    return this.api
      .post<ImageCreativeCostPreviewDto, CreateCampaignCreativeRequestDto>(
        `/api/v1/workspaces/${workspaceId}/projects/${projectId}/image-creatives/preview-cost`,
        mapCampaignCreativeRequest(payload),
        { context },
      )
      .pipe(map(({ data }) => mapImageCreativeCostPreview(data)));
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

function mapCampaignCreativeRequest(
  payload: CreateCampaignCreativeRequest,
): CreateCampaignCreativeRequestDto {
  return {
    promptDraftId: payload.promptDraftId,
    sourcePrompt: payload.sourcePrompt.trim(),
    productAssetId: payload.productAssetId,
    creativeFormat: payload.creativeFormat,
    platform: payload.platform,
    language: payload.language,
    qualityMode: payload.qualityMode,
    requestedVersionCount: payload.requestedVersionCount,
    stylePreset: payload.stylePreset,
    backgroundStyle: payload.backgroundStyle,
    cta: payload.cta,
  };
}

function mapAiCreativeGenerateFormData(payload: AiCreativeGenerateRequest): FormData {
  const formData = new FormData();
  appendFormData(formData, 'workspaceId', payload.workspaceId);
  appendFormData(formData, 'brandId', payload.brandId);
  appendFormData(formData, 'productServiceId', payload.productServiceId);
  appendFormData(formData, 'campaignId', payload.campaignId);
  appendFormData(formData, 'promptTitlePreview', payload.promptTitlePreview);
  appendFormData(formData, 'platform', payload.platform);
  appendFormData(formData, 'language', payload.language);
  appendFormData(formData, 'creativeType', payload.creativeType);
  appendFormData(formData, 'tone', payload.tone);
  appendFormData(formData, 'modelQuality', payload.modelQuality);
  appendFormData(formData, 'generationModeHint', payload.generationModeHint);
  appendFormData(formData, 'campaignIdea', payload.campaignIdea);
  appendFormData(formData, 'headline', payload.headline ?? payload.campaignIdea);
  appendFormData(formData, 'subheadline', payload.subheadline);
  appendFormData(formData, 'offerText', payload.offerText);
  appendFormData(formData, 'targetAudience', payload.targetAudience);
  appendFormData(formData, 'productDescription', payload.productDescription ?? payload.campaignIdea);
  appendFormData(formData, 'campaignObjective', payload.campaignObjective);
  appendFormData(formData, 'cta', payload.cta);
  appendFormData(formData, 'versions', payload.versions);
  appendFormData(formData, 'size', payload.size);
  appendFormData(formData, 'quality', payload.quality);
  appendFormData(formData, 'outputFormat', payload.outputFormat);
  appendFormData(formData, 'background', payload.background);
  appendFormData(formData, 'noHumanModel', payload.noHumanModel);
  appendFormData(formData, 'existingAssetId', payload.existingAssetId);
  appendFormData(formData, 'backgroundPrompt', payload.backgroundPrompt);

  if (payload.productImage) {
    formData.append('productImage', payload.productImage);
  }
  if (payload.logoImage) {
    formData.append('logoImage', payload.logoImage);
  }
  if (payload.referenceImage) {
    formData.append('referenceImage', payload.referenceImage);
  }
  if (payload.maskImage) {
    formData.append('maskImage', payload.maskImage);
  }

  return formData;
}

function hasAiCreativeFiles(payload: AiCreativeGenerateRequest): boolean {
  return Boolean(payload.productImage || payload.logoImage || payload.referenceImage || payload.maskImage);
}

function withoutUndefinedAiCreativeFields(payload: AiCreativeGenerateRequest): AiCreativeGenerateRequest {
  return Object.fromEntries(
    Object.entries(payload).filter(([, value]) =>
      value !== undefined &&
      value !== null &&
      !(typeof value === 'string' && value.trim().length === 0) &&
      !(value instanceof File)
    ),
  ) as AiCreativeGenerateRequest;
}

function appendFormData(
  formData: FormData,
  key: string,
  value: string | number | boolean | null | undefined,
): void {
  if (value === null || value === undefined) {
    return;
  }
  if (typeof value === 'string' && value.trim().length === 0) {
    return;
  }
  formData.append(key, String(value));
}

function mapProductImageCreativeReadiness(
  source: ProductImageCreativeReadinessDto,
): ProductImageCreativeReadiness {
  const structuredMessages = [
    ...(source.readinessMessages ?? []),
    ...(source.messages ?? []).filter(isReadinessMessageDto),
  ].map((item) => ({
    code: item.code ?? 'READINESS_BLOCKED',
    message: item.message,
  }));
  const stringMessages = (source.messages ?? [])
    .map((item) => typeof item === 'string' ? item : item.message)
    .filter((message) => message.trim().length > 0);

  return {
    ready: source.ready,
    workspaceReady: source.workspaceReady,
    packageReady: source.packageReady,
    creditsReady: source.creditsReady,
    providerReady: source.providerReady,
    routingReady: source.routingReady,
    productAssetReady: source.productAssetReady,
    messages: stringMessages.length > 0 ? stringMessages : structuredMessages.map((item) => item.message),
    readinessMessages: structuredMessages,
  };
}

function mapCreativePipelineRun(source: CreativePipelineRunDto): CreativePipelineRun {
  return {
    creativeRequestId: source.creativeRequestId,
    pipelineRunId: source.pipelineRunId,
    status: source.status,
    strategy: source.strategy,
    primaryProviderCode: source.primaryProviderCode,
    planJson: source.planJson ?? {},
    estimatedCreditCost: Number(source.estimatedCreditCost ?? 0),
    actualCreditCost: source.actualCreditCost === null || source.actualCreditCost === undefined
      ? null
      : Number(source.actualCreditCost),
    failureReason: source.failureReason,
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
    completedAt: source.completedAt,
    layers: (source.layers ?? []).map((layer) => ({
      id: layer.id,
      sequence: Number(layer.sequence ?? 0),
      layerType: layer.layerType,
      providerCode: layer.providerCode,
      modelCode: layer.modelCode,
      status: layer.status,
      inputJson: layer.inputJson ?? {},
      outputJson: layer.outputJson ?? {},
      inputAssetIds: layer.inputAssetIds ?? [],
      outputAssetIds: layer.outputAssetIds ?? [],
      estimatedCost: Number(layer.estimatedCost ?? 0),
      actualCost: layer.actualCost === null || layer.actualCost === undefined ? null : Number(layer.actualCost),
      startedAt: layer.startedAt,
      completedAt: layer.completedAt,
      failureReason: layer.failureReason,
    })),
  };
}

function mapImageCreativeCostPreview(source: ImageCreativeCostPreviewDto): ImageCreativeCostPreview {
  return {
    toolCode: source.toolCode,
    qualityMode: source.qualityMode,
    requestedVersionCount: Number(source.requestedVersionCount ?? 1),
    unitCreditCost: Number(source.unitCreditCost ?? 0),
    totalCreditCost: Number(source.totalCreditCost ?? 0),
  };
}

function isReadinessMessageDto(
  value: string | ProductImageCreativeReadinessMessageDto,
): value is ProductImageCreativeReadinessMessageDto {
  return typeof value === 'object' && value !== null && 'message' in value;
}

function mapOutputUrl(source: CreativeOutputUrlDto): CreativeOutputUrl {
  return {
    url: source.url,
    expiresAt: source.expiresAt,
  };
}
