export enum ActivityType {
  BrandCreated = 'BRAND_CREATED',
  ProductCreated = 'PRODUCT_CREATED',
  ProjectCreated = 'PROJECT_CREATED',
  AssetUploaded = 'ASSET_UPLOADED',
  CreativeRequestCreated = 'CREATIVE_REQUEST_CREATED',
  GenerationCompleted = 'GENERATION_COMPLETED',
  ApprovalAction = 'APPROVAL_ACTION',
  DownloadCompleted = 'DOWNLOAD_COMPLETED',
  ShareCreated = 'SHARE_CREATED',
  PaymentCompleted = 'PAYMENT_COMPLETED',
  SubscriptionChanged = 'SUBSCRIPTION_CHANGED',
  AiProviderSwitched = 'AI_PROVIDER_SWITCHED',
  RoutingPolicyChanged = 'ROUTING_POLICY_CHANGED',
  ProfileUpdated = 'PROFILE_UPDATED',
  ProfileImageUpdated = 'PROFILE_IMAGE_UPDATED',
  PasswordChanged = 'PASSWORD_CHANGED',
}

export interface ActivityFeed {
  readonly id: string;
  readonly workspaceId: string;
  readonly actorUserId: string | null;
  readonly actorName: string | null;
  readonly actorProfileImageUrl?: string | null;
  readonly activityType: ActivityType | string;
  readonly referenceType: string | null;
  readonly referenceId: string | null;
  readonly title: string;
  readonly description: string | null;
  readonly metadataJson: string | null;
  readonly createdAt: string;
}

export interface ActivityFilters {
  readonly activityType?: ActivityType | string | null;
  readonly actorUserId?: string | null;
  readonly from?: string | null;
  readonly to?: string | null;
  readonly page?: number | null;
  readonly size?: number | null;
}

export interface DateRangeFilter {
  readonly from: string | null;
  readonly to: string | null;
}

export interface ActivityActionResult {
  readonly ok: boolean;
  readonly message?: string;
}
