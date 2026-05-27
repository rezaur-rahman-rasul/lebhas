export enum NotificationType {
  CreativeRequestCreated = 'CREATIVE_REQUEST_CREATED',
  GenerationStarted = 'GENERATION_STARTED',
  GenerationCompleted = 'GENERATION_COMPLETED',
  GenerationFailed = 'GENERATION_FAILED',
  ApprovalRequested = 'APPROVAL_REQUESTED',
  ApprovalApproved = 'APPROVAL_APPROVED',
  ApprovalRejected = 'APPROVAL_REJECTED',
  DownloadCompleted = 'DOWNLOAD_COMPLETED',
  ShareLinkCreated = 'SHARE_LINK_CREATED',
  PaymentSucceeded = 'PAYMENT_SUCCEEDED',
  PaymentFailed = 'PAYMENT_FAILED',
  SubscriptionChanged = 'SUBSCRIPTION_CHANGED',
  CreditLow = 'CREDIT_LOW',
  StorageLimitExceeded = 'STORAGE_LIMIT_EXCEEDED',
  AiProviderFailed = 'AI_PROVIDER_FAILED',
  AiProviderRecovered = 'AI_PROVIDER_RECOVERED',
  SystemAlert = 'SYSTEM_ALERT',
  ProfileUpdated = 'PROFILE_UPDATED',
  ProfileImageUpdated = 'PROFILE_IMAGE_UPDATED',
  PasswordChanged = 'PASSWORD_CHANGED',
  SessionRevoked = 'SESSION_REVOKED',
  SecurityActivityDetected = 'SECURITY_ACTIVITY_DETECTED',
}

export enum NotificationPriority {
  Low = 'LOW',
  Normal = 'NORMAL',
  High = 'HIGH',
  Critical = 'CRITICAL',
}

export interface Notification {
  readonly id: string;
  readonly workspaceId: string;
  readonly recipientUserId: string;
  readonly notificationType: NotificationType | string;
  readonly title: string;
  readonly message: string;
  readonly referenceType: string | null;
  readonly referenceId: string | null;
  readonly priority: NotificationPriority | string;
  readonly isRead: boolean;
  readonly readAt: string | null;
  readonly createdAt: string;
}

export interface NotificationPreference {
  readonly id: string;
  readonly workspaceId: string;
  readonly userId: string;
  readonly notificationType: NotificationType | string;
  readonly inAppEnabled: boolean;
  readonly emailEnabled: boolean;
  readonly smsEnabled: boolean;
  readonly pushEnabled: boolean;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface NotificationPreferenceUpdateRequest {
  readonly notificationType: NotificationType | string;
  readonly inAppEnabled: boolean;
  readonly emailEnabled: boolean;
  readonly smsEnabled: boolean;
  readonly pushEnabled: boolean;
}

export interface NotificationFilters {
  readonly notificationType?: NotificationType | string | null;
  readonly priority?: NotificationPriority | string | null;
  readonly unreadOnly?: boolean | null;
  readonly referenceType?: string | null;
  readonly from?: string | null;
  readonly to?: string | null;
  readonly page?: number | null;
  readonly size?: number | null;
}

export interface NotificationActionResult {
  readonly ok: boolean;
  readonly message?: string;
}
