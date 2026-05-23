import { Asset } from '@app/features/admin/assets/models/asset.models';

export type PromptPlatform = 'FACEBOOK' | 'INSTAGRAM' | 'TIKTOK' | 'LINKEDIN';
export type CampaignObjective =
  | 'AWARENESS'
  | 'TRAFFIC'
  | 'ENGAGEMENT'
  | 'LEADS'
  | 'SALES'
  | 'APP_PROMOTION'
  | 'BRAND_PROMOTION';
export type PromptLanguage = 'ENGLISH' | 'BANGLA' | 'MIXED';
export type CreativeStyle =
  | 'DIRECT_RESPONSE'
  | 'STORYTELLING'
  | 'UGC'
  | 'CINEMATIC'
  | 'CLEAN'
  | 'BOLD'
  | 'EDUCATIONAL'
  | 'LIFESTYLE';
export type PromptTone =
  | 'PROFESSIONAL'
  | 'FRIENDLY'
  | 'PLAYFUL'
  | 'BOLD'
  | 'MINIMAL'
  | 'LUXURY'
  | 'URGENT'
  | 'EMPATHETIC';
export type SuggestionType =
  | 'ENHANCEMENT'
  | 'REWRITE'
  | 'GENERAL_SUGGESTIONS'
  | 'CTA_SUGGESTIONS'
  | 'HEADLINE_SUGGESTIONS'
  | 'OFFER_SUGGESTIONS'
  | 'CREATIVE_ANGLE_SUGGESTIONS'
  | 'CAMPAIGN_TONE_SUGGESTIONS'
  | 'BUSINESS_CATEGORY_SUGGESTIONS';
export type PromptTemplateStatus = 'ACTIVE' | 'INACTIVE';
export type PromptHistoryStatus = 'SUCCEEDED' | 'FAILED';

export interface PromptTemplate {
  readonly id: string;
  readonly workspaceId: string;
  readonly name: string;
  readonly description: string | null;
  readonly platform: PromptPlatform;
  readonly campaignObjective: CampaignObjective;
  readonly businessType: string | null;
  readonly language: PromptLanguage;
  readonly templateText: string;
  readonly isSystemDefault: boolean;
  readonly status: PromptTemplateStatus;
  readonly createdBy: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface PromptHistory {
  readonly id: string;
  readonly workspaceId: string;
  readonly userId: string;
  readonly sourcePrompt: string;
  readonly enhancedPrompt: string | null;
  readonly language: PromptLanguage | null;
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly businessType: string | null;
  readonly brandContextSnapshot: Readonly<Record<string, unknown>>;
  readonly suggestionType: SuggestionType | null;
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
  readonly tokenUsage: number | null;
  readonly status: PromptHistoryStatus;
  readonly createdAt: string;
}

export interface PromptTemplateFilter {
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly language: PromptLanguage | null;
  readonly businessType: string;
  readonly status: PromptTemplateStatus | null;
  readonly search: string;
  readonly systemDefault: boolean | null;
  readonly includeSystemDefaults: boolean;
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

export interface PromptPagination {
  readonly page: number;
  readonly size: number;
  readonly totalItems: number;
  readonly totalPages: number;
  readonly first: boolean;
  readonly last: boolean;
}

export interface PromptTemplatePayload {
  readonly name: string;
  readonly description: string | null;
  readonly platform: PromptPlatform;
  readonly campaignObjective: CampaignObjective;
  readonly businessType: string | null;
  readonly language: PromptLanguage;
  readonly templateText: string;
  readonly isSystemDefault: boolean;
  readonly status: PromptTemplateStatus;
}

export interface PromptEnhanceRequest {
  readonly prompt: string;
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
  readonly useBrandContext: boolean;
}

export interface PromptRewriteRequest extends PromptEnhanceRequest {}

export interface PromptSuggestionsRequest extends PromptEnhanceRequest {
  readonly suggestionTypes: readonly SuggestionType[];
}

export interface PromptEnhanceResponse {
  readonly enhancedPrompt: string;
  readonly reasoningSummary: string | null;
  readonly suggestedMissingFields: readonly string[];
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
  readonly tokenUsage: number | null;
}

export interface PromptRewriteResponse {
  readonly variations: readonly string[];
  readonly reasoningSummary: string | null;
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
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
  readonly aiModel: string | null;
  readonly tokenUsage: number | null;
}

export interface PromptSuggestionListResponse {
  readonly suggestionType: SuggestionType;
  readonly suggestions: readonly string[];
  readonly reasoningSummary: string | null;
  readonly aiProvider: string | null;
  readonly aiModel: string | null;
  readonly tokenUsage: number | null;
}

export interface PromptSuggestionSection {
  readonly key: SuggestionType;
  readonly title: string;
  readonly suggestions: readonly string[];
}

export interface PromptActionResult {
  readonly ok: boolean;
  readonly message?: string;
  readonly fieldErrors: Readonly<Record<string, string>>;
}

export interface PromptBuilderDraft {
  readonly templateId: string | null;
  readonly prompt: string;
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly businessType: string;
  readonly creativeStyle: CreativeStyle | null;
  readonly language: PromptLanguage | null;
  readonly tone: PromptTone | null;
  readonly targetAudience: string;
  readonly offerDetails: string;
  readonly ctaPreference: string;
  readonly useBrandContext: boolean;
}

export interface PromptAssetOption extends Asset {}

export const PROMPT_MAX_LENGTH = 5000;
export const PROMPT_MIN_LENGTH = 5;
export const PROMPT_TEMPLATE_PAGE_SIZE = 24;
export const PROMPT_HISTORY_PAGE_SIZE = 20;
export const PROMPT_TEMPLATE_NAME_MAX_LENGTH = 120;
export const PROMPT_TAG_MAX_LENGTH = 80;

export const DEFAULT_PROMPT_TEMPLATE_FILTERS: PromptTemplateFilter = {
  platform: null,
  campaignObjective: null,
  language: null,
  businessType: '',
  status: null,
  search: '',
  systemDefault: null,
  includeSystemDefaults: true,
};

export const DEFAULT_PROMPT_HISTORY_FILTERS: PromptHistoryFilter = {
  userId: null,
  suggestionType: null,
  platform: null,
  campaignObjective: null,
  status: null,
  createdFrom: null,
  createdTo: null,
};

export const DEFAULT_PROMPT_TEMPLATE_PAGINATION: PromptPagination = {
  page: 0,
  size: PROMPT_TEMPLATE_PAGE_SIZE,
  totalItems: 0,
  totalPages: 0,
  first: true,
  last: true,
};

export const DEFAULT_PROMPT_HISTORY_PAGINATION: PromptPagination = {
  page: 0,
  size: PROMPT_HISTORY_PAGE_SIZE,
  totalItems: 0,
  totalPages: 0,
  first: true,
  last: true,
};

export const EMPTY_PROMPT_SUGGESTIONS: PromptSuggestionsResponse = {
  ctaSuggestions: [],
  headlineSuggestions: [],
  offerSuggestions: [],
  creativeAngleSuggestions: [],
  campaignToneSuggestions: [],
  businessCategorySuggestions: [],
  reasoningSummary: null,
  aiProvider: null,
  aiModel: null,
  tokenUsage: null,
};

export const PLATFORM_OPTIONS: readonly { readonly value: PromptPlatform; readonly label: string }[] = [
  { value: 'FACEBOOK', label: 'Facebook' },
  { value: 'INSTAGRAM', label: 'Instagram' },
  { value: 'TIKTOK', label: 'TikTok' },
  { value: 'LINKEDIN', label: 'LinkedIn' },
];

export const CAMPAIGN_OBJECTIVE_OPTIONS: readonly {
  readonly value: CampaignObjective;
  readonly label: string;
}[] = [
  { value: 'AWARENESS', label: 'Awareness' },
  { value: 'TRAFFIC', label: 'Traffic' },
  { value: 'ENGAGEMENT', label: 'Engagement' },
  { value: 'LEADS', label: 'Leads' },
  { value: 'SALES', label: 'Sales' },
  { value: 'APP_PROMOTION', label: 'App promotion' },
  { value: 'BRAND_PROMOTION', label: 'Brand promotion' },
];

export const PROMPT_LANGUAGE_OPTIONS: readonly {
  readonly value: PromptLanguage;
  readonly label: string;
}[] = [
  { value: 'ENGLISH', label: 'English' },
  { value: 'BANGLA', label: 'Bangla' },
  { value: 'MIXED', label: 'Mixed' },
];

export const CREATIVE_STYLE_OPTIONS: readonly {
  readonly value: CreativeStyle;
  readonly label: string;
}[] = [
  { value: 'DIRECT_RESPONSE', label: 'Direct response' },
  { value: 'STORYTELLING', label: 'Storytelling' },
  { value: 'UGC', label: 'UGC' },
  { value: 'CINEMATIC', label: 'Cinematic' },
  { value: 'CLEAN', label: 'Clean' },
  { value: 'BOLD', label: 'Bold' },
  { value: 'EDUCATIONAL', label: 'Educational' },
  { value: 'LIFESTYLE', label: 'Lifestyle' },
];

export const PROMPT_TONE_OPTIONS: readonly {
  readonly value: PromptTone;
  readonly label: string;
}[] = [
  { value: 'PROFESSIONAL', label: 'Professional' },
  { value: 'FRIENDLY', label: 'Friendly' },
  { value: 'PLAYFUL', label: 'Playful' },
  { value: 'BOLD', label: 'Bold' },
  { value: 'MINIMAL', label: 'Minimal' },
  { value: 'LUXURY', label: 'Luxury' },
  { value: 'URGENT', label: 'Urgent' },
  { value: 'EMPATHETIC', label: 'Empathetic' },
];

export const PROMPT_TEMPLATE_STATUS_OPTIONS: readonly {
  readonly value: PromptTemplateStatus;
  readonly label: string;
}[] = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
];

export const PROMPT_HISTORY_STATUS_OPTIONS: readonly {
  readonly value: PromptHistoryStatus;
  readonly label: string;
}[] = [
  { value: 'SUCCEEDED', label: 'Succeeded' },
  { value: 'FAILED', label: 'Failed' },
];

export const PROMPT_SUGGESTION_TYPE_OPTIONS: readonly {
  readonly value: SuggestionType;
  readonly label: string;
}[] = [
  { value: 'GENERAL_SUGGESTIONS', label: 'General suggestions' },
  { value: 'CTA_SUGGESTIONS', label: 'CTA suggestions' },
  { value: 'HEADLINE_SUGGESTIONS', label: 'Headline suggestions' },
  { value: 'OFFER_SUGGESTIONS', label: 'Offer suggestions' },
  { value: 'CREATIVE_ANGLE_SUGGESTIONS', label: 'Creative angle suggestions' },
  { value: 'CAMPAIGN_TONE_SUGGESTIONS', label: 'Campaign tone suggestions' },
  { value: 'BUSINESS_CATEGORY_SUGGESTIONS', label: 'Business category suggestions' },
];

export function promptPlatformLabel(value: PromptPlatform | null): string {
  return PLATFORM_OPTIONS.find((option) => option.value === value)?.label ?? 'Not selected';
}

export function campaignObjectiveLabel(value: CampaignObjective | null): string {
  return (
    CAMPAIGN_OBJECTIVE_OPTIONS.find((option) => option.value === value)?.label ?? 'Not selected'
  );
}

export function promptLanguageLabel(value: PromptLanguage | null): string {
  return PROMPT_LANGUAGE_OPTIONS.find((option) => option.value === value)?.label ?? 'Not selected';
}

export function promptTemplateStatusLabel(value: PromptTemplateStatus): string {
  return PROMPT_TEMPLATE_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export function promptHistoryStatusLabel(value: PromptHistoryStatus): string {
  return PROMPT_HISTORY_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export function suggestionTypeLabel(value: SuggestionType | null): string {
  return PROMPT_SUGGESTION_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? 'Prompt event';
}

export function promptTemplateStatusTone(
  value: PromptTemplateStatus,
): 'brand' | 'blue' | 'red' | 'neutral' {
  return value === 'ACTIVE' ? 'brand' : 'neutral';
}

export function promptHistoryStatusTone(
  value: PromptHistoryStatus,
): 'brand' | 'blue' | 'red' | 'neutral' {
  return value === 'SUCCEEDED' ? 'brand' : 'red';
}

export function buildPromptSuggestionSections(
  suggestions: PromptSuggestionsResponse | null,
): readonly PromptSuggestionSection[] {
  if (!suggestions) {
    return [];
  }

  const sections: readonly PromptSuggestionSection[] = [
    {
      key: 'CTA_SUGGESTIONS',
      title: 'CTA suggestions',
      suggestions: suggestions.ctaSuggestions,
    },
    {
      key: 'HEADLINE_SUGGESTIONS',
      title: 'Headline suggestions',
      suggestions: suggestions.headlineSuggestions,
    },
    {
      key: 'OFFER_SUGGESTIONS',
      title: 'Offer suggestions',
      suggestions: suggestions.offerSuggestions,
    },
    {
      key: 'CREATIVE_ANGLE_SUGGESTIONS',
      title: 'Creative angle suggestions',
      suggestions: suggestions.creativeAngleSuggestions,
    },
    {
      key: 'CAMPAIGN_TONE_SUGGESTIONS',
      title: 'Campaign tone suggestions',
      suggestions: suggestions.campaignToneSuggestions,
    },
    {
      key: 'BUSINESS_CATEGORY_SUGGESTIONS',
      title: 'Business category suggestions',
      suggestions: suggestions.businessCategorySuggestions,
    },
  ];

  return sections.filter((section) => section.suggestions.length > 0);
}

export function normalizePromptText(value: string): string {
  return value.trim();
}
