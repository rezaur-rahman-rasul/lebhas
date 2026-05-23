export type ProjectCampaignStatus = 'ACTIVE' | 'ARCHIVED';

export interface ProjectCampaign {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandId: string;
  readonly productServiceId: string;
  readonly createdByUserId: string;
  readonly name: string;
  readonly description: string | null;
  readonly campaignObjective: string | null;
  readonly targetPlatform: string | null;
  readonly campaignType: string | null;
  readonly status: ProjectCampaignStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreateProjectCampaignPayload {
  readonly name: string;
  readonly description: string | null;
  readonly campaignObjective: string | null;
  readonly targetPlatform: string | null;
  readonly campaignType: string | null;
}

export interface UpdateProjectCampaignPayload extends CreateProjectCampaignPayload {
  readonly status: ProjectCampaignStatus;
}
