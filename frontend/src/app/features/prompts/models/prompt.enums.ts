/** Lifecycle of a project-scoped prompt draft in the UI. */
export type PromptStatus = 'DRAFT' | 'ENHANCED' | 'ARCHIVED';

export type PromptLanguage = 'ENGLISH' | 'BANGLA' | 'MIXED';

export type PromptPlatform = 'FACEBOOK' | 'INSTAGRAM' | 'TIKTOK' | 'LINKEDIN';

export type CampaignObjective =
  | 'AWARENESS'
  | 'TRAFFIC'
  | 'ENGAGEMENT'
  | 'LEADS'
  | 'SALES'
  | 'APP_PROMOTION'
  | 'BRAND_PROMOTION';

export type PromptTone =
  | 'PROFESSIONAL'
  | 'FRIENDLY'
  | 'PLAYFUL'
  | 'BOLD'
  | 'MINIMAL'
  | 'LUXURY'
  | 'URGENT'
  | 'EMPATHETIC';

export type CreativeStyle =
  | 'DIRECT_RESPONSE'
  | 'STORYTELLING'
  | 'UGC'
  | 'CINEMATIC'
  | 'CLEAN'
  | 'BOLD'
  | 'EDUCATIONAL'
  | 'LIFESTYLE';

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

/** Backend template status (wire format). */
export type PromptTemplateStatus = 'ACTIVE' | 'INACTIVE';

/** Backend prompt history event status (wire format). */
export type PromptHistoryStatus = 'SUCCEEDED' | 'FAILED';
