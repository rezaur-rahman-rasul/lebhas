import {
  CampaignObjective,
  PromptLanguage,
  PromptPlatform,
} from '@app/features/admin/prompts/models/prompt.models';

export type CreativeType =
  | 'STATIC_IMAGE'
  | 'CAROUSEL_IMAGE'
  | 'SHORT_VIDEO'
  | 'PRODUCT_PROMO_VIDEO'
  | 'STORY_CREATIVE'
  | 'MOTION_GRAPHIC';
export type CreativeOutputFormat = 'PNG' | 'JPG' | 'WEBP' | 'MP4' | 'MOV';
export type ImageCreativeFormat =
  | 'FACEBOOK_SQUARE'
  | 'FACEBOOK_BANNER'
  | 'INSTAGRAM_POST'
  | 'INSTAGRAM_STORY'
  | 'TIKTOK_VERTICAL'
  | 'TIKTOK_PRODUCT_AD'
  | 'LINKEDIN_POST'
  | 'LINKEDIN_BANNER'
  | 'CUSTOM';
export type ImageCreativeQualityMode = 'BASIC' | 'PREMIUM';
export type CreativeGenerationStatus =
  | 'DRAFT'
  | 'QUEUED'
  | 'PROCESSING'
  | 'GENERATING'
  | 'DOWNLOADING'
  | 'UPLOADING'
  | 'READY'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';
export type GenerationJobType = 'IMAGE_GENERATION' | 'VIDEO_GENERATION';
export type CreativePipelineStatus =
  | 'PLANNED'
  | 'QUEUED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'SKIPPED'
  | 'FALLBACK_USED';
export type CreativePipelineLayerType =
  | 'IMAGE_ANALYSIS'
  | 'BACKGROUND_REMOVAL'
  | 'IMAGE_CLEANUP'
  | 'PROMPT_GENERATION'
  | 'IMAGE_GENERATION'
  | 'TEXT_OVERLAY'
  | 'VISION_QUALITY_CHECK'
  | 'IMAGE_RESIZE'
  | 'IMAGE_EXPORT'
  | 'INTERNAL_SAVE'
  | string;

export type AiCreativePlatform = 'FACEBOOK' | 'INSTAGRAM' | 'TIKTOK' | 'LINKEDIN' | 'OTHER';
export type AiCreativeType = 'SQUARE_POST' | 'STORY' | 'BANNER' | 'PRODUCT_AD';
export type AiCreativeTone = 'PROMOTIONAL' | 'PREMIUM' | 'FRIENDLY' | 'URGENT';
export type AiModelQuality = 'BASIC' | 'PREMIUM';
export type AiCreativeSize = '1024x1024' | '1024x1536' | '1536x1024';
export type AiCreativeQuality = 'low' | 'medium' | 'high';
export type AiOutputFormat = 'png' | 'jpeg' | 'webp';
export type AiCreativeBackground = 'opaque' | 'transparent';
export type AiCreativeStatus =
  | 'REQUESTED'
  | 'PLANNING'
  | 'STARTED'
  | 'PROCESSING'
  | 'GENERATING'
  | 'DOWNLOADING'
  | 'UPLOADING'
  | 'READY'
  | 'COMPLETED'
  | 'FAILED';
export type AiLayerStatus = 'PLANNED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type AiGenerationMode =
  | 'TEXT_TO_CREATIVE'
  | 'PRODUCT_IMAGE_TO_CREATIVE'
  | 'MULTI_REFERENCE'
  | 'BACKGROUND_REPLACE'
  | 'TRANSPARENT_ASSET'
  | 'TEXT_ONLY_CREATIVE'
  | 'PRODUCT_IMAGE_CREATIVE'
  | 'MULTI_REFERENCE_CREATIVE'
  | 'BACKGROUND_REPLACEMENT';

export interface AiCreativeGenerateRequest {
  readonly workspaceId: string;
  readonly brandId: string;
  readonly productServiceId?: string;
  readonly campaignId?: string;
  readonly promptTitlePreview?: string;
  readonly platform: AiCreativePlatform;
  readonly language: string;
  readonly creativeType: AiCreativeType;
  readonly tone: AiCreativeTone;
  readonly modelQuality: AiModelQuality;
  readonly generationModeHint?: AiGenerationMode;
  readonly campaignIdea?: string;
  readonly headline?: string;
  readonly subheadline?: string;
  readonly offerText?: string;
  readonly targetAudience?: string;
  readonly productDescription?: string;
  readonly campaignObjective?: string;
  readonly cta?: string | null;
  readonly includeCta?: boolean;
  readonly includeLogo?: boolean;
  readonly includeTypography?: boolean;
  readonly versions: number;
  readonly size: AiCreativeSize;
  readonly quality: AiCreativeQuality;
  readonly outputFormat: AiOutputFormat;
  readonly background: AiCreativeBackground;
  readonly noHumanModel: boolean;
  readonly productImage?: File;
  readonly existingAssetId?: string;
  readonly logoAssetId?: string;
  readonly logoImage?: File;
  readonly referenceImage?: File;
  readonly maskImage?: File;
  readonly backgroundPrompt?: string;
}

export interface AiCreativeResponse {
  readonly creativeId: string;
  readonly workspaceId: string;
  readonly brandId: string;
  readonly productServiceId?: string | null;
  readonly campaignId?: string | null;
  readonly status: AiCreativeStatus;
  readonly generationMode: AiGenerationMode;
  readonly provider: string;
  readonly model: string;
  readonly platform: AiCreativePlatform;
  readonly creativeType: AiCreativeType;
  readonly language: string;
  readonly tone: AiCreativeTone;
  readonly modelQuality: AiModelQuality;
  readonly size: AiCreativeSize;
  readonly quality: AiCreativeQuality;
  readonly outputFormat: AiOutputFormat;
  readonly background: AiCreativeBackground;
  readonly fileUrl?: string | null;
  readonly generatedAssetId?: string | null;
  readonly assetId?: string | null;
  readonly signedPreviewUrl?: string | null;
  readonly publicPreviewUrl?: string | null;
  readonly downloadUrl?: string | null;
  readonly signedDownloadUrl?: string | null;
  readonly publicDownloadUrl?: string | null;
  readonly thumbnailUrl?: string | null;
  readonly r2ObjectKey?: string | null;
  readonly fileSize?: number | null;
  readonly width?: number | null;
  readonly height?: number | null;
  readonly asset?: Readonly<Record<string, unknown>> | null;
  readonly generatedAsset?: Readonly<Record<string, unknown>> | null;
  readonly urls?: Readonly<Record<string, unknown>> | null;
  readonly requestedVersions: number;
  readonly generatedVersionNo?: number | null;
  readonly costEstimate?: number | null;
  readonly creditUsed?: number | null;
  readonly errorMessage?: string | null;
  readonly createdAt: string;
  readonly completedAt?: string | null;
}

export interface CreativeProgressResponse {
  readonly creativeId: string;
  readonly status: AiCreativeStatus;
  readonly currentLayerKey?: string | null;
  readonly currentLayerLabel?: string | null;
  readonly layers: readonly CreativeProgressLayer[];
}

export interface CreativeProgressLayer {
  readonly layerKey: string;
  readonly label: string;
  readonly sequenceNo: number;
  readonly status: AiLayerStatus;
  readonly startedAt?: string | null;
  readonly completedAt?: string | null;
  readonly errorMessage?: string | null;
}

export interface CreativeCreditPreviewRequest {
  readonly workspaceId: string;
  readonly brandId: string;
  readonly productServiceId?: string;
  readonly campaignId?: string;
  readonly platform?: AiCreativePlatform;
  readonly creativeType: AiCreativeType;
  readonly modelQuality: AiModelQuality;
  readonly versions: number;
  readonly hasProductImage: boolean;
  readonly hasLogoImage?: boolean;
  readonly hasReferenceImage?: boolean;
  readonly hasMaskImage?: boolean;
  readonly outputFormat?: AiOutputFormat;
  readonly background?: AiCreativeBackground;
  readonly size?: AiCreativeSize;
}

export interface CreativeCreditPreviewResponse {
  readonly requestedVersions: number;
  readonly availableCredits: number;
  readonly creditStatus: 'READY' | 'MAY_BE_INSUFFICIENT' | 'UNAVAILABLE';
  readonly hasEnoughCredits: boolean;
  readonly blockGeneration: boolean;
  readonly message: string;
  readonly remainingCreditsAfterGeneration: number | null;
  readonly actualCreditsUsed: number | null;
}

export interface CreativeGenerationRequest {
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
  readonly brandContextSnapshot: Readonly<Record<string, unknown>>;
  readonly assetContextSnapshot: readonly Readonly<Record<string, unknown>>[];
  readonly generationConfig: Readonly<Record<string, unknown>>;
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
}

export interface GenerationJob {
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

export interface CreativeOutput {
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
  readonly metadata: Readonly<Record<string, unknown>>;
  readonly status: CreativeGenerationStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreateCreativeGenerationRequest {
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

export interface CreateCampaignCreativeRequest {
  readonly promptDraftId: string | null;
  readonly sourcePrompt: string;
  readonly productAssetId: string | null;
  readonly logoAssetId?: string | null;
  readonly creativeFormat: ImageCreativeFormat;
  readonly platform: PromptPlatform;
  readonly language: Exclude<PromptLanguage, 'MIXED'>;
  readonly qualityMode: ImageCreativeQualityMode;
  readonly requestedVersionCount: number;
  readonly stylePreset: string | null;
  readonly backgroundStyle: string | null;
  readonly headline?: string | null;
  readonly subheadline?: string | null;
  readonly offerText?: string | null;
  readonly cta: string | null;
  readonly includeCta?: boolean;
  readonly includeTypography?: boolean;
}

export interface ProductImageCreativeReadiness {
  readonly ready: boolean;
  readonly workspaceReady: boolean;
  readonly packageReady: boolean;
  readonly creditsReady: boolean;
  readonly providerReady: boolean;
  readonly routingReady: boolean;
  readonly productAssetReady: boolean;
  readonly messages: readonly string[];
  readonly readinessMessages: readonly ProductImageCreativeReadinessMessage[];
}

export interface ImageCreativeCostPreview {
  readonly toolCode: string;
  readonly qualityMode: ImageCreativeQualityMode;
  readonly requestedVersionCount: number;
  readonly unitCreditCost: number | null;
  readonly totalCreditCost: number | null;
}

export interface ProductImageCreativeReadinessMessage {
  readonly code: string;
  readonly message: string;
}

export interface CreativePipelineLayerRun {
  readonly id: string;
  readonly sequence: number;
  readonly layerType: CreativePipelineLayerType;
  readonly providerCode: string;
  readonly modelCode: string | null;
  readonly status: CreativePipelineStatus;
  readonly inputJson: Readonly<Record<string, unknown>>;
  readonly outputJson: Readonly<Record<string, unknown>>;
  readonly inputAssetIds: readonly string[];
  readonly outputAssetIds: readonly string[];
  readonly estimatedCost: number;
  readonly actualCost: number | null;
  readonly startedAt: string | null;
  readonly completedAt: string | null;
  readonly failureReason: string | null;
}

export interface CreativePipelineRun {
  readonly creativeRequestId: string;
  readonly pipelineRunId: string;
  readonly status: CreativePipelineStatus;
  readonly strategy: string;
  readonly primaryProviderCode: string;
  readonly planJson: Readonly<Record<string, unknown>>;
  readonly estimatedCreditCost: number;
  readonly actualCreditCost: number | null;
  readonly failureReason: string | null;
  readonly createdAt: string | null;
  readonly updatedAt: string | null;
  readonly completedAt: string | null;
  readonly layers: readonly CreativePipelineLayerRun[];
}

export interface CreativeGenerationFilter {
  readonly userId: string | null;
  readonly status: CreativeGenerationStatus | null;
  readonly creativeType: CreativeType | null;
  readonly platform: PromptPlatform | null;
}

export interface CreativeGenerationPagination {
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly first: boolean;
  readonly last: boolean;
}

export interface CreativeOutputUrl {
  readonly url: string;
  readonly expiresAt: string | null;
}

export interface CreativeGenerationDraft {
  readonly promptHistoryId: string | null;
  readonly sourcePrompt: string;
  readonly enhancedPrompt: string;
  readonly creativeType: CreativeType | null;
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly outputFormat: CreativeOutputFormat | null;
  readonly language: PromptLanguage | null;
  readonly width: number | null;
  readonly height: number | null;
  readonly duration: number | null;
  readonly generationConfig: Readonly<Record<string, unknown>>;
  readonly useBrandContext: boolean;
}

export const CREATIVE_GENERATION_PAGE_SIZE = 20;

export const CREATIVE_TYPE_OPTIONS: readonly {
  readonly value: CreativeType;
  readonly label: string;
  readonly description: string;
  readonly icon: string;
}[] = [
  {
    value: 'STATIC_IMAGE',
    label: 'Static image',
    description: 'Single ad image for feed, story, or sponsored placement.',
    icon: 'image',
  },
  {
    value: 'CAROUSEL_IMAGE',
    label: 'Carousel image',
    description: 'Multi-frame image concept foundation.',
    icon: 'panel-top',
  },
  {
    value: 'SHORT_VIDEO',
    label: 'Short video',
    description: 'Video generation foundation for motion ads.',
    icon: 'video',
  },
  {
    value: 'PRODUCT_PROMO_VIDEO',
    label: 'Product promo video',
    description: 'Product-focused video request foundation.',
    icon: 'badge-play',
  },
  {
    value: 'STORY_CREATIVE',
    label: 'Story creative',
    description: 'Vertical story-oriented static creative.',
    icon: 'smartphone',
  },
  {
    value: 'MOTION_GRAPHIC',
    label: 'Motion graphic',
    description: 'Motion design request foundation.',
    icon: 'sparkles',
  },
];

export const CREATIVE_OUTPUT_FORMAT_OPTIONS: readonly {
  readonly value: CreativeOutputFormat;
  readonly label: string;
  readonly mediaType: 'image' | 'video';
}[] = [
  { value: 'PNG', label: 'PNG', mediaType: 'image' },
  { value: 'JPG', label: 'JPG', mediaType: 'image' },
  { value: 'WEBP', label: 'WEBP', mediaType: 'image' },
  { value: 'MP4', label: 'MP4', mediaType: 'video' },
  { value: 'MOV', label: 'MOV', mediaType: 'video' },
];

export const IMAGE_SIZE_OPTIONS: readonly {
  readonly label: string;
  readonly width: number;
  readonly height: number;
}[] = [
  { label: 'Square 1:1', width: 1080, height: 1080 },
  { label: 'Story 9:16', width: 1080, height: 1920 },
  { label: 'Feed 4:5', width: 1080, height: 1350 },
  { label: 'Landscape 1.91:1', width: 1200, height: 628 },
  { label: 'LinkedIn 1.91:1', width: 1200, height: 627 },
];

export const CREATIVE_GENERATION_STATUS_OPTIONS: readonly {
  readonly value: CreativeGenerationStatus;
  readonly label: string;
}[] = [
  { value: 'DRAFT', label: 'Draft' },
  { value: 'QUEUED', label: 'Queued' },
  { value: 'PROCESSING', label: 'Processing' },
  { value: 'GENERATING', label: 'Generating' },
  { value: 'DOWNLOADING', label: 'Downloading asset' },
  { value: 'UPLOADING', label: 'Uploading preview' },
  { value: 'READY', label: 'Ready' },
  { value: 'COMPLETED', label: 'Completed' },
  { value: 'FAILED', label: 'Failed' },
  { value: 'CANCELLED', label: 'Cancelled' },
];

export const DEFAULT_CREATIVE_GENERATION_FILTERS: CreativeGenerationFilter = {
  userId: null,
  status: null,
  creativeType: null,
  platform: null,
};

export const DEFAULT_CREATIVE_GENERATION_PAGINATION: CreativeGenerationPagination = {
  page: 0,
  size: CREATIVE_GENERATION_PAGE_SIZE,
  totalItems: 0,
  totalPages: 0,
  first: true,
  last: true,
};

export const DEFAULT_GENERATION_DRAFT: CreativeGenerationDraft = {
  promptHistoryId: null,
  sourcePrompt: '',
  enhancedPrompt: '',
  creativeType: 'STATIC_IMAGE',
  platform: 'INSTAGRAM',
  campaignObjective: 'SALES',
  outputFormat: 'PNG',
  language: 'ENGLISH',
  width: 1080,
  height: 1080,
  duration: null,
  generationConfig: {},
  useBrandContext: true,
};

export function creativeTypeLabel(value: CreativeType | null): string {
  return CREATIVE_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? 'Not selected';
}

export function creativeOutputFormatLabel(value: CreativeOutputFormat | null): string {
  return CREATIVE_OUTPUT_FORMAT_OPTIONS.find((option) => option.value === value)?.label ?? 'Not selected';
}

export function creativeGenerationStatusLabel(value: CreativeGenerationStatus): string {
  return CREATIVE_GENERATION_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export function creativeGenerationStatusTone(
  value: CreativeGenerationStatus,
): 'brand' | 'blue' | 'red' | 'neutral' {
  switch (value) {
    case 'READY':
    case 'COMPLETED':
      return 'brand';
    case 'QUEUED':
    case 'PROCESSING':
    case 'GENERATING':
    case 'DOWNLOADING':
    case 'UPLOADING':
      return 'blue';
    case 'FAILED':
      return 'red';
    default:
      return 'neutral';
  }
}

export function isVideoCreative(value: CreativeType | null): boolean {
  return value === 'SHORT_VIDEO' || value === 'PRODUCT_PROMO_VIDEO' || value === 'MOTION_GRAPHIC';
}

export function isImageCreative(value: CreativeType | null): boolean {
  return value === 'STATIC_IMAGE' || value === 'CAROUSEL_IMAGE' || value === 'STORY_CREATIVE';
}

export function isVideoFormat(value: CreativeOutputFormat | null): boolean {
  return value === 'MP4' || value === 'MOV';
}

export function isImageFormat(value: CreativeOutputFormat | null): boolean {
  return value === 'PNG' || value === 'JPG' || value === 'WEBP';
}

export function isTerminalGenerationStatus(value: CreativeGenerationStatus): boolean {
  return value === 'READY' || value === 'COMPLETED' || value === 'FAILED' || value === 'CANCELLED';
}

export function generationProgressPercent(value: CreativeGenerationStatus | null): number {
  switch (value) {
    case 'DRAFT':
      return 5;
    case 'QUEUED':
      return 25;
    case 'PROCESSING':
      return 35;
    case 'GENERATING':
      return 55;
    case 'DOWNLOADING':
      return 70;
    case 'UPLOADING':
      return 85;
    case 'READY':
    case 'COMPLETED':
      return 100;
    case 'FAILED':
    case 'CANCELLED':
      return 100;
    default:
      return 0;
  }
}

export function formatOutputDimensions(output: Pick<CreativeOutput, 'width' | 'height'>): string {
  return output.width && output.height ? `${output.width} x ${output.height}` : 'Dimensions pending';
}

export function formatOutputDuration(duration: number | null): string {
  if (!duration) {
    return 'No duration';
  }

  return `${duration}s`;
}
