import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { maskSensitiveMetadata } from '@app/features/audit/services/sensitive-metadata';
import { NotificationType } from '@app/features/notifications/models/notification.models';
import { friendlyNotificationType } from '@app/features/notifications/components/notification-type-icon/notification-type-icon';
import {
  isAllowedProfileImageSize,
  isAllowedProfileImageType,
} from './services/profile-security';
import {
  PreferredLanguage,
  SecurityActivityType,
  SupportUserProfileView,
  ThemePreference,
  UserProfileView,
  UserSessionView,
} from './models/profile.models';
import { ProfileApiService } from './services/profile-api.service';
import { ProfileStore } from './state/profile.store';

const appRoot = join(process.cwd(), 'src/app');
const profileRoot = join(appRoot, 'features/profile');

function read(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8');
}

function collectFiles(
  directory: string,
  predicate: (filePath: string) => boolean,
  files: string[] = [],
): string[] {
  for (const entry of readdirSync(directory)) {
    const fullPath = join(directory, entry);
    const stats = statSync(fullPath);

    if (stats.isDirectory()) {
      collectFiles(fullPath, predicate, files);
      continue;
    }

    if (predicate(fullPath)) {
      files.push(fullPath);
    }
  }

  return files;
}

function relativeProfilePath(filePath: string): string {
  return relative(profileRoot, filePath).replaceAll('\\', '/');
}

const profile: UserProfileView = {
  userId: 'user-1',
  firstName: 'Reza',
  lastName: 'Rasul',
  displayName: 'Reza Rasul',
  email: 'reza@example.test',
  phoneNumber: '+8801000000000',
  jobTitle: 'Owner',
  profileImageUrl: null,
  timezone: 'Asia/Dhaka',
  locale: 'en-US',
  settings: {
    preferredLanguage: PreferredLanguage.English,
    themePreference: ThemePreference.System,
    notificationEmailEnabled: true,
    notificationInAppEnabled: true,
    marketingEmailEnabled: false,
  },
  updatedAt: '2026-05-27T08:00:00Z',
};

const profileWithImage: UserProfileView = {
  ...profile,
  profileImageUrl: 'https://cdn.example.test/profile/user-1.webp',
};

const securityActivity = [
  {
    activityType: SecurityActivityType.ProfileUpdated,
    ipAddressMasked: '203.0.*.*',
    deviceSummary: 'Chrome on desktop device',
    success: true,
    failureReason: null,
    createdAt: '2026-05-27T08:15:00Z',
  },
  {
    activityType: SecurityActivityType.PasswordChanged,
    ipAddressMasked: '203.0.*.*',
    deviceSummary: 'Chrome on desktop device',
    success: true,
    failureReason: null,
    createdAt: '2026-05-27T08:20:00Z',
  },
];

const sessions: readonly UserSessionView[] = [
  {
    sessionId: 'session-current',
    deviceSummary: 'Chrome on desktop device',
    ipAddressMasked: '203.0.*.*',
    current: true,
    createdAt: '2026-05-27T08:00:00Z',
    lastActiveAt: '2026-05-27T08:30:00Z',
  },
  {
    sessionId: 'session-other',
    deviceSummary: 'Safari on mobile device',
    ipAddressMasked: '203.0.*.*',
    current: false,
    createdAt: '2026-05-26T08:00:00Z',
    lastActiveAt: '2026-05-26T08:30:00Z',
  },
];

function configureProfileStore(options: { readonly allowed?: boolean } = {}) {
  const allowed = signal(options.allowed ?? true).asReadonly();
  const currentUser = signal({
    id: 'user-1',
    firstName: 'Auth',
    lastName: 'User',
    name: 'Auth User',
    fullName: 'Auth User',
    email: 'reza@example.test',
    phone: null,
    role: 'CREW',
    permissions: [],
    workspace: { id: 'workspace-1' },
    workspaceName: 'Lebhas Workspace',
    updatedAt: '2026-05-27T07:00:00Z',
  });
  const patchCurrentUser = vi.fn((value) => currentUser.set(value));
  const api = {
    getMyProfile: vi.fn().mockResolvedValue(profile),
    updateMyProfile: vi.fn().mockResolvedValue({ ...profile, displayName: 'Updated Profile' }),
    updateMySettings: vi.fn().mockResolvedValue({
      ...profile.settings,
      preferredLanguage: PreferredLanguage.Both,
      themePreference: ThemePreference.Dark,
    }),
    changePassword: vi.fn().mockResolvedValue(undefined),
    requestProfileImageUploadUrl: vi.fn().mockResolvedValue({
      uploadUrl: 'https://upload.example.test/signed',
      uploadSessionId: 'upload-session-1',
      expiresAt: '2026-05-27T09:00:00Z',
    }),
    uploadProfileImageToSignedUrl: vi.fn().mockImplementation(
      (_url: string, _file: File, onProgress: (progress: number) => void) => {
        onProgress(55);
        return Promise.resolve();
      },
    ),
    confirmProfileImageUpload: vi.fn().mockResolvedValue(profileWithImage),
    removeProfileImage: vi.fn().mockResolvedValue(profile),
    getSecurityActivity: vi.fn().mockResolvedValue(securityActivity),
    getSessions: vi.fn().mockResolvedValue(sessions),
    revokeSession: vi.fn().mockResolvedValue(undefined),
    getMasterUserProfile: vi.fn().mockResolvedValue({
      ...profileWithImage,
      role: 'ADMIN',
      workspaceId: 'workspace-1',
      workspaceName: 'Lebhas Workspace',
      emailVisible: true,
      phoneVisible: true,
    } satisfies SupportUserProfileView),
    getMasterUserSecurityActivity: vi.fn().mockResolvedValue(securityActivity),
  };

  TestBed.configureTestingModule({
    providers: [
      ProfileStore,
      { provide: ProfileApiService, useValue: api },
      {
        provide: PermissionStore,
        useValue: {
          canViewOwnProfile: allowed,
          canEditOwnProfile: allowed,
          canChangeOwnPassword: allowed,
          canManageProfileImage: allowed,
          canViewSecurityActivity: allowed,
          canViewUserProfileSupport: allowed,
        },
      },
      {
        provide: CurrentUserStore,
        useValue: {
          currentUser: currentUser.asReadonly(),
          displayName: signal('Auth User').asReadonly(),
          patchCurrentUser,
        },
      },
      {
        provide: NotificationStateService,
        useValue: {
          success: vi.fn(),
          error: vi.fn(),
        },
      },
    ],
  });

  return {
    store: TestBed.inject(ProfileStore),
    api,
    patchCurrentUser,
  };
}

describe('Profile Management Batch 9 store and API behavior', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads the current user profile and renders display identity state', async () => {
    const { store, api } = configureProfileStore();

    await store.loadMyProfile();

    expect(api.getMyProfile).toHaveBeenCalled();
    expect(store.profile()?.email).toBe('reza@example.test');
    expect(store.displayName()).toBe('Reza Rasul');
    expect(store.initials()).toBe('RR');
  });

  it('lets CREW manage their own profile through existing own-profile permissions', async () => {
    const { store, api } = configureProfileStore();

    const result = await store.updateMyProfile({
      firstName: 'Crew',
      lastName: 'Member',
      displayName: 'Crew Member',
      phoneNumber: null,
      jobTitle: 'Designer',
      timezone: 'Asia/Dhaka',
      locale: 'en-US',
    });

    expect(result.ok).toBe(true);
    expect(api.updateMyProfile).toHaveBeenCalled();
  });

  it('submits profile updates and patches topbar auth display state', async () => {
    const { store, patchCurrentUser } = configureProfileStore();

    await store.updateMyProfile({
      firstName: 'Updated',
      lastName: 'Profile',
      displayName: 'Updated Profile',
      phoneNumber: null,
      jobTitle: null,
      timezone: 'Asia/Dhaka',
      locale: 'en-US',
    });

    expect(store.displayName()).toBe('Updated Profile');
    expect(patchCurrentUser).toHaveBeenCalledWith(
      expect.objectContaining({
        name: 'Updated Profile',
        fullName: 'Updated Profile',
      }),
    );
  });

  it('saves preferred language and theme preference through the profile API', async () => {
    const { store, api } = configureProfileStore();

    await store.updateMySettings({
      preferredLanguage: PreferredLanguage.Both,
      themePreference: ThemePreference.Dark,
      notificationEmailEnabled: true,
      notificationInAppEnabled: true,
      marketingEmailEnabled: false,
    });

    expect(api.updateMySettings).toHaveBeenCalledWith(
      expect.objectContaining({
        preferredLanguage: PreferredLanguage.Both,
        themePreference: ThemePreference.Dark,
      }),
    );
    expect(store.preferredLanguageLabel()).toBe('Bangla and English');
    expect(store.themePreferenceLabel()).toBe('Dark');
  });

  it('calls the change password API without persisting password values', async () => {
    const { store, api } = configureProfileStore();

    await store.changePassword({
      currentPassword: 'OldPassword123!',
      newPassword: 'NewPassword123!',
      confirmPassword: 'NewPassword123!',
    });

    expect(api.changePassword).toHaveBeenCalledWith({
      currentPassword: 'OldPassword123!',
      newPassword: 'NewPassword123!',
      confirmPassword: 'NewPassword123!',
    });
    expect(JSON.stringify(store.profile())).not.toContain('NewPassword123!');
  });

  it('uploads profile images through signed URL flow and updates avatar state', async () => {
    const { store, api } = configureProfileStore();
    const file = new File(['profile'], 'profile.webp', { type: 'image/webp' });

    await store.uploadProfileImage(file);

    expect(api.requestProfileImageUploadUrl).toHaveBeenCalledWith({
      fileName: 'profile.webp',
      contentType: 'image/webp',
      fileSizeBytes: file.size,
    });
    expect(api.uploadProfileImageToSignedUrl).toHaveBeenCalled();
    expect(api.confirmProfileImageUpload).toHaveBeenCalledWith({
      uploadSessionId: 'upload-session-1',
    });
    expect(store.imageUploadProgress()).toBe(100);
    expect(store.profileImageUrl()).toBe('https://cdn.example.test/profile/user-1.webp');
  });

  it('removes profile images and returns to fallback initials', async () => {
    const { store } = configureProfileStore();

    await store.confirmProfileImageUpload({ uploadSessionId: 'upload-session-1' });
    expect(store.hasProfileImage()).toBe(true);

    await store.removeProfileImage();

    expect(store.hasProfileImage()).toBe(false);
    expect(store.initials()).toBe('RR');
  });

  it('loads security activity, sessions, and revokes non-current sessions', async () => {
    const { store, api } = configureProfileStore();

    await store.loadSecurityActivity();
    await store.loadSessions();
    await store.revokeSession('session-other');

    expect(store.recentSecurityActivity()[0]?.activityType).toBe(SecurityActivityType.ProfileUpdated);
    expect(store.currentSession()?.sessionId).toBe('session-current');
    expect(store.otherSessions()).toEqual([]);
    expect(api.revokeSession).toHaveBeenCalledWith('session-other');
  });

  it('blocks another-user support profile access for unauthorized users', async () => {
    const { store, api } = configureProfileStore({ allowed: false });

    const result = await store.loadMasterUserProfile('other-user');

    expect(result.ok).toBe(false);
    expect(api.getMasterUserProfile).not.toHaveBeenCalled();
    expect(store.error()).toBe('You do not have access to user profile support details.');
  });
});

describe('Profile Management Batch 9 UI and source coverage', () => {
  it('protects profile routes under the authenticated dashboard shell and registers all profile routes', () => {
    const appRoutes = read('src/app/app.routes.ts');
    const dashboardRoutes = read('src/app/features/dashboard/dashboard.routes.ts');
    const profileRoutes = read('src/app/features/profile/profile.routes.ts');

    expect(appRoutes).toContain('canActivate: [authGuard]');
    expect(dashboardRoutes).toContain('...PROFILE_ROUTES');
    for (const route of [
      "path: 'profile'",
      "path: 'profile/edit'",
      "path: 'profile/security'",
      "path: 'profile/sessions'",
      "path: 'profile/settings'",
    ]) {
      expect(profileRoutes).toContain(route);
    }
    expect(profileRoutes).not.toContain(':userId');
  });

  it('renders profile page, summary identity, fallback initials, and profile image support', () => {
    const page = read('src/app/features/profile/page/profile-page.html');
    const summary = read('src/app/features/profile/components/profile-summary-card/profile-summary-card.html');
    const avatar = read('src/app/features/profile/components/profile-avatar/profile-avatar.html');
    const avatarSource = read('src/app/features/profile/components/profile-avatar/profile-avatar.ts');

    expect(page).toContain('app-profile-summary-card');
    expect(summary).toContain('fullName()');
    expect(summary).toContain('profile().email');
    expect(avatar).toContain('safeImageUrl()');
    expect(avatar).toContain('fallbackInitials()');
    expect(avatarSource).toContain('safeProfileImageUrl');
    expect(avatarSource).toContain("getInitials(this.profile()) || 'U'");
  });

  it('topbar My Profile menu navigates to /profile and updates from profile store', () => {
    const topbar = read('src/app/core/layout/user-profile-dropdown/user-profile-dropdown.html');
    const source = read('src/app/core/layout/user-profile-dropdown/user-profile-dropdown.ts');

    expect(topbar).toContain('data-testid="user-menu-profile"');
    expect(topbar).toContain('My Profile');
    expect(source).toContain("await this.navigateTo('/profile')");
    expect(source).toContain('this.profileStore.displayName()');
    expect(source).toContain('this.profileStore.profileImageUrl');
  });

  it('validates firstName and submits profile update from the edit profile form', () => {
    const source = read('src/app/features/profile/edit-profile/edit-profile-form.ts');
    const template = read('src/app/features/profile/edit-profile/edit-profile-form.html');

    expect(source).toContain("firstName: ['', [Validators.required");
    expect(template).toContain('First name is required.');
    expect(source).toContain('this.store.updateMyProfile(payload)');
    expect(template).toContain('formControlName="firstName"');
  });

  it('validates password confirmation, renders strength meter, calls API, and clears fields after success', () => {
    const source = read('src/app/features/profile/change-password/change-password-form.ts');
    const template = read('src/app/features/profile/change-password/change-password-form.html');
    const meter = read('src/app/features/profile/components/password-strength-meter/password-strength-meter.html');

    expect(source).toContain("matchingFieldsValidator('newPassword', 'confirmPassword')");
    expect(template).toContain('app-password-strength-meter');
    expect(meter).toContain('label()');
    expect(read('src/app/features/profile/components/password-strength-meter/password-strength-meter.ts')).toContain(
      "return 'Strong'",
    );
    expect(read('src/app/features/profile/components/password-strength-meter/password-strength-meter.ts')).toContain(
      "return 'Weak'",
    );
    expect(source).toContain('this.store.changePassword(this.form.getRawValue())');
    expect(source).toContain('clearPasswordFormState(this.form)');
  });

  it('renders account settings fields for preferred language and theme preference', () => {
    const source = read('src/app/features/profile/settings/account-settings.ts');
    const template = read('src/app/features/profile/settings/account-settings.html');

    expect(template).toContain('formControlName="preferredLanguage"');
    expect(template).toContain('formControlName="themePreference"');
    expect(source).toContain('this.store.updateMySettings(payload)');
    expect(source).toContain("this.theme.setTheme('dark')");
    expect(source).toContain("this.theme.setTheme('light')");
  });

  it('validates image type and size, displays progress, and never shows signed upload URLs', () => {
    const source = read('src/app/features/profile/components/profile-image-uploader/profile-image-uploader.ts');
    const template = read('src/app/features/profile/components/profile-image-uploader/profile-image-uploader.html');
    const validFile = new File(['image'], 'profile.png', { type: 'image/png' });
    const invalidFile = new File(['image'], 'profile.gif', { type: 'image/gif' });
    const largeFile = { type: 'image/png', size: 6 * 1024 * 1024 } as File;

    expect(isAllowedProfileImageType(validFile)).toBe(true);
    expect(isAllowedProfileImageType(invalidFile)).toBe(false);
    expect(isAllowedProfileImageSize(largeFile)).toBe(false);
    expect(source).toContain('Please upload JPG, PNG, or WEBP image.');
    expect(source).toContain('Image is too large. Please choose a smaller file.');
    expect(template).toContain('Profile photo upload progress');
    expect(template).toContain('progressValue()');
    expect(template).not.toContain('uploadUrl');
  });

  it('renders security activity, sessions, revoke confirm dialog, and calls revoke through store', () => {
    const security = read('src/app/features/profile/security/security-activity.html');
    const sessionsPage = read('src/app/features/profile/sessions/user-sessions.html');
    const sessionsSource = read('src/app/features/profile/sessions/user-sessions.ts');

    expect(security).toContain('app-security-activity-card');
    expect(sessionsPage).toContain('app-session-card');
    expect(sessionsPage).toContain('app-confirm-dialog');
    expect(sessionsPage).toContain('Revoke Session');
    expect(sessionsSource).toContain('this.store.revokeSession(session.sessionId)');
  });

  it('renders profile notification types and masks profile-sensitive audit metadata', () => {
    expect(friendlyNotificationType(NotificationType.ProfileUpdated)).toBe('Profile updated.');
    expect(friendlyNotificationType(NotificationType.PasswordChanged)).toBe('Password changed.');

    const masked = maskSensitiveMetadata(
      JSON.stringify({
        action: 'PROFILE_IMAGE_UPDATED',
        objectKey: 'profiles/raw-object-key.webp',
        password: 'NeverShowThis',
        safeLabel: 'Profile updated',
      }),
    );

    expect(masked).toContain('"objectKey":"****"');
    expect(masked).toContain('"password":"****"');
    expect(masked).toContain('Profile updated');
    expect(masked).not.toContain('raw-object-key');
    expect(masked).not.toContain('NeverShowThis');
  });

  it('keeps Master support profile view read-only and guarded when implemented', () => {
    const source = read('src/app/features/profile/components/support-profile-view/support-profile-view.ts');
    const template = read('src/app/features/profile/components/support-profile-view/support-profile-view.html');

    expect(source).toContain('canViewUserProfileSupport');
    expect(template).toContain('You do not have permission to view this user profile.');
    expect(template).toContain('Read-only support profile');
    expect(template).toContain('Security activity summary');
    expect(source).toContain('getMasterUserProfile');
    expect(source).toContain('getMasterUserSecurityActivity');
    expect(source).not.toContain('updateMyProfile');
    expect(source).not.toContain('changePassword');
    expect(source).not.toContain('revokeSession');
    expect(template).not.toContain('Save Profile');
    expect(template).not.toContain('Change Password');
    expect(template).not.toContain('Revoke Session');
  });
});

describe('Profile Management Batch 9 final guardrails', () => {
  it('has all required profile components', () => {
    for (const component of [
      'profile-avatar',
      'profile-image-uploader',
      'profile-summary-card',
      'profile-settings-card',
      'password-strength-meter',
      'security-activity-card',
      'session-card',
      'profile-empty-state',
      'profile-loading-state',
    ]) {
      expect(statSync(join(profileRoot, 'components', component, `${component}.ts`)).isFile()).toBe(true);
      expect(statSync(join(profileRoot, 'components', component, `${component}.html`)).isFile()).toBe(true);
      expect(statSync(join(profileRoot, 'components', component, `${component}.scss`)).isFile()).toBe(true);
    }
  });

  it('does not use legacy Angular syntax, NgModules, inline templates, CSS files, or component naming', () => {
    const files = collectFiles(profileRoot, () => true);
    const htmlOffenders = files
      .filter((filePath) => filePath.endsWith('.html'))
      .filter((filePath) => /\*ngIf\b|\*ngFor\b/.test(readFileSync(filePath, 'utf8')))
      .map(relativeProfilePath);
    const sourceOffenders = files
      .filter((filePath) => filePath.endsWith('.ts') && !filePath.endsWith('.spec.ts'))
      .filter((filePath) => /@NgModule\b|template\s*:`|template\s*: '/.test(readFileSync(filePath, 'utf8')))
      .map(relativeProfilePath);
    const cssFiles = files.filter((filePath) => filePath.endsWith('.css')).map(relativeProfilePath);
    const componentNamedFiles = files
      .filter((filePath) => /\.component\./.test(filePath))
      .map(relativeProfilePath);

    expect(htmlOffenders).toEqual([]);
    expect(sourceOffenders).toEqual([]);
    expect(cssFiles).toEqual([]);
    expect(componentNamedFiles).toEqual([]);
  });

  it('does not expose object keys, hardcoded signed URLs, profile image URLs, secrets, tokens, logging, reload, DOM hacks, or localStorage profile hacks', () => {
    const forbidden =
      /https:\/\/signed|https:\/\/cdn\.|console\.log|localStorage|sessionStorage|setTimeout|document\.querySelector|location\.reload|passwordHash|accessToken|providerSecret|paymentSecret/i;
    const offenders = collectFiles(profileRoot, (filePath) => {
      const productionFile =
        filePath.endsWith('.html') ||
        (filePath.endsWith('.ts') &&
          !filePath.endsWith('.spec.ts') &&
          !filePath.endsWith('profile.models.ts') &&
          !filePath.endsWith('profile-security.ts'));
      if (!productionFile) {
        return false;
      }

      const content = readFileSync(filePath, 'utf8');
      return forbidden.test(content);
    }).map(relativeProfilePath);

    expect(offenders).toEqual([]);

    const htmlSnapshots = collectFiles(profileRoot, (filePath) => filePath.endsWith('.html'))
      .map((filePath) => readFileSync(filePath, 'utf8'))
      .join('\n');
    expect(htmlSnapshots).not.toMatch(/objectKey|object_key|refreshToken|accessToken/i);
  });

  it('does not duplicate profile modules, auth/user stores, or fake upload foundations', () => {
    const profileFiles = collectFiles(profileRoot, () => true).map(relativeProfilePath);
    const appFiles = collectFiles(appRoot, (filePath) => filePath.endsWith('.ts')).map((filePath) =>
      relative(appRoot, filePath).replaceAll('\\', '/'),
    );

    expect(profileFiles.filter((file) => file.endsWith('state/profile.store.ts')).length).toBe(1);
    expect(profileFiles.filter((file) => file.endsWith('services/profile-api.service.ts')).length).toBe(1);
    expect(appFiles.filter((file) => file.endsWith('core/auth/current-user.store.ts')).length).toBe(1);
    expect(read('src/app/features/profile/state/profile.store.ts')).not.toContain('base64');
    expect(read('src/app/features/profile/services/profile-api.service.ts')).toContain('HttpRequest');
    expect(read('src/app/features/profile/services/profile-api.service.ts')).toContain('SKIP_APP_HEADERS');
  });

  it('uses standard ApiResponse handling in the profile API service', () => {
    const service = read('src/app/features/profile/services/profile-api.service.ts');
    const coreApi = read('src/app/core/api/api.service.ts');
    const unwrap = read('src/app/shared/utils/api-response.ts');

    expect(service).toContain('unwrapApiResponse');
    expect(service).toContain('this.api.get<T>(path');
    expect(service).toContain('this.api.post<T, TBody>(path');
    expect(service).toContain('this.api.put<T, TBody>(path');
    expect(coreApi).toContain('ApiResponse<T>');
    expect(unwrap).toContain('ApiResponse<T>');
  });
});
