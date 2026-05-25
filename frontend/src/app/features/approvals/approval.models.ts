export type ApprovalStatus = string;

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
  readonly capabilities?: ApprovalCapabilities | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ApprovalDecisionPayload {
  readonly decision: string;
  readonly note?: string | null;
}
