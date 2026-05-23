import { ApiResponse } from '@app/shared/models/api-response.model';

import {
  CampaignObjective,
  CreativeStyle,
  PromptHistoryStatus,
  PromptLanguage,
  PromptPlatform,
  PromptTemplateStatus,
  PromptTone,
  SuggestionType,
} from './prompt.enums';
import { PromptHistory, PromptHistoryPage, PromptPagination, PromptTemplate } from './prompt.models';

/** Shared builder context sent to project-scoped intelligence endpoints. */
export interface PromptIntelligenceContext {
  readonly customPrompt: string;
  readonly assetIds: readonly string[];
  readonly templateId: string | null;
  readonly businessType: string | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly platform: PromptPlatform | null;
  readonly creativeStyle: CreativeStyle | null;
  readonly language: PromptLanguage | null;
  readonly tone: PromptTone | null;
  readonly targetAudience: string | null;
  readonly offerDetails: string | null;
  readonly ctaPreference: string | null;
  readonly useBrandProfile: boolean;
}

export type EnhancePromptRequest = PromptIntelligenceContext;

export interface PromptSuggestionsRequest extends PromptIntelligenceContext {
  readonly suggestionTypes: readonly SuggestionType[];
}

export interface EnhancePromptResponse {
  readonly enhancedPrompt: string;
  readonly reasoningSummary: string | null;
  readonly suggestedMissingFields: readonly string[];
  readonly aiProvider: string | null;
  readonly model: string | null;
  readonly tokenUsage: number | null;
}

export interface PromptSuggestionsResponse {
  readonly ctaSuggestions: readonly string[];
  readonly headlineSuggestions: readonly string[];
  readonly offerSuggestions: readonly string[];
  readonly creativeAngleSuggestions: readonly string[];
  readonly campaignToneSuggestions: readonly string[];
  readonly businessCategorySuggestions: readonly string[];
  readonly reasoningSummary: string | null;
  readonly aiProvider: string | null;
  readonly model: string | null;
  readonly tokenUsage: number | null;
}

export interface PromptHistoryFilter {
  readonly userId: string | null;
  readonly suggestionType: SuggestionType | null;
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly status: PromptHistoryStatus | null;
  readonly createdFrom: string | null;
  readonly createdTo: string | null;
}

export interface PromptHistoryListQuery extends PromptHistoryFilter {
  readonly page: number;
  readonly size: number;
}

export type PromptHistoryListResponse = PromptHistoryPage;

export interface PromptTemplateFilter {
  readonly category: string | null;
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly language: PromptLanguage | null;
  readonly businessType: string | null;
  readonly status: PromptTemplateStatus | null;
  readonly search: string | null;
  readonly systemDefault: boolean | null;
  readonly includeSystemDefaults: boolean;
}

export interface CreatePromptTemplateRequest {
  readonly name: string;
  readonly category: string | null;
  readonly description: string | null;
  readonly platform: PromptPlatform;
  readonly campaignObjective: CampaignObjective;
  readonly businessType: string | null;
  readonly language: PromptLanguage;
  readonly promptBody: string;
  readonly isDefault: boolean;
  readonly status: PromptTemplateStatus;
}

export type UpdatePromptTemplateRequest = CreatePromptTemplateRequest;

export type PromptTemplateListResponse = readonly PromptTemplate[];

export type EnhancePromptApiResponse = ApiResponse<EnhancePromptResponse>;
export type PromptSuggestionsApiResponse = ApiResponse<PromptSuggestionsResponse>;
export type PromptHistoryListApiResponse = ApiResponse<PromptHistoryListResponse>;
export type PromptTemplateApiResponse = ApiResponse<PromptTemplate>;
export type PromptTemplateListApiResponse = ApiResponse<PromptTemplateListResponse>;

/** Wire DTO shape returned by the backend history list endpoint before mapping. */
export interface PromptHistoryViewDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly projectId: string;
  readonly userId: string;
  readonly sourcePrompt: string;
  readonly enhancedPrompt: string | null;
  readonly language: PromptLanguage | null;
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly businessType: string | null;
  readonly brandContextSnapshot: Readonly<Record<string, unknown>> | null;
  readonly suggestionType: SuggestionType | null;
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
  readonly tokenUsage: number | null;
  readonly status: PromptHistoryStatus;
  readonly createdAt: string;
}

export interface PromptHistoryPageDto {
  readonly items: readonly PromptHistoryViewDto[];
  readonly totalItems: number;
  readonly totalPages: number;
  readonly page: number;
  readonly size: number;
  readonly first: boolean;
  readonly last: boolean;
}

/** Wire DTO shape returned by the backend template endpoints before mapping. */
export interface PromptTemplateViewDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly name: string;
  readonly category: string | null;
  readonly description: string | null;
  readonly platform: PromptPlatform;
  readonly campaignObjective: CampaignObjective;
  readonly businessType: string | null;
  readonly language: PromptLanguage;
  readonly templateText: string;
  readonly systemDefault: boolean;
  readonly status: PromptTemplateStatus;
  readonly createdBy: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreatePromptTemplateRequestDto {
  readonly name: string;
  readonly category: string | null;
  readonly description: string | null;
  readonly platform: PromptPlatform;
  readonly campaignObjective: CampaignObjective;
  readonly businessType: string | null;
  readonly language: PromptLanguage;
  readonly templateText: string;
  readonly systemDefault: boolean;
  readonly status: PromptTemplateStatus;
}

export type UpdatePromptTemplateRequestDto = CreatePromptTemplateRequestDto;

export interface PromptEnhancementViewDto {
  readonly enhancedPrompt: string;
  readonly reasoningSummary: string | null;
  readonly suggestedMissingFields: readonly string[] | null;
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
  readonly tokenUsage: number | null;
}

export interface PromptSuggestionsViewDto {
  readonly ctaSuggestions: readonly string[] | null;
  readonly headlineSuggestions: readonly string[] | null;
  readonly offerSuggestions: readonly string[] | null;
  readonly creativeAngleSuggestions: readonly string[] | null;
  readonly campaignToneSuggestions: readonly string[] | null;
  readonly businessCategorySuggestions: readonly string[] | null;
  readonly reasoningSummary: string | null;
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
  readonly tokenUsage: number | null;
}

export const DEFAULT_PROMPT_HISTORY_FILTERS: PromptHistoryFilter = {
  userId: null,
  suggestionType: null,
  platform: null,
  campaignObjective: null,
  status: null,
  createdFrom: null,
  createdTo: null,
};

export const DEFAULT_PROMPT_TEMPLATE_FILTERS: PromptTemplateFilter = {
  category: null,
  platform: null,
  campaignObjective: null,
  language: null,
  businessType: null,
  status: null,
  search: null,
  systemDefault: null,
  includeSystemDefaults: true,
};

export function mapPromptHistoryViewDto(source: PromptHistoryViewDto): PromptHistory {
  return {
    id: source.id,
    workspaceId: source.workspaceId,
    projectCampaignId: source.projectId,
    creativeRequestId: null,
    sourcePrompt: source.sourcePrompt,
    enhancedPrompt: source.enhancedPrompt,
    language: source.language,
    platform: source.platform,
    campaignObjective: source.campaignObjective,
    businessType: source.businessType,
    brandContextSnapshot: source.brandContextSnapshot,
    suggestionType: source.suggestionType,
    status: source.status,
    aiProvider: source.aiProvider,
    model: source.aiModel,
    tokenUsage: source.tokenUsage,
    createdBy: source.userId,
    createdAt: source.createdAt,
  };
}

export function mapPromptTemplateViewDto(source: PromptTemplateViewDto): PromptTemplate {
  return {
    id: source.id,
    workspaceId: source.workspaceId,
    name: source.name,
    category: source.category,
    description: source.description,
    platform: source.platform,
    campaignObjective: source.campaignObjective,
    businessType: source.businessType,
    language: source.language,
    promptBody: source.templateText,
    isDefault: source.systemDefault,
    status: source.status,
    createdBy: source.createdBy,
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
  };
}

export function mapPromptHistoryPageDto(source: PromptHistoryPageDto): PromptHistoryPage {
  return {
    items: source.items.map(mapPromptHistoryViewDto),
    pagination: mapPaginationDto(source),
  };
}

export function mapEnhancementViewDto(source: PromptEnhancementViewDto): EnhancePromptResponse {
  return {
    enhancedPrompt: source.enhancedPrompt,
    reasoningSummary: source.reasoningSummary,
    suggestedMissingFields: source.suggestedMissingFields ?? [],
    aiProvider: source.aiProvider,
    model: source.aiModel,
    tokenUsage: source.tokenUsage,
  };
}

export function mapSuggestionsViewDto(source: PromptSuggestionsViewDto): PromptSuggestionsResponse {
  return {
    ctaSuggestions: source.ctaSuggestions ?? [],
    headlineSuggestions: source.headlineSuggestions ?? [],
    offerSuggestions: source.offerSuggestions ?? [],
    creativeAngleSuggestions: source.creativeAngleSuggestions ?? [],
    campaignToneSuggestions: source.campaignToneSuggestions ?? [],
    businessCategorySuggestions: source.businessCategorySuggestions ?? [],
    reasoningSummary: source.reasoningSummary,
    aiProvider: source.aiProvider,
    model: source.aiModel,
    tokenUsage: source.tokenUsage,
  };
}

export function mapCreatePromptTemplateRequest(
  source: CreatePromptTemplateRequest,
): CreatePromptTemplateRequestDto {
  return {
    name: source.name,
    category: source.category,
    description: source.description,
    platform: source.platform,
    campaignObjective: source.campaignObjective,
    businessType: source.businessType,
    language: source.language,
    templateText: source.promptBody,
    systemDefault: source.isDefault,
    status: source.status,
  };
}

function mapPaginationDto(source: PromptHistoryPageDto): PromptPagination {
  return {
    page: source.page,
    size: source.size,
    totalItems: source.totalItems,
    totalPages: source.totalPages,
    first: source.first,
    last: source.last,
  };
}
