import {
  CampaignObjective,
  PromptLanguage,
  PromptPlatform,
  PromptTone,
} from '@app/features/admin/prompts/models/prompt.models';

export type TextToolKind = 'post' | 'caption' | 'ads-copy' | 'hashtags';
export type TextToolQualityMode = 'Basic' | 'Premium';

export interface TextToolRequest {
  readonly brandId: string;
  readonly brandName: string;
  readonly productServiceId: string;
  readonly productServiceName: string;
  readonly projectId: string;
  readonly projectName: string;
  readonly platform: PromptPlatform;
  readonly language: PromptLanguage;
  readonly tone: PromptTone;
  readonly sourceIdea: string;
  readonly qualityMode: TextToolQualityMode;
  readonly postGoal?: string | null;
  readonly captionStyle?: string | null;
  readonly campaignObjective?: CampaignObjective | null;
  readonly targetAudience?: string | null;
  readonly offerDetails?: string | null;
  readonly industry?: string | null;
  readonly campaignTheme?: string | null;
  readonly hashtagCount?: number | null;
}

export interface TextToolOutputSection {
  readonly title: string;
  readonly body: string;
  readonly copyLabel: string;
}

export interface TextToolResult {
  readonly id: string | null;
  readonly tool: TextToolKind;
  readonly title: string;
  readonly sections: readonly TextToolOutputSection[];
  readonly creditCost: number | null;
  readonly createdAt: string | null;
}

export interface TextToolHistoryItem {
  readonly id: string;
  readonly tool: TextToolKind;
  readonly title: string;
  readonly preview: string;
  readonly createdAt: string | null;
}

