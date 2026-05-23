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
