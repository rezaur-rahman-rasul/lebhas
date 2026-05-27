export enum PreferredLanguage {
  Bangla = 'BANGLA',
  English = 'ENGLISH',
  Both = 'BOTH',
}

export enum ThemePreference {
  System = 'SYSTEM',
  Dark = 'DARK',
  Light = 'LIGHT',
}

export enum SecurityActivityType {
  LoginSuccess = 'LOGIN_SUCCESS',
  LoginFailed = 'LOGIN_FAILED',
  Logout = 'LOGOUT',
  PasswordChanged = 'PASSWORD_CHANGED',
  PasswordChangeFailed = 'PASSWORD_CHANGE_FAILED',
  ProfileUpdated = 'PROFILE_UPDATED',
  ProfileImageUpdated = 'PROFILE_IMAGE_UPDATED',
  ProfileImageRemoved = 'PROFILE_IMAGE_REMOVED',
  RefreshTokenRotated = 'REFRESH_TOKEN_ROTATED',
  SessionRevoked = 'SESSION_REVOKED',
}

export interface UserAccountSettings {
  readonly preferredLanguage: PreferredLanguage | string;
  readonly themePreference: ThemePreference | string;
  readonly notificationEmailEnabled: boolean;
  readonly notificationInAppEnabled: boolean;
  readonly marketingEmailEnabled: boolean;
}

export interface UserProfileView {
  readonly userId: string;
  readonly firstName: string;
  readonly lastName: string;
  readonly displayName: string;
  readonly email: string;
  readonly phoneNumber: string | null;
  readonly jobTitle: string | null;
  readonly profileImageUrl: string | null;
  readonly timezone: string | null;
  readonly locale: string | null;
  readonly settings: UserAccountSettings;
  readonly updatedAt: string;
}

export interface SupportUserProfileView extends UserProfileView {
  readonly role?: string | null;
  readonly workspaceId?: string | null;
  readonly workspaceName?: string | null;
  readonly emailVisible?: boolean | null;
  readonly phoneVisible?: boolean | null;
}

export interface UpdateProfileRequest {
  readonly firstName: string;
  readonly lastName: string;
  readonly displayName: string;
  readonly phoneNumber: string | null;
  readonly jobTitle: string | null;
  readonly timezone: string | null;
  readonly locale: string | null;
}

export interface UpdateAccountSettingsRequest {
  readonly preferredLanguage: PreferredLanguage | string;
  readonly themePreference: ThemePreference | string;
  readonly notificationEmailEnabled: boolean;
  readonly notificationInAppEnabled: boolean;
  readonly marketingEmailEnabled: boolean;
}

export interface ChangePasswordRequest {
  readonly currentPassword: string;
  readonly newPassword: string;
  readonly confirmPassword: string;
}

export interface ProfileImageUploadUrlRequest {
  readonly fileName: string;
  readonly contentType: string;
  readonly fileSizeBytes: number;
}

export interface ProfileImageUploadUrlResponse {
  readonly uploadUrl: string;
  readonly uploadSessionId: string;
  readonly expiresAt: string;
}

export interface ConfirmProfileImageUploadRequest {
  readonly uploadSessionId: string;
  readonly checksum?: string;
}

export interface SecurityActivityView {
  readonly activityType: SecurityActivityType | string;
  readonly ipAddressMasked: string | null;
  readonly deviceSummary: string | null;
  readonly success: boolean;
  readonly failureReason: string | null;
  readonly createdAt: string;
}

export interface UserSessionView {
  readonly sessionId: string;
  readonly deviceSummary: string | null;
  readonly ipAddressMasked: string | null;
  readonly current: boolean;
  readonly createdAt: string;
  readonly lastActiveAt: string | null;
}

export interface ProfileActionResult {
  readonly ok: boolean;
  readonly message?: string;
}
