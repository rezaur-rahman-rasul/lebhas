export type CreativeRequestStatus = string;

export type CreativeRequestFriendlyStatus =
  | 'Queued'
  | 'Generating'
  | 'Completed'
  | 'Failed'
  | 'Refunded';

export interface CreativeRequest {
  readonly id: string;
  readonly workspaceId: string;
  readonly brandId: string;
  readonly productServiceId: string;
  readonly projectCampaignId: string;
  readonly title: string;
  readonly brief: string | null;
  readonly status: CreativeRequestStatus;
  readonly promptId?: string | null;
  readonly assetIds?: readonly string[] | null;
  readonly targetPlatform?: string | null;
  readonly requestedFormat?: string | null;
  readonly requestedVersions?: number | null;
  readonly estimatedCredits?: number | null;
  readonly createdByName?: string | null;
  readonly createdByProfileImageUrl?: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface CreateCreativeRequestPayload {
  readonly title: string;
  readonly brief: string | null;
  readonly assetIds?: readonly string[];
  readonly promptId?: string | null;
  readonly targetPlatform?: string | null;
  readonly requestedFormat?: string | null;
  readonly requestedVersions?: number | null;
}

export interface UpdateCreativeRequestPayload {
  readonly title: string;
  readonly brief: string | null;
  readonly status?: CreativeRequestStatus;
}

export interface CreativeRequestActionResult {
  readonly ok: boolean;
  readonly message?: string;
  readonly fieldErrors: Readonly<Record<string, string>>;
}

export function creativeRequestStatusLabel(status: CreativeRequestStatus | null | undefined): CreativeRequestFriendlyStatus {
  const normalized = status?.trim().toUpperCase().replace(/[\s-]+/g, '_');
  switch (normalized) {
    case 'QUEUED':
    case 'PENDING':
    case 'SUBMITTED':
      return 'Queued';
    case 'GENERATING':
    case 'PROCESSING':
    case 'IN_PROGRESS':
      return 'Generating';
    case 'COMPLETED':
    case 'READY':
    case 'SUCCEEDED':
      return 'Completed';
    case 'FAILED':
    case 'ERROR':
      return 'Failed';
    case 'REFUNDED':
    case 'CREDIT_REFUNDED':
      return 'Refunded';
    default:
      return 'Queued';
  }
}

export function creativeRequestStatusTone(
  status: CreativeRequestStatus | null | undefined,
): 'brand' | 'blue' | 'red' | 'neutral' {
  switch (creativeRequestStatusLabel(status)) {
    case 'Completed':
      return 'brand';
    case 'Generating':
      return 'blue';
    case 'Failed':
      return 'red';
    default:
      return 'neutral';
  }
}
