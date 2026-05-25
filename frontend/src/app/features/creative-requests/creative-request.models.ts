export type CreativeRequestStatus = string;

export interface CreativeRequest {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandId: string;
  readonly productServiceId: string;
  readonly projectCampaignId: string;
  readonly title: string;
  readonly brief: string | null;
  readonly status: CreativeRequestStatus;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreateCreativeRequestPayload {
  readonly title: string;
  readonly brief: string | null;
  readonly assetIds?: readonly string[];
  readonly promptId?: string | null;
}

export interface UpdateCreativeRequestPayload {
  readonly title: string;
  readonly brief: string | null;
  readonly status?: CreativeRequestStatus;
}
