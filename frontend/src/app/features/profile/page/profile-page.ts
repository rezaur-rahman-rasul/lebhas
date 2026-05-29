import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { IconComponent } from '@app/shared/components/icon/icon';
import { ProfileAvatarComponent } from '../components/profile-avatar/profile-avatar';
import { ProfileEmptyStateComponent } from '../components/profile-empty-state/profile-empty-state';
import { ProfileImageUploaderComponent } from '../components/profile-image-uploader/profile-image-uploader';
import { ProfileLoadingStateComponent } from '../components/profile-loading-state/profile-loading-state';
import { ProfileStore } from '../state/profile.store';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    ButtonComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    IconComponent,
    ProfileAvatarComponent,
    ProfileEmptyStateComponent,
    ProfileImageUploaderComponent,
    ProfileLoadingStateComponent,
  ],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage {
  private readonly fb = new FormBuilder();
  private readonly router = inject(Router);
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(ProfileStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly auth = inject(CurrentUserStore);

  protected readonly submitted = signal(false);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly photoMessage = signal<string | null>(null);
  protected readonly removingPhoto = signal(false);

  protected readonly accessDenied = computed(() => !this.permissions.canViewOwnProfile());
  protected readonly profile = this.store.profile;
  protected readonly canEdit = this.permissions.canEditOwnProfile;
  protected readonly canManageImage = this.permissions.canManageProfileImage;
  protected readonly role = computed(() => friendlyRole(this.auth.currentRole()));
  protected readonly workspaceName = this.workspace.workspaceLabel;

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.required, Validators.maxLength(80)]],
    displayName: ['', [Validators.maxLength(140)]],
    phoneNumber: [''],
    timezone: [''],
    locale: [''],
    jobTitle: [''],
    bio: [{ value: '', disabled: true }],
  });

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      void this.store.loadMyProfile();
      void this.workspace.initialize();
    });

    effect(() => {
      const profile = this.profile();
      if (!profile) {
        return;
      }

      this.form.reset({
        firstName: profile.firstName ?? '',
        lastName: profile.lastName ?? '',
        displayName: profile.displayName ?? '',
        phoneNumber: profile.phoneNumber ?? '',
        timezone: profile.timezone ?? '',
        locale: profile.locale ?? '',
        jobTitle: profile.jobTitle ?? '',
        bio: 'Bio will appear here when backend support is available.',
      });
    });
  }

  protected retry(): void {
    this.store.clearError();
    void this.store.loadMyProfile();
  }

  protected goToSettings(): void {
    void this.router.navigate(['/account-settings']);
  }

  protected goToSecurityActivity(): void {
    void this.router.navigate(['/security-activity']);
  }

  protected async saveProfile(): Promise<void> {
    this.submitted.set(true);
    this.successMessage.set(null);

    if (this.form.invalid || !this.canEdit()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const result = await this.store.updateMyProfile({
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      displayName: value.displayName.trim(),
      phoneNumber: value.phoneNumber.trim() || null,
      jobTitle: value.jobTitle.trim() || null,
      timezone: value.timezone.trim() || null,
      locale: value.locale.trim() || null,
    });

    if (result.ok) {
      this.submitted.set(false);
      this.successMessage.set('Profile changes saved.');
    }
  }

  protected async requestPhotoUpload(file: File): Promise<void> {
    this.photoMessage.set(null);
    const result = await this.store.uploadProfileImage(file);
    this.photoMessage.set(result.ok ? 'Profile photo updated.' : result.message ?? 'Profile photo could not be updated.');
  }

  protected async removeProfileImage(): Promise<void> {
    this.removingPhoto.set(true);
    const result = await this.store.removeProfileImage();
    this.photoMessage.set(result.ok ? 'Profile photo removed.' : result.message ?? 'Profile photo could not be removed.');
    this.removingPhoto.set(false);
  }
}

function friendlyRole(role: string | null): string {
  switch (role) {
    case 'MASTER':
      return 'Master';
    case 'ADMIN':
      return 'Admin';
    case 'CREW':
      return 'Crew';
    default:
      return 'User';
  }
}
