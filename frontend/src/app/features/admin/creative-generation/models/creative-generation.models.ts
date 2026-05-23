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
export type CreativeGenerationStatus =
  | 'DRAFT'
  | 'QUEUED'
  | 'PROCESSING'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED';
export type GenerationJobType = 'IMAGE_GENERATION' | 'VIDEO_GENERATION';

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
    case 'COMPLETED':
      return 'brand';
    case 'QUEUED':
    case 'PROCESSING':
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
  return value === 'COMPLETED' || value === 'FAILED' || value === 'CANCELLED';
}

export function generationProgressPercent(value: CreativeGenerationStatus | null): number {
  switch (value) {
    case 'DRAFT':
      return 5;
    case 'QUEUED':
      return 25;
    case 'PROCESSING':
      return 68;
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
