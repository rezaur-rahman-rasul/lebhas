import {
  CampaignObjective,
  PromptHistoryStatus,
  PromptLanguage,
  PromptPlatform,
  PromptStatus,
  PromptTemplateStatus,
  PromptTone,
  SuggestionType,
} from './prompt.enums';

/** Project-scoped prompt draft / session record used by the builder UI. */
export interface Prompt {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandId: string;
  readonly productServiceId: string;
  readonly projectCampaignId: string;
  readonly createdBy: string;
  readonly sourcePrompt: string;
  readonly enhancedPrompt: string | null;
  readonly language: PromptLanguage | null;
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly tone: PromptTone | null;
  readonly targetAudience: string | null;
  readonly status: PromptStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
}

/** Workspace prompt template available to the builder. */
export interface PromptTemplate {
  readonly id: string;
  readonly workspaceId: string;
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
  readonly createdBy: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

/** Persisted prompt intelligence event for a project. */
export interface PromptHistory {
  readonly id: string;
  readonly workspaceId: string;
  readonly projectCampaignId: string;
  readonly creativeRequestId: string | null;
  readonly sourcePrompt: string;
  readonly enhancedPrompt: string | null;
  readonly language: PromptLanguage | null;
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly businessType: string | null;
  readonly brandContextSnapshot: Readonly<Record<string, unknown>> | null;
  readonly suggestionType: SuggestionType | null;
  readonly status: PromptHistoryStatus;
  readonly aiProvider: string | null;
  readonly model: string | null;
  readonly tokenUsage: number | null;
  readonly createdBy: string;
  readonly createdAt: string;
}

export interface PromptPagination {
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly first: boolean;
  readonly last: boolean;
}

export interface PromptHistoryPage {
  readonly items: readonly PromptHistory[];
  readonly pagination: PromptPagination;
}

export const PROMPT_MIN_LENGTH = 5;
export const PROMPT_MAX_LENGTH = 5000;
export const PROMPT_TEMPLATE_NAME_MAX_LENGTH = 120;
export const PROMPT_CATEGORY_MAX_LENGTH = 80;
export const PROMPT_BUSINESS_TYPE_MAX_LENGTH = 80;
export const PROMPT_TARGET_AUDIENCE_MAX_LENGTH = 160;
export const PROMPT_OFFER_DETAILS_MAX_LENGTH = 600;
export const PROMPT_CTA_PREFERENCE_MAX_LENGTH = 120;
export const PROMPT_HISTORY_PAGE_SIZE = 20;
export const PROMPT_TEMPLATE_LIST_PAGE_SIZE = 24;

export const DEFAULT_PROMPT_PAGINATION: PromptPagination = {
  page: 0,
  size: PROMPT_HISTORY_PAGE_SIZE,
  totalItems: 0,
  totalPages: 0,
  first: true,
  last: true,
};
