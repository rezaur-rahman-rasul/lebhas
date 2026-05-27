export type ApprovalStatus = string;
export type ApprovalDecision = 'APPROVE' | 'REJECT' | 'REQUEST_CHANGES';
export type ApprovalFriendlyStatus = 'Queued' | 'In review' | 'Approved' | 'Changes requested' | 'Rejected';

export interface ApprovalCapabilities {
  readonly canApprove?: boolean | null;
  readonly canReject?: boolean | null;
  readonly canShare?: boolean | null;
  readonly canDownload?: boolean | null;
}

export interface ApprovalItem {
  readonly id: string;
  readonly workspaceId: string;
  readonly creativeRequestId: string;
  readonly generatedVersionId: string;
  readonly status: ApprovalStatus;
  readonly title?: string | null;
  readonly previewUrl?: string | null;
  readonly shareUrl?: string | null;
  readonly shareExpiresAt?: string | null;
  readonly downloadUrl?: string | null;
  readonly submittedBy?: string | null;
  readonly submittedByName?: string | null;
  readonly submittedByProfileImageUrl?: string | null;
  readonly history?: readonly ApprovalTimelineItem[] | null;
  readonly capabilities?: ApprovalCapabilities | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ApprovalDecisionPayload {
  readonly decision: ApprovalDecision;
  readonly note?: string | null;
}

export interface ApprovalTimelineItem {
  readonly id?: string | null;
  readonly status: ApprovalStatus;
  readonly note?: string | null;
  readonly actorName?: string | null;
  readonly actorProfileImageUrl?: string | null;
  readonly createdAt: string;
}

export interface ApprovalActionResult {
  readonly ok: boolean;
  readonly message?: string;
  readonly fieldErrors: Readonly<Record<string, string>>;
}

export function approvalStatusLabel(status: ApprovalStatus | null | undefined): ApprovalFriendlyStatus {
  const normalized = status?.trim().toUpperCase().replace(/[\s-]+/g, '_');
  switch (normalized) {
    case 'APPROVED':
    case 'ACCEPTED':
      return 'Approved';
    case 'REJECTED':
    case 'DECLINED':
      return 'Rejected';
    case 'CHANGES_REQUESTED':
    case 'REQUEST_CHANGES':
    case 'REVISION_REQUESTED':
      return 'Changes requested';
    case 'IN_REVIEW':
    case 'REVIEWING':
      return 'In review';
    default:
      return 'Queued';
  }
}

export function approvalStatusTone(
  status: ApprovalStatus | null | undefined,
): 'brand' | 'blue' | 'red' | 'neutral' {
  switch (approvalStatusLabel(status)) {
    case 'Approved':
      return 'brand';
    case 'In review':
    case 'Changes requested':
      return 'blue';
    case 'Rejected':
      return 'red';
    default:
      return 'neutral';
  }
}
