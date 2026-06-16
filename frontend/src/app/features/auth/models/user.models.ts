export type UserRole = 'MASTER' | 'ADMIN' | 'CREW';

export type Permission =
  | 'USER_VIEW'
  | 'USER_CREATE'
  | 'USER_UPDATE'
  | 'USER_STATUS_UPDATE'
  | 'WORKSPACE_CREATE'
  | 'CREW_INVITE'
  | 'CREW_VIEW'
  | 'CREW_UPDATE'
  | 'CREW_REMOVE'
  | 'WORKSPACE_VIEW'
  | 'WORKSPACE_UPDATE'
  | 'WORKSPACE_STATUS_UPDATE'
  | 'WORKSPACE_SETTINGS_VIEW'
  | 'WORKSPACE_SETTINGS_UPDATE'
  | 'BRAND_VIEW'
  | 'BRAND_MANAGE'
  | 'BRAND_PROFILE_UPDATE'
  | 'PRODUCT_VIEW'
  | 'PRODUCT_MANAGE'
  | 'PRODUCT_SERVICE_MANAGE'
  | 'PROJECT_VIEW'
  | 'PROJECT_CREATE'
  | 'PROJECT_UPDATE'
  | 'PROJECT_CAMPAIGN_MANAGE'
  | 'ASSET_VIEW'
  | 'ASSET_UPLOAD'
  | 'ASSET_UPDATE'
  | 'ASSET_DELETE'
  | 'ASSET_FOLDER_MANAGE'
  | 'PROMPT_INTELLIGENCE_USE'
  | 'PROMPT_TEMPLATE_VIEW'
  | 'PROMPT_TEMPLATE_MANAGE'
  | 'PROMPT_HISTORY_VIEW'
  | 'AI_MONITORING_VIEW'
  | 'AI_PROVIDER_METRICS_VIEW'
  | 'AI_LAYER_ANALYTICS_VIEW'
  | 'AI_WORKSPACE_USAGE_VIEW'
  | 'AI_QUALITY_SCORE_VIEW'
  | 'AI_FAILURE_VIEW'
  | 'USAGE_BILLING_VIEW'
  | 'CREDIT_LEDGER_VIEW'
  | 'DOWNLOAD_USAGE_VIEW'
  | 'SHARE_USAGE_VIEW'
  | 'MASTER_USAGE_VIEW'
  | 'PLAN_UTILIZATION_VIEW'
  | 'PAYMENT_PROVIDER_MANAGE'
  | 'CREDIT_PACKAGE_MANAGE'
  | 'SUBSCRIPTION_PURCHASE'
  | 'CREDIT_PURCHASE'
  | 'PAYMENT_VIEW'
  | 'INVOICE_VIEW'
  | 'NOTIFICATION_VIEW'
  | 'NOTIFICATION_PREFERENCE_MANAGE'
  | 'ACTIVITY_FEED_VIEW'
  | 'AUDIT_LOG_VIEW'
  | 'MASTER_MONITORING_VIEW'
  | 'SYSTEM_HEALTH_VIEW'
  | 'MONITORING_ALERT_VIEW'
  | 'CREATIVE_GENERATE'
  | 'CREATIVE_EDIT'
  | 'CREATIVE_DOWNLOAD'
  | 'CREATIVE_SUBMIT'
  | 'SESSION_MANAGE';

export type UserStatus = 'ACTIVE' | 'INVITED' | 'SUSPENDED' | 'DISABLED';

export interface WorkspaceContext {
  readonly id: string | null;
  readonly name: string | null;
}

export interface CurrentUserResponse {
  readonly id: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly email: string;
  readonly phone: string | null;
  readonly role: UserRole;
  readonly status: UserStatus;
  readonly emailVerified: boolean;
  readonly lastLoginAt: string | null;
  readonly workspaceId: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly permissions: readonly Permission[];
}

export interface CurrentUser {
  readonly id: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly profileImageUrl?: string | null;
  readonly name: string;
  readonly fullName: string;
  readonly email: string;
  readonly phone: string | null;
  readonly role: UserRole;
  readonly status: UserStatus;
  readonly emailVerified: boolean;
  readonly lastLoginAt: string | null;
  readonly createdAt: string;
  readonly updatedAt: string;
  readonly permissions: readonly Permission[];
  readonly workspaceId: string | null;
  readonly workspaceName: string | null;
  readonly workspace: WorkspaceContext;
}
