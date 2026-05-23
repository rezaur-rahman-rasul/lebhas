import {
  CampaignObjective,
  CreatePromptTemplateRequest,
  PromptIntelligenceContext,
  PromptLanguage,
  PromptPlatform,
  UpdatePromptTemplateRequest,
} from '../models';
import {
  PROMPT_MAX_LENGTH,
  PROMPT_MIN_LENGTH,
  PROMPT_TEMPLATE_NAME_MAX_LENGTH,
} from '../models/prompt.models';

const PLATFORMS = new Set<PromptPlatform>(['FACEBOOK', 'INSTAGRAM', 'TIKTOK', 'LINKEDIN']);
const LANGUAGES = new Set<PromptLanguage>(['ENGLISH', 'BANGLA', 'MIXED']);
const CAMPAIGN_OBJECTIVES = new Set<CampaignObjective>([
  'AWARENESS',
  'TRAFFIC',
  'ENGAGEMENT',
  'LEADS',
  'SALES',
  'APP_PROMOTION',
  'BRAND_PROMOTION',
]);

export function validateProjectId(projectId: string | null | undefined): string | null {
  return projectId?.trim() ? null : 'Select a project before using prompt intelligence.';
}

export function validateSourcePrompt(sourcePrompt: string): Readonly<Record<string, string>> {
  const errors: Record<string, string> = {};
  const trimmed = sourcePrompt.trim();

  if (!trimmed) {
    errors['sourcePrompt'] = 'Enter a source prompt.';
    return errors;
  }

  if (trimmed.length < PROMPT_MIN_LENGTH) {
    errors['sourcePrompt'] = `Prompt must be at least ${PROMPT_MIN_LENGTH} characters.`;
  } else if (trimmed.length > PROMPT_MAX_LENGTH) {
    errors['sourcePrompt'] = `Prompt must be ${PROMPT_MAX_LENGTH} characters or fewer.`;
  }

  return errors;
}

export function validateIntelligenceContext(
  context: Pick<
    PromptIntelligenceContext,
    'platform' | 'language' | 'campaignObjective'
  >,
): Readonly<Record<string, string>> {
  const errors: Record<string, string> = {};

  if (!context.platform || !PLATFORMS.has(context.platform)) {
    errors['platform'] = 'Select a valid platform.';
  }

  if (!context.language || !LANGUAGES.has(context.language)) {
    errors['language'] = 'Select a valid language.';
  }

  if (!context.campaignObjective || !CAMPAIGN_OBJECTIVES.has(context.campaignObjective)) {
    errors['campaignObjective'] = 'Select a valid campaign objective.';
  }

  return errors;
}

export function validateEnhanceRequest(
  projectId: string | null | undefined,
  sourcePrompt: string,
  context: Pick<
    PromptIntelligenceContext,
    'platform' | 'language' | 'campaignObjective'
  >,
): Readonly<Record<string, string>> {
  return {
    ...projectIdError(projectId),
    ...validateSourcePrompt(sourcePrompt),
    ...validateIntelligenceContext(context),
  };
}

export function validateTemplatePayload(
  payload: CreatePromptTemplateRequest | UpdatePromptTemplateRequest,
): Readonly<Record<string, string>> {
  const errors: Record<string, string> = {};
  const name = payload.name.trim();
  const body = payload.promptBody.trim();

  if (!name) {
    errors['name'] = 'Template name is required.';
  } else if (name.length > PROMPT_TEMPLATE_NAME_MAX_LENGTH) {
    errors['name'] = `Template name must be ${PROMPT_TEMPLATE_NAME_MAX_LENGTH} characters or fewer.`;
  }

  if (!body) {
    errors['promptBody'] = 'Template body is required.';
  } else if (body.length < PROMPT_MIN_LENGTH) {
    errors['promptBody'] = `Template body must be at least ${PROMPT_MIN_LENGTH} characters.`;
  } else if (body.length > PROMPT_MAX_LENGTH) {
    errors['promptBody'] = `Template body must be ${PROMPT_MAX_LENGTH} characters or fewer.`;
  }

  if (!PLATFORMS.has(payload.platform)) {
    errors['platform'] = 'Select a valid platform.';
  }

  if (!LANGUAGES.has(payload.language)) {
    errors['language'] = 'Select a valid language.';
  }

  if (!CAMPAIGN_OBJECTIVES.has(payload.campaignObjective)) {
    errors['campaignObjective'] = 'Select a valid campaign objective.';
  }

  return errors;
}

function projectIdError(projectId: string | null | undefined): Readonly<Record<string, string>> {
  const message = validateProjectId(projectId);
  return message ? { projectId: message } : {};
}
