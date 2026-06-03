import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  CreatePromptTemplateRequest,
  EnhancePromptRequest,
  EnhancePromptResponse,
  mapCreatePromptTemplateRequest,
  mapEnhancementViewDto,
  mapPromptHistoryPageDto,
  mapPromptTemplateViewDto,
  mapSuggestionsViewDto,
  PromptEnhancementViewDto,
  PromptHistoryListQuery,
  PromptHistoryPage,
  PromptHistoryPageDto,
  PromptSuggestionsRequest,
  PromptSuggestionsResponse,
  PromptSuggestionsViewDto,
  PromptTemplate,
  PromptTemplateFilter,
  PromptTemplateViewDto,
  SuggestionType,
  UpdatePromptTemplateRequest,
} from '../models';

interface PromptIntelligenceRequestDto {
  readonly customPrompt: string;
  readonly assetIds: readonly string[];
  readonly templateId: string | null;
  readonly businessType: string | null;
  readonly campaignObjective: EnhancePromptRequest['campaignObjective'];
  readonly platform: EnhancePromptRequest['platform'];
  readonly creativeStyle: EnhancePromptRequest['creativeStyle'];
  readonly language: EnhancePromptRequest['language'];
  readonly tone: EnhancePromptRequest['tone'];
  readonly targetAudience: string | null;
  readonly offerDetails: string | null;
  readonly ctaPreference: string | null;
  readonly useBrandProfile: boolean;
}

interface PromptSuggestionsRequestDto extends PromptIntelligenceRequestDto {
  readonly suggestionTypes: readonly SuggestionType[];
}

@Injectable({ providedIn: 'root' })
export class PromptApiService {
  private readonly api = inject(ApiService);

  async enhancePrompt(
    workspaceId: string,
    projectId: string,
    payload: EnhancePromptRequest,
    context?: HttpContext,
  ): Promise<EnhancePromptResponse> {
    const response = await firstValueFrom(
      this.api.post<PromptEnhancementViewDto, PromptIntelligenceRequestDto>(
        this.projectPromptsPath(workspaceId, projectId, 'enhance'),
        mapIntelligenceRequestDto(payload),
        { context },
      ),
    );

    return mapEnhancementViewDto(unwrapApiResponse(response));
  }

  async generateSuggestions(
    workspaceId: string,
    projectId: string,
    payload: PromptSuggestionsRequest,
    context?: HttpContext,
  ): Promise<PromptSuggestionsResponse> {
    const response = await firstValueFrom(
      this.api.post<PromptSuggestionsViewDto, PromptSuggestionsRequestDto>(
        this.projectPromptsPath(workspaceId, projectId, 'suggestions'),
        mapSuggestionsRequestDto(payload),
        { context },
      ),
    );

    return mapSuggestionsViewDto(unwrapApiResponse(response));
  }

  async listHistory(
    workspaceId: string,
    projectId: string,
    query: PromptHistoryListQuery,
    context?: HttpContext,
  ): Promise<PromptHistoryPage> {
    const response = await firstValueFrom(
      this.api.get<PromptHistoryPageDto>(
        this.projectPromptsPath(workspaceId, projectId, 'history'),
        {
          params: {
            userId: query.userId,
            suggestionType: query.suggestionType,
            platform: query.platform,
            campaignObjective: query.campaignObjective,
            status: query.status,
            createdFrom: query.createdFrom,
            createdTo: query.createdTo,
            page: query.page,
            size: query.size,
          },
          context,
        },
      ),
    );

    return mapPromptHistoryPageDto(unwrapApiResponse(response));
  }

  async listTemplates(
    workspaceId: string,
    filters: PromptTemplateFilter,
    context?: HttpContext,
  ): Promise<readonly PromptTemplate[]> {
    const response = await firstValueFrom(
      this.api.get<readonly PromptTemplateViewDto[]>(
        ApiEndpoints.prompts.templates(workspaceId),
        {
          params: {
            category: filters.category,
            platform: filters.platform,
            campaignObjective: filters.campaignObjective,
            language: filters.language,
            businessType: filters.businessType,
            status: filters.status,
            search: filters.search,
            systemDefault: filters.systemDefault,
            includeSystemDefaults: filters.includeSystemDefaults,
          },
          context,
        },
      ),
    );

    return unwrapApiResponse(response).map(mapPromptTemplateViewDto);
  }

  async createTemplate(
    workspaceId: string,
    payload: CreatePromptTemplateRequest,
    context?: HttpContext,
  ): Promise<PromptTemplate> {
    const response = await firstValueFrom(
      this.api.post<PromptTemplateViewDto, ReturnType<typeof mapCreatePromptTemplateRequest>>(
        ApiEndpoints.prompts.templates(workspaceId),
        mapCreatePromptTemplateRequest(payload),
        { context },
      ),
    );

    return mapPromptTemplateViewDto(unwrapApiResponse(response));
  }

  async updateTemplate(
    workspaceId: string,
    templateId: string,
    payload: UpdatePromptTemplateRequest,
    context?: HttpContext,
  ): Promise<PromptTemplate> {
    const response = await firstValueFrom(
      this.api.put<PromptTemplateViewDto, ReturnType<typeof mapCreatePromptTemplateRequest>>(
        `${ApiEndpoints.prompts.templates(workspaceId)}/${encodeURIComponent(templateId)}`,
        mapCreatePromptTemplateRequest(payload),
        { context },
      ),
    );

    return mapPromptTemplateViewDto(unwrapApiResponse(response));
  }

  async deleteTemplate(workspaceId: string, templateId: string, context?: HttpContext): Promise<void> {
    await firstValueFrom(
      this.api.delete<void>(`${ApiEndpoints.prompts.templates(workspaceId)}/${encodeURIComponent(templateId)}`, {
        context,
      }),
    );
  }

  private projectPromptsPath(workspaceId: string, projectId: string, suffix: string): string {
    switch (suffix) {
      case 'enhance':
        return ApiEndpoints.prompts.enhance(workspaceId, projectId);
      case 'suggestions':
        return ApiEndpoints.prompts.suggestions(workspaceId, projectId);
      case 'history':
        return ApiEndpoints.prompts.history(workspaceId, projectId);
      default:
        return `/api/v1/workspaces/${encodeURIComponent(workspaceId)}/projects/${encodeURIComponent(projectId)}/prompts/${encodeURIComponent(suffix)}`;
    }
  }
}

function mapIntelligenceRequestDto(payload: EnhancePromptRequest): PromptIntelligenceRequestDto {
  return {
    customPrompt: payload.customPrompt.trim(),
    assetIds: [...payload.assetIds],
    templateId: payload.templateId,
    businessType: normalizeOptionalText(payload.businessType),
    campaignObjective: payload.campaignObjective,
    platform: payload.platform,
    creativeStyle: payload.creativeStyle,
    language: payload.language,
    tone: payload.tone,
    targetAudience: normalizeOptionalText(payload.targetAudience),
    offerDetails: normalizeOptionalText(payload.offerDetails),
    ctaPreference: normalizeOptionalText(payload.ctaPreference),
    useBrandProfile: payload.useBrandProfile,
  };
}

function mapSuggestionsRequestDto(payload: PromptSuggestionsRequest): PromptSuggestionsRequestDto {
  return {
    ...mapIntelligenceRequestDto(payload),
    suggestionTypes: [...payload.suggestionTypes],
  };
}

function normalizeOptionalText(value: string | null): string | null {
  const trimmed = value?.trim();
  return trimmed ? trimmed : null;
}
