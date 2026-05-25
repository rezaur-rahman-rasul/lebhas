export type GeneratedVersionStatus = string;

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
  readonly capabilities?: GeneratedVersionCapabilities | null;
  readonly createdAt: string;
  readonly updatedAt: string;
}
