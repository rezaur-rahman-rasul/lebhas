import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import {
  CampaignObjective,
  PromptEnhanceRequest,
  PromptEnhanceResponse,
  PromptHistory,
  PromptHistoryFilter,
  PromptHistoryStatus,
  PromptLanguage,
  PromptPagination,
  PromptPlatform,
  PromptRewriteRequest,
  PromptRewriteResponse,
  PromptSuggestionListResponse,
  PromptSuggestionsRequest,
  PromptSuggestionsResponse,
  PromptTemplate,
  PromptTemplateFilter,
  PromptTemplatePayload,
  PromptTemplateStatus,
  SuggestionType,
  DEFAULT_PROMPT_HISTORY_PAGINATION,
} from '../models/prompt.models';

interface PagedResultDto<T> {
  readonly items: readonly T[];
  readonly totalItems: number;
  readonly totalPages: number;
  readonly page: number;
  readonly size: number;
  readonly first: boolean;
  readonly last: boolean;
}

interface PromptTemplateResponseDto {
  readonly id: string;
  readonly workspaceId: string;
  readonly name: string;
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

interface PromptHistoryResponseDto {
  readonly id: string;
  readonly workspaceId: string;
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

interface PromptEnhancementResponseDto {
  readonly enhancedPrompt: string;
  readonly reasoningSummary: string | null;
  readonly suggestedMissingFields: readonly string[] | null;
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
  readonly tokenUsage: number | null;
}

interface PromptRewriteResponseDto {
  readonly variations: readonly string[] | null;
  readonly reasoningSummary: string | null;
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
  readonly tokenUsage: number | null;
}

interface PromptSuggestionsResponseDto {
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

interface PromptSuggestionListResponseDto {
  readonly suggestionType: SuggestionType;
  readonly suggestions: readonly string[] | null;
  readonly reasoningSummary: string | null;
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
  readonly tokenUsage: number | null;
}

interface PromptRequestDto {
  readonly customPrompt: string;
  readonly assetIds: readonly string[];
  readonly templateId: string | null;
  readonly businessType: string | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly platform: PromptPlatform | null;
  readonly creativeStyle: PromptEnhanceRequest['creativeStyle'];
  readonly language: PromptLanguage | null;
  readonly tone: PromptEnhanceRequest['tone'];
  readonly targetAudience: string | null;
  readonly offerDetails: string | null;
  readonly ctaPreference: string | null;
  readonly useBrandProfile: boolean;
}

interface PromptSuggestionsRequestDto extends PromptRequestDto {
  readonly suggestionTypes: readonly SuggestionType[];
}

@Injectable({ providedIn: 'root' })
export class PromptService {
  private readonly api = inject(ApiService);

  listTemplates(workspaceId: string, filters: PromptTemplateFilter, context?: HttpContext) {
    return this.api
      .get<readonly PromptTemplateResponseDto[]>(`/api/v1/workspaces/${workspaceId}/prompt-templates`, {
        params: {
          platform: filters.platform,
          campaignObjective: filters.campaignObjective,
          language: filters.language,
          businessType: filters.businessType || null,
          status: filters.status,
          search: filters.search || null,
          systemDefault: filters.systemDefault,
          includeSystemDefaults: filters.includeSystemDefaults,
        },
        context,
      })
      .pipe(map(({ data }) => data.map(mapPromptTemplate)));
  }

  getTemplate(workspaceId: string, templateId: string, context?: HttpContext) {
    return this.api
      .get<PromptTemplateResponseDto>(
        `/api/v1/workspaces/${workspaceId}/prompt-templates/${templateId}`,
        { context },
      )
      .pipe(map(({ data }) => mapPromptTemplate(data)));
  }

  createTemplate(workspaceId: string, payload: PromptTemplatePayload) {
    return this.api
      .post<PromptTemplateResponseDto, PromptTemplateRequestDto>(
        `/api/v1/workspaces/${workspaceId}/prompt-templates`,
        mapPromptTemplateRequest(payload),
      )
      .pipe(map(({ data }) => mapPromptTemplate(data)));
  }

  updateTemplate(workspaceId: string, templateId: string, payload: PromptTemplatePayload) {
    return this.api
      .put<PromptTemplateResponseDto, PromptTemplateRequestDto>(
        `/api/v1/workspaces/${workspaceId}/prompt-templates/${templateId}`,
        mapPromptTemplateRequest(payload),
      )
      .pipe(map(({ data }) => mapPromptTemplate(data)));
  }

  deleteTemplate(workspaceId: string, templateId: string) {
    return this.api.delete<void>(`/api/v1/workspaces/${workspaceId}/prompt-templates/${templateId}`);
  }

  enhancePrompt(workspaceId: string, payload: PromptEnhanceRequest) {
    return this.api
      .post<PromptEnhancementResponseDto, PromptRequestDto>(
        `/api/v1/workspaces/${workspaceId}/prompts/enhance`,
        mapPromptRequest(payload),
      )
      .pipe(map(({ data }) => mapEnhancement(data)));
  }

  rewritePrompt(workspaceId: string, payload: PromptRewriteRequest) {
    return this.api
      .post<PromptRewriteResponseDto, PromptRewriteRequestDto>(
        `/api/v1/workspaces/${workspaceId}/prompts/rewrite`,
        mapPromptRewriteRequest(payload),
      )
      .pipe(map(({ data }) => mapRewrite(data)));
  }

  generateSuggestions(workspaceId: string, payload: PromptSuggestionsRequest) {
    return this.api
      .post<PromptSuggestionsResponseDto, PromptSuggestionsRequestDto>(
        `/api/v1/workspaces/${workspaceId}/prompts/suggestions`,
        mapPromptSuggestionsRequest(payload),
      )
      .pipe(map(({ data }) => mapSuggestions(data)));
  }

  generateCtaSuggestions(workspaceId: string, payload: PromptSuggestionsRequest) {
    return this.generateSuggestionList(
      workspaceId,
      '/cta-suggestions',
      payload,
    );
  }

  generateHeadlineSuggestions(workspaceId: string, payload: PromptSuggestionsRequest) {
    return this.generateSuggestionList(
      workspaceId,
      '/headline-suggestions',
      payload,
    );
  }

  generateOfferSuggestions(workspaceId: string, payload: PromptSuggestionsRequest) {
    return this.generateSuggestionList(
      workspaceId,
      '/offer-suggestions',
      payload,
    );
  }

  generateCreativeAngleSuggestions(workspaceId: string, payload: PromptSuggestionsRequest) {
    return this.generateSuggestionList(
      workspaceId,
      '/creative-angle-suggestions',
      payload,
    );
  }

  generateCampaignToneSuggestions(workspaceId: string, payload: PromptSuggestionsRequest) {
    return this.generateSuggestionList(
      workspaceId,
      '/campaign-tone-suggestions',
      payload,
    );
  }

  generateBusinessCategorySuggestions(workspaceId: string, payload: PromptSuggestionsRequest) {
    return this.generateSuggestionList(
      workspaceId,
      '/business-category-suggestions',
      payload,
    );
  }

  listHistory(
    workspaceId: string,
    filters: PromptHistoryFilter,
    page: number,
    size: number,
    context?: HttpContext,
  ) {
    return this.api
      .get<PagedResultDto<PromptHistoryResponseDto>>(
        `/api/v1/workspaces/${workspaceId}/prompt-history`,
        {
          params: {
            userId: filters.userId,
            suggestionType: filters.suggestionType,
            platform: filters.platform,
            campaignObjective: filters.campaignObjective,
            status: filters.status,
            createdFrom: filters.createdFrom,
            createdTo: filters.createdTo,
            page,
            size,
          },
          context,
        },
      )
      .pipe(
        map(({ data }) => ({
          items: data.items.map(mapPromptHistory),
          pagination: mapPagination(data),
        })),
      );
  }

  getHistory(workspaceId: string, historyId: string, context?: HttpContext) {
    return this.api
      .get<PromptHistoryResponseDto>(`/api/v1/workspaces/${workspaceId}/prompt-history/${historyId}`, {
        context,
      })
      .pipe(map(({ data }) => mapPromptHistory(data)));
  }

  private generateSuggestionList(
    workspaceId: string,
    endpoint: string,
    payload: PromptSuggestionsRequest,
  ) {
    return this.api
      .post<PromptSuggestionListResponseDto, PromptSuggestionsRequestDto>(
        `/api/v1/workspaces/${workspaceId}/prompts${endpoint}`,
        mapPromptSuggestionsRequest(payload),
      )
      .pipe(map(({ data }) => mapSuggestionList(data)));
  }
}

interface PromptTemplateRequestDto {
  readonly name: string;
  readonly description: string | null;
  readonly platform: PromptPlatform;
  readonly campaignObjective: CampaignObjective;
  readonly businessType: string | null;
  readonly language: PromptLanguage;
  readonly templateText: string;
  readonly systemDefault: boolean;
  readonly status: PromptTemplateStatus;
}

interface PromptRewriteRequestDto extends PromptRequestDto {
  readonly existingPrompt: string;
}

function mapPromptTemplate(source: PromptTemplateResponseDto): PromptTemplate {
  return {
    id: source.id,
    workspaceId: source.workspaceId,
    name: source.name,
    description: source.description,
    platform: source.platform,
    campaignObjective: source.campaignObjective,
    businessType: source.businessType,
    language: source.language,
    templateText: source.templateText,
    isSystemDefault: source.systemDefault,
    status: source.status,
    createdBy: source.createdBy,
    createdAt: source.createdAt,
    updatedAt: source.updatedAt,
  };
}

function mapPromptHistory(source: PromptHistoryResponseDto): PromptHistory {
  return {
    id: source.id,
    workspaceId: source.workspaceId,
    userId: source.userId,
    sourcePrompt: source.sourcePrompt,
    enhancedPrompt: source.enhancedPrompt,
    language: source.language,
    platform: source.platform,
    campaignObjective: source.campaignObjective,
    businessType: source.businessType,
    brandContextSnapshot: source.brandContextSnapshot ?? {},
    suggestionType: source.suggestionType,
    aiProvider: source.aiProvider,
    aiModel: source.aiModel,
    tokenUsage: source.tokenUsage,
    status: source.status,
    createdAt: source.createdAt,
  };
}

function mapEnhancement(source: PromptEnhancementResponseDto): PromptEnhanceResponse {
  return {
    enhancedPrompt: source.enhancedPrompt,
    reasoningSummary: source.reasoningSummary,
    suggestedMissingFields: source.suggestedMissingFields ?? [],
    aiProvider: source.aiProvider,
    aiModel: source.aiModel,
    tokenUsage: source.tokenUsage,
  };
}

function mapRewrite(source: PromptRewriteResponseDto): PromptRewriteResponse {
  return {
    variations: source.variations ?? [],
    reasoningSummary: source.reasoningSummary,
    aiProvider: source.aiProvider,
    aiModel: source.aiModel,
    tokenUsage: source.tokenUsage,
  };
}

function mapSuggestions(source: PromptSuggestionsResponseDto): PromptSuggestionsResponse {
  return {
    ctaSuggestions: source.ctaSuggestions ?? [],
    headlineSuggestions: source.headlineSuggestions ?? [],
    offerSuggestions: source.offerSuggestions ?? [],
    creativeAngleSuggestions: source.creativeAngleSuggestions ?? [],
    campaignToneSuggestions: source.campaignToneSuggestions ?? [],
    businessCategorySuggestions: source.businessCategorySuggestions ?? [],
    reasoningSummary: source.reasoningSummary,
    aiProvider: source.aiProvider,
    aiModel: source.aiModel,
    tokenUsage: source.tokenUsage,
  };
}

function mapSuggestionList(source: PromptSuggestionListResponseDto): PromptSuggestionListResponse {
  return {
    suggestionType: source.suggestionType,
    suggestions: source.suggestions ?? [],
    reasoningSummary: source.reasoningSummary,
    aiProvider: source.aiProvider,
    aiModel: source.aiModel,
    tokenUsage: source.tokenUsage,
  };
}

function mapPagination(source: PagedResultDto<PromptHistoryResponseDto>): PromptPagination {
  return {
    page: source.page ?? DEFAULT_PROMPT_HISTORY_PAGINATION.page,
    size: source.size ?? DEFAULT_PROMPT_HISTORY_PAGINATION.size,
    totalItems: source.totalItems ?? DEFAULT_PROMPT_HISTORY_PAGINATION.totalItems,
    totalPages: source.totalPages ?? DEFAULT_PROMPT_HISTORY_PAGINATION.totalPages,
    first: source.first ?? DEFAULT_PROMPT_HISTORY_PAGINATION.first,
    last: source.last ?? DEFAULT_PROMPT_HISTORY_PAGINATION.last,
  };
}

function mapPromptTemplateRequest(payload: PromptTemplatePayload): PromptTemplateRequestDto {
  return {
    name: payload.name,
    description: payload.description,
    platform: payload.platform,
    campaignObjective: payload.campaignObjective,
    businessType: payload.businessType,
    language: payload.language,
    templateText: payload.templateText,
    systemDefault: payload.isSystemDefault,
    status: payload.status,
  };
}

function mapPromptRequest(payload: PromptEnhanceRequest): PromptRequestDto {
  return {
    customPrompt: payload.prompt,
    assetIds: payload.assetIds,
    templateId: payload.templateId,
    businessType: payload.businessType,
    campaignObjective: payload.campaignObjective,
    platform: payload.platform,
    creativeStyle: payload.creativeStyle,
    language: payload.language,
    tone: payload.tone,
    targetAudience: payload.targetAudience,
    offerDetails: payload.offerDetails,
    ctaPreference: payload.ctaPreference,
    useBrandProfile: payload.useBrandContext,
  };
}

function mapPromptRewriteRequest(payload: PromptRewriteRequest): PromptRewriteRequestDto {
  return {
    ...mapPromptRequest(payload),
    existingPrompt: payload.prompt,
  };
}

function mapPromptSuggestionsRequest(payload: PromptSuggestionsRequest): PromptSuggestionsRequestDto {
  return {
    ...mapPromptRequest(payload),
    suggestionTypes: payload.suggestionTypes,
  };
}
