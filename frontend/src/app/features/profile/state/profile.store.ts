import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { CurrentUser } from '@app/features/auth/models/user.models';
import {
  ChangePasswordRequest,
  ConfirmProfileImageUploadRequest,
  PreferredLanguage,
  ProfileActionResult,
  ProfileImageUploadUrlRequest,
  ProfileImageUploadUrlResponse,
  SecurityActivityView,
  ThemePreference,
  UpdateAccountSettingsRequest,
  UpdateProfileRequest,
  UserAccountSettings,
  UserProfileView,
  UserSessionView,
} from '../models/profile.models';
import { ProfileApiService } from '../services/profile-api.service';
import { getInitials, safeProfileImageUrl } from '../services/profile-security';

@Injectable({ providedIn: 'root' })
export class ProfileStore {
  private readonly api = inject(ProfileApiService);
  private readonly auth = inject(CurrentUserStore);
  private readonly permissions = inject(PermissionStore);
  private readonly notifications = inject(NotificationStateService);

  private readonly profileSignal = signal<UserProfileView | null>(null);
  private readonly accountSettingsSignal = signal<UserAccountSettings | null>(null);
  private readonly securityActivitySignal = signal<readonly SecurityActivityView[]>([]);
  private readonly sessionsSignal = signal<readonly UserSessionView[]>([]);
  private readonly imageUploadProgressSignal = signal(0);
  private readonly loadingSignal = signal(false);
  private readonly savingSignal = signal(false);
  private readonly uploadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly uploadErrorSignal = signal<string | null>(null);
  private readonly uploadSessionSignal = signal<ProfileImageUploadUrlResponse | null>(null);

  readonly profile = this.profileSignal.asReadonly();
  readonly accountSettings = this.accountSettingsSignal.asReadonly();
  readonly securityActivity = this.securityActivitySignal.asReadonly();
  readonly sessions = this.sessionsSignal.asReadonly();
  readonly imageUploadProgress = this.imageUploadProgressSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly saving = this.savingSignal.asReadonly();
  readonly uploading = this.uploadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly uploadError = this.uploadErrorSignal.asReadonly();

  readonly displayName = computed(() => {
    const profile = this.profileSignal();
    if (profile?.displayName) {
      return profile.displayName;
    }

    const fullName = [profile?.firstName, profile?.lastName].filter(Boolean).join(' ');
    return fullName || this.auth.displayName();
  });
  readonly initials = computed(() => getInitials(this.profileSignal()));
  readonly savedProfileImageUrl = computed(() => safeProfileImageUrl(this.profileSignal()));
  readonly profileImageUrl = this.savedProfileImageUrl;
  readonly preferredLanguageLabel = computed(() =>
    preferredLanguageLabel(this.accountSettingsSignal()?.preferredLanguage),
  );
  readonly themePreferenceLabel = computed(() =>
    themePreferenceLabel(this.accountSettingsSignal()?.themePreference),
  );
  readonly hasProfileImage = computed(() => Boolean(this.profileImageUrl()));
  readonly recentSecurityActivity = computed(() => this.securityActivitySignal().slice(0, 8));
  readonly currentSession = computed(
    () => this.sessionsSignal().find((session) => session.current) ?? null,
  );
  readonly otherSessions = computed(() =>
    this.sessionsSignal().filter((session) => !session.current),
  );

  async loadMyProfile(): Promise<ProfileActionResult> {
    if (!this.permissions.canViewOwnProfile()) {
      return this.restricted('You do not have access to your profile.');
    }

    return this.runLoading(async () => {
      this.setProfile(await this.api.getMyProfile());
    });
  }

  async updateMyProfile(
    payload: UpdateProfileRequest,
    profileImageFile: File | null = null,
  ): Promise<ProfileActionResult> {
    if (!this.permissions.canEditOwnProfile()) {
      return this.restricted('You do not have permission to update your profile.');
    }

    return this.runSaving(async () => {
      const profile = profileImageFile
        ? await this.api.updateMyProfileWithImage(payload, profileImageFile)
        : await this.api.updateMyProfile(payload);
      this.setProfile(profile);
      if (profileImageFile) {
        this.uploadSessionSignal.set(null);
        this.imageUploadProgressSignal.set(100);
      }
    });
  }

  async updateMySettings(payload: UpdateAccountSettingsRequest): Promise<ProfileActionResult> {
    if (!this.permissions.canEditOwnProfile()) {
      return this.restricted('You do not have permission to update your settings.');
    }

    return this.runSaving(async () => {
      this.accountSettingsSignal.set(await this.api.updateMySettings(payload));
    });
  }

  async changePassword(payload: ChangePasswordRequest): Promise<ProfileActionResult> {
    if (!this.permissions.canChangeOwnPassword()) {
      return this.restricted('You do not have permission to change your password.');
    }

    return this.runSaving(() => this.api.changePassword(payload), {
      successMessage: 'Your password was updated.',
    });
  }

  async requestProfileImageUploadUrl(
    payload: ProfileImageUploadUrlRequest,
  ): Promise<ProfileActionResult> {
    if (!this.permissions.canManageProfileImage()) {
      return this.restricted('You do not have permission to update your profile image.');
    }

    return this.runUploading(async () => {
      this.uploadSessionSignal.set(await this.api.requestProfileImageUploadUrl(payload));
      this.imageUploadProgressSignal.set(0);
    });
  }

  async uploadProfileImage(file: File): Promise<ProfileActionResult> {
    if (!this.permissions.canManageProfileImage()) {
      return this.restricted('You do not have permission to update your profile image.');
    }
    this.uploadErrorSignal.set(null);
    this.imageUploadProgressSignal.set(0);
    return { ok: true };
  }

  async confirmProfileImageUpload(
    payload: ConfirmProfileImageUploadRequest,
  ): Promise<ProfileActionResult> {
    if (!this.permissions.canManageProfileImage()) {
      return this.restricted('You do not have permission to update your profile image.');
    }

    return this.runUploading(async () => {
      this.setProfile(await this.api.confirmProfileImageUpload(payload));
      this.uploadSessionSignal.set(null);
      this.imageUploadProgressSignal.set(100);
    });
  }

  async removeProfileImage(): Promise<ProfileActionResult> {
    if (!this.permissions.canManageProfileImage()) {
      return this.restricted('You do not have permission to remove your profile image.');
    }

    return this.runSaving(async () => {
      this.setProfile(await this.api.removeProfileImage());
      this.uploadSessionSignal.set(null);
      this.imageUploadProgressSignal.set(0);
      this.notifications.success('Profile', 'Profile photo removed.');
    });
  }

  async loadSecurityActivity(): Promise<ProfileActionResult> {
    if (!this.permissions.canViewSecurityActivity()) {
      return this.restricted('You do not have access to security activity.');
    }

    return this.runLoading(async () => {
      this.securityActivitySignal.set(await this.api.getSecurityActivity());
    });
  }

  async loadSessions(): Promise<ProfileActionResult> {
    if (!this.permissions.canViewSecurityActivity()) {
      return this.restricted('You do not have access to active sessions.');
    }

    return this.runLoading(async () => {
      this.sessionsSignal.set(await this.api.getSessions());
    });
  }

  async revokeSession(sessionId: string): Promise<ProfileActionResult> {
    if (!this.permissions.canViewSecurityActivity()) {
      return this.restricted('You do not have permission to revoke sessions.');
    }

    return this.runSaving(async () => {
      await this.api.revokeSession(sessionId);
      this.sessionsSignal.update((sessions) =>
        sessions.filter((session) => session.sessionId !== sessionId),
      );
    });
  }

  async loadMasterUserProfile(userId: string): Promise<ProfileActionResult> {
    if (!this.permissions.canViewUserProfileSupport()) {
      return this.restricted('You do not have access to user profile support details.');
    }

    return this.runLoading(async () => {
      this.setProfile(await this.api.getMasterUserProfile(userId), { syncAuth: false });
    });
  }

  async loadMasterUserSecurityActivity(userId: string): Promise<ProfileActionResult> {
    if (!this.permissions.canViewUserProfileSupport()) {
      return this.restricted('You do not have access to user security activity.');
    }

    return this.runLoading(async () => {
      this.securityActivitySignal.set(await this.api.getMasterUserSecurityActivity(userId));
    });
  }

  clearError(): void {
    this.errorSignal.set(null);
    this.uploadErrorSignal.set(null);
  }

  resetUploadProgress(): void {
    this.imageUploadProgressSignal.set(0);
    this.uploadSessionSignal.set(null);
  }

  discardStagedProfileImagePreview(): void {
    this.imageUploadProgressSignal.set(0);
    this.uploadErrorSignal.set(null);
  }

  reset(): void {
    this.profileSignal.set(null);
    this.accountSettingsSignal.set(null);
    this.securityActivitySignal.set([]);
    this.sessionsSignal.set([]);
    this.imageUploadProgressSignal.set(0);
    this.loadingSignal.set(false);
    this.savingSignal.set(false);
    this.uploadingSignal.set(false);
    this.errorSignal.set(null);
    this.uploadErrorSignal.set(null);
    this.uploadSessionSignal.set(null);
  }

  private setProfile(
    profile: UserProfileView,
    options: { readonly syncAuth?: boolean } = {},
  ): void {
    this.profileSignal.set(profile);
    this.accountSettingsSignal.set(profile.settings);

    if (options.syncAuth === false) {
      return;
    }

    this.syncCurrentUser(profile);
  }

  private syncCurrentUser(profile: UserProfileView): void {
    const current = this.auth.currentUser();
    if (!current || current.id !== profile.userId) {
      return;
    }

    const fullName = [profile.firstName, profile.lastName].filter(Boolean).join(' ');
    const patched: CurrentUser = {
      ...current,
      firstName: profile.firstName,
      lastName: profile.lastName,
      name: profile.displayName || fullName || current.name,
      fullName: profile.displayName || fullName || current.fullName,
      profileImageUrl: profile.profileImageUrl ?? null,
      phone: profile.phoneNumber,
      updatedAt: profile.updatedAt,
    };

    this.auth.patchCurrentUser(patched);
  }

  private async runLoading(action: () => Promise<void>): Promise<ProfileActionResult> {
    return this.run(action, this.loadingSignal);
  }

  private async runSaving(
    action: () => Promise<void>,
    options?: { readonly successMessage?: string },
  ): Promise<ProfileActionResult> {
    return this.run(action, this.savingSignal, options);
  }

  private async runUploading(action: () => Promise<void>): Promise<ProfileActionResult> {
    return this.run(action, this.uploadingSignal, undefined, {
      errorTarget: this.uploadErrorSignal,
      errorMapper: friendlyProfileUploadError,
      notifyOnError: false,
    });
  }

  private async run(
    action: () => Promise<void>,
    stateSignal: { set(value: boolean): void },
    options?: { readonly successMessage?: string },
    errorOptions: {
      readonly errorTarget?: { set(value: string | null): void };
      readonly errorMapper?: (error: unknown) => string;
      readonly notifyOnError?: boolean;
    } = {},
  ): Promise<ProfileActionResult> {
    const errorTarget = errorOptions.errorTarget ?? this.errorSignal;
    const errorMapper = errorOptions.errorMapper ?? friendlyProfileError;
    stateSignal.set(true);
    errorTarget.set(null);

    try {
      await action();
      if (options?.successMessage) {
        this.notifications.success('Profile', options.successMessage);
      }
      return { ok: true };
    } catch (error) {
      const message = errorMapper(error);
      errorTarget.set(message);
      if (errorOptions.notifyOnError !== false) {
        this.notifications.error('Profile', message);
      }
      return { ok: false, message };
    } finally {
      stateSignal.set(false);
    }
  }

  private restricted(message: string): ProfileActionResult {
    this.errorSignal.set(message);
    return { ok: false, message };
  }
}

function preferredLanguageLabel(value: PreferredLanguage | string | null | undefined): string {
  switch (value) {
    case PreferredLanguage.Bangla:
      return 'Bangla';
    case PreferredLanguage.English:
      return 'English';
    case PreferredLanguage.Both:
      return 'Bangla and English';
    default:
      return 'Not set';
  }
}

function themePreferenceLabel(value: ThemePreference | string | null | undefined): string {
  switch (value) {
    case ThemePreference.System:
      return 'Use system setting';
    case ThemePreference.Dark:
      return 'Dark';
    case ThemePreference.Light:
      return 'Light';
    default:
      return 'Not set';
  }
}

function friendlyProfileError(error: unknown): string {
  return normalizeHttpError(error).message || 'We could not update profile information. Please try again.';
}

function friendlyProfileUploadError(error: unknown): string {
  const normalized = normalizeHttpError(error);
  const technicalMessage = normalized.message.toLowerCase();
  const genericLoadFailure = technicalMessage.includes('we could not load this data');

  if (normalized.status === 0) {
    return 'Profile photo upload could not reach storage. Check the backend and storage CORS, then retry.';
  }

  if (
    normalized.status >= 500 ||
    genericLoadFailure ||
    technicalMessage.includes('internal server error') ||
    technicalMessage.includes('unexpected server error')
  ) {
    return 'Profile photo upload failed on the server. Check backend storage/R2 configuration, then retry.';
  }

  if (genericLoadFailure) {
    return 'Profile photo upload failed. Check backend storage/R2 configuration, then retry.';
  }

  return normalized.message || 'Profile photo could not be uploaded. Please try again.';
}
