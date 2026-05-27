import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ProfileEmptyStateComponent } from '../components/profile-empty-state/profile-empty-state';
import { ProfileImageUploaderComponent } from '../components/profile-image-uploader/profile-image-uploader';
import { ProfileLoadingStateComponent } from '../components/profile-loading-state/profile-loading-state';
import { ProfileSettingsCardComponent } from '../components/profile-settings-card/profile-settings-card';
import { ProfileSummaryCardComponent } from '../components/profile-summary-card/profile-summary-card';
import { SecurityActivityCardComponent } from '../components/security-activity-card/security-activity-card';
import { ProfileStore } from '../state/profile.store';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [
    DatePipe,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    ProfileEmptyStateComponent,
    ProfileImageUploaderComponent,
    ProfileLoadingStateComponent,
    ProfileSettingsCardComponent,
    ProfileSummaryCardComponent,
    SecurityActivityCardComponent,
  ],
  templateUrl: './profile-page.html',
  styleUrl: './profile-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfilePage {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(ProfileStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly auth = inject(CurrentUserStore);
  private readonly router = inject(Router);

  protected readonly photoMessage = signal<string | null>(null);
  protected readonly removingPhoto = signal(false);

  protected readonly accessDenied = computed(() => !this.permissions.canViewOwnProfile());
  protected readonly profile = this.store.profile;
  protected readonly settings = this.store.accountSettings;
  protected readonly recentSecurityActivity = this.store.recentSecurityActivity;
  protected readonly hasSecurityActivity = computed(() => this.recentSecurityActivity().length > 0);
  protected readonly canEdit = this.permissions.canEditOwnProfile;
  protected readonly canManageImage = this.permissions.canManageProfileImage;
  protected readonly role = this.auth.currentRole;
  protected readonly workspaceName = this.workspace.workspaceLabel;

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      void this.store.loadMyProfile();
      void this.store.loadSecurityActivity();
      void this.workspace.initialize();
    });
  }

  protected retry(): void {
    this.store.clearError();
    void this.store.loadMyProfile();
    void this.store.loadSecurityActivity();
  }

  protected goToEditProfile(): void {
    void this.router.navigate(['/profile/edit']);
  }

  protected goToChangePassword(): void {
    void this.router.navigate(['/profile/change-password']);
  }

  protected goToSettings(): void {
    void this.router.navigate(['/profile/settings']);
  }

  protected goToSessions(): void {
    void this.router.navigate(['/profile/sessions']);
  }

  protected goToSecurityActivity(): void {
    void this.router.navigate(['/profile/security']);
  }

  protected async requestPhotoUpload(file: File): Promise<void> {
    this.photoMessage.set(null);
    const result = await this.store.uploadProfileImage(file);

    this.photoMessage.set(
      result.ok
        ? 'Profile photo updated successfully.'
        : result.message ?? 'Profile photo could not be prepared. Please try again.',
    );
  }

  protected async removeProfileImage(): Promise<void> {
    this.removingPhoto.set(true);
    const result = await this.store.removeProfileImage();
    this.photoMessage.set(
      result.ok ? 'Profile photo removed.' : result.message ?? 'Profile photo could not be removed.',
    );
    this.removingPhoto.set(false);
  }
}
