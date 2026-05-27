export type GeneratedVersionStatus = string;
export type GeneratedVersionFriendlyStatus = 'Queued' | 'Generating' | 'Completed' | 'Failed' | 'Refunded';

export interface GeneratedVersionCapabilities {
  readonly canApprove?: boolean | null;
  readonly canShare?: boolean | null;
  readonly canDownload?: boolean | null;
}

export interface GeneratedVersion {
  readonly id: string;
  readonly workspaceId: string;
  readonly creativeRequestId: string;
  readonly versionNumber: number | null;
  readonly status: GeneratedVersionStatus;
  readonly previewUrl?: string | null;
  readonly metadata?: Readonly<Record<string, unknown>> | null;
  readonly createdByName?: string | null;
  readonly createdByProfileImageUrl?: string | null;
  readonly capabilities?: GeneratedVersionCapabilities | null;
  readonly downloadUrl?: string | null;
  readonly shareUrl?: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface GeneratedVersionActionResult {
  readonly ok: boolean;
  readonly message?: string;
  readonly fieldErrors: Readonly<Record<string, string>>;
}

export interface GeneratedVersionLink {
  readonly url: string;
  readonly expiresAt?: string | null;
}

export function generatedVersionStatusLabel(status: GeneratedVersionStatus | null | undefined): GeneratedVersionFriendlyStatus {
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

export function generatedVersionStatusTone(
  status: GeneratedVersionStatus | null | undefined,
): 'brand' | 'blue' | 'red' | 'neutral' {
  switch (generatedVersionStatusLabel(status)) {
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

export function generatedVersionMetadataValue(
  version: GeneratedVersion,
  key: string,
): string | number | boolean | null {
  const value = version.metadata?.[key];
  return typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean'
    ? value
    : null;
}
