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

export interface PromptSelectOption<T extends string> {
  readonly value: T;
  readonly label: string;
}

export const PLATFORM_OPTIONS: readonly PromptSelectOption<PromptPlatform>[] = [
  { value: 'FACEBOOK', label: 'Facebook' },
  { value: 'INSTAGRAM', label: 'Instagram' },
  { value: 'TIKTOK', label: 'TikTok' },
  { value: 'LINKEDIN', label: 'LinkedIn' },
];

export const CAMPAIGN_OBJECTIVE_OPTIONS: readonly PromptSelectOption<CampaignObjective>[] = [
  { value: 'AWARENESS', label: 'Awareness' },
  { value: 'TRAFFIC', label: 'Traffic' },
  { value: 'ENGAGEMENT', label: 'Engagement' },
  { value: 'LEADS', label: 'Leads' },
  { value: 'SALES', label: 'Sales' },
  { value: 'APP_PROMOTION', label: 'App promotion' },
  { value: 'BRAND_PROMOTION', label: 'Brand promotion' },
];

export const PROMPT_LANGUAGE_OPTIONS: readonly PromptSelectOption<PromptLanguage>[] = [
  { value: 'ENGLISH', label: 'English' },
  { value: 'BANGLA', label: 'Bangla' },
  { value: 'MIXED', label: 'Mixed' },
];

export const PROMPT_TONE_OPTIONS: readonly PromptSelectOption<PromptTone>[] = [
  { value: 'PROFESSIONAL', label: 'Professional' },
  { value: 'FRIENDLY', label: 'Friendly' },
  { value: 'PLAYFUL', label: 'Playful' },
  { value: 'BOLD', label: 'Bold' },
  { value: 'MINIMAL', label: 'Minimal' },
  { value: 'LUXURY', label: 'Luxury' },
  { value: 'URGENT', label: 'Urgent' },
  { value: 'EMPATHETIC', label: 'Empathetic' },
];

export const CREATIVE_STYLE_OPTIONS: readonly PromptSelectOption<CreativeStyle>[] = [
  { value: 'DIRECT_RESPONSE', label: 'Direct response' },
  { value: 'STORYTELLING', label: 'Storytelling' },
  { value: 'UGC', label: 'UGC' },
  { value: 'CINEMATIC', label: 'Cinematic' },
  { value: 'CLEAN', label: 'Clean' },
  { value: 'BOLD', label: 'Bold' },
  { value: 'EDUCATIONAL', label: 'Educational' },
  { value: 'LIFESTYLE', label: 'Lifestyle' },
];

export const PROMPT_HISTORY_STATUS_OPTIONS: readonly PromptSelectOption<PromptHistoryStatus>[] = [
  { value: 'SUCCEEDED', label: 'Succeeded' },
  { value: 'FAILED', label: 'Failed' },
];

export const PROMPT_SUGGESTION_TYPE_OPTIONS: readonly PromptSelectOption<SuggestionType>[] = [
  { value: 'ENHANCEMENT', label: 'Enhancement' },
  { value: 'REWRITE', label: 'Rewrite' },
  { value: 'GENERAL_SUGGESTIONS', label: 'General suggestions' },
  { value: 'CTA_SUGGESTIONS', label: 'CTA suggestions' },
  { value: 'HEADLINE_SUGGESTIONS', label: 'Headline suggestions' },
  { value: 'OFFER_SUGGESTIONS', label: 'Offer suggestions' },
  { value: 'CREATIVE_ANGLE_SUGGESTIONS', label: 'Creative angle suggestions' },
  { value: 'CAMPAIGN_TONE_SUGGESTIONS', label: 'Campaign tone suggestions' },
  { value: 'BUSINESS_CATEGORY_SUGGESTIONS', label: 'Business category suggestions' },
];

export const PROMPT_TEMPLATE_STATUS_OPTIONS: readonly PromptSelectOption<PromptTemplateStatus>[] = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
];

export const DEFAULT_BUILDER_SUGGESTION_TYPES: readonly SuggestionType[] = [
  'CTA_SUGGESTIONS',
  'HEADLINE_SUGGESTIONS',
  'OFFER_SUGGESTIONS',
  'CREATIVE_ANGLE_SUGGESTIONS',
  'CAMPAIGN_TONE_SUGGESTIONS',
  'BUSINESS_CATEGORY_SUGGESTIONS',
];

export function promptPlatformLabel(value: PromptPlatform | null | undefined): string {
  if (!value) {
    return 'Not selected';
  }

  return PLATFORM_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export function campaignObjectiveLabel(value: CampaignObjective | null | undefined): string {
  if (!value) {
    return 'Not selected';
  }

  return CAMPAIGN_OBJECTIVE_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export function promptLanguageLabel(value: PromptLanguage | null | undefined): string {
  if (!value) {
    return 'Not selected';
  }

  return PROMPT_LANGUAGE_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export function promptHistoryStatusLabel(value: PromptHistoryStatus | null | undefined): string {
  if (!value) {
    return 'Unknown';
  }

  return PROMPT_HISTORY_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export function promptHistoryStatusTone(
  value: PromptHistoryStatus,
): 'brand' | 'blue' | 'red' | 'neutral' {
  return value === 'SUCCEEDED' ? 'brand' : 'red';
}

export function suggestionTypeLabel(value: SuggestionType | null | undefined): string {
  if (!value) {
    return 'Prompt event';
  }

  return PROMPT_SUGGESTION_TYPE_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export function promptPreviewText(value: string | null | undefined, maxLength = 180): string {
  if (!value?.trim()) {
    return '—';
  }

  const trimmed = value.trim();
  if (trimmed.length <= maxLength) {
    return trimmed;
  }

  return `${trimmed.slice(0, maxLength).trimEnd()}…`;
}

export function promptTemplateStatusLabel(value: PromptTemplateStatus | null | undefined): string {
  if (!value) {
    return 'Unknown';
  }

  return PROMPT_TEMPLATE_STATUS_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export function promptTemplateStatusTone(
  value: PromptTemplateStatus,
): 'brand' | 'blue' | 'red' | 'neutral' {
  return value === 'ACTIVE' ? 'brand' : 'neutral';
}

export function promptToneLabel(value: PromptTone | null | undefined): string {
  if (!value) {
    return 'Not selected';
  }

  return PROMPT_TONE_OPTIONS.find((option) => option.value === value)?.label ?? value;
}

export function parsePromptPlatform(value: string | null | undefined): PromptPlatform | null {
  if (!value) {
    return null;
  }

  const normalized = value.trim().toUpperCase().replace(/\s+/g, '_') as PromptPlatform;
  return PLATFORM_OPTIONS.some((option) => option.value === normalized) ? normalized : null;
}

export function parseCampaignObjective(value: string | null | undefined): CampaignObjective | null {
  if (!value) {
    return null;
  }

  const normalized = value.trim().toUpperCase().replace(/\s+/g, '_') as CampaignObjective;
  return CAMPAIGN_OBJECTIVE_OPTIONS.some((option) => option.value === normalized)
    ? normalized
    : null;
}
