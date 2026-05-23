import {
  CampaignObjective,
  CreativeStyle,
  EnhancePromptResponse,
  Prompt,
  PromptSuggestionsResponse,
  PromptTone,
} from '../models';
import { PromptLanguage, PromptPlatform } from '../models/prompt.enums';

export interface PromptActionResult {
  readonly ok: boolean;
  readonly message?: string;
  readonly fieldErrors: Readonly<Record<string, string>>;
}

export interface SelectedProjectContext {
  readonly workspaceId: string;
  readonly projectId: string;
  readonly brandId: string;
  readonly productServiceId: string;
  readonly brandName: string | null;
  readonly productName: string | null;
  readonly projectName: string | null;
}

export interface PromptBuilderSettings {
  readonly templateId: string | null;
  readonly platform: PromptPlatform | null;
  readonly campaignObjective: CampaignObjective | null;
  readonly language: PromptLanguage | null;
  readonly creativeStyle: CreativeStyle | null;
  readonly tone: PromptTone | null;
  readonly businessType: string | null;
  readonly targetAudience: string;
  readonly offerDetails: string;
  readonly ctaPreference: string;
  readonly useBrandProfile: boolean;
}

export const DEFAULT_PROMPT_BUILDER_SETTINGS: PromptBuilderSettings = {
  templateId: null,
  platform: null,
  campaignObjective: null,
  language: 'ENGLISH',
  creativeStyle: null,
  tone: null,
  businessType: null,
  targetAudience: '',
  offerDetails: '',
  ctaPreference: '',
  useBrandProfile: true,
};

export function successResult(): PromptActionResult {
  return { ok: true, fieldErrors: {} };
}

export function failureResult(
  message: string,
  fieldErrors: Readonly<Record<string, string>> = {},
): PromptActionResult {
  return { ok: false, message, fieldErrors };
}

export function fieldErrorsResult(fieldErrors: Readonly<Record<string, string>>): PromptActionResult {
  return { ok: false, fieldErrors };
}

export interface PromptDraftState {
  readonly prompts: readonly Prompt[];
  readonly sourcePrompt: string;
  readonly enhancedPrompt: EnhancePromptResponse | null;
  readonly suggestions: PromptSuggestionsResponse | null;
}
