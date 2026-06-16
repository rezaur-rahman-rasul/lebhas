import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
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
import { ProfileApiService } from '../services/profile-api.service';
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
  private readonly profileApi = inject(ProfileApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly submitted = signal(false);
  protected readonly successMessage = signal<string | null>(null);
  protected readonly photoMessage = signal<string | null>(null);
  protected readonly selectedProfileImageFile = signal<File | null>(null);
  protected readonly previewResetKey = signal(0);
  protected readonly rewardStatus = signal<Readonly<Record<string, boolean>>>({});
  protected readonly rewardEmail = signal('');
  protected readonly facebookUrl = signal('');
  protected readonly instagramUrl = signal('');
  protected readonly rewardMessage = signal<string | null>(null);

  protected readonly accessDenied = computed(() => !this.permissions.canViewOwnProfile());
  protected readonly profile = this.store.profile;
  protected readonly canEdit = this.permissions.canEditOwnProfile;
  protected readonly role = computed(() => friendlyRole(this.auth.currentRole()));
  protected readonly workspaceName = this.workspace.workspaceLabel;
  protected readonly savedProfileImageUrl = this.store.savedProfileImageUrl;
  protected readonly imageDirty = computed(() => Boolean(this.selectedProfileImageFile()));
  protected readonly formDirty = signal(false);
  protected readonly profileDirty = computed(() => this.formDirty() || this.imageDirty());
  protected readonly profileEmail = computed(() => this.profile()?.email?.trim() || this.auth.currentUser()?.email?.trim() || '');
  protected readonly hasAccountEmail = computed(() => Boolean(this.profileEmail()));

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
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.formDirty.set(this.form.dirty));

    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      void this.store.loadMyProfile();
      void this.workspace.initialize();
      void this.loadRewardStatus();
    });

    effect(() => {
      const profile = this.profile();
      if (!profile) {
        return;
      }

      this.form.reset(
        {
          firstName: profile.firstName ?? '',
          lastName: profile.lastName ?? '',
          displayName: profile.displayName ?? '',
          phoneNumber: profile.phoneNumber ?? '',
          timezone: profile.timezone ?? '',
          locale: profile.locale ?? '',
          jobTitle: profile.jobTitle ?? '',
          bio: 'Bio will appear here when backend support is available.',
        },
        { emitEvent: false },
      );
      this.formDirty.set(false);
      this.rewardEmail.set(profile.email?.trim() || this.auth.currentUser()?.email?.trim() || '');
    });
  }

  protected retry(): void {
    this.store.clearError();
    this.clearSelectedPhoto();
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
    const result = await this.store.updateMyProfile(
      {
        firstName: value.firstName.trim(),
        lastName: value.lastName.trim(),
        displayName: value.displayName.trim(),
        phoneNumber: value.phoneNumber.trim() || null,
        jobTitle: value.jobTitle.trim() || null,
        timezone: value.timezone.trim() || null,
        locale: value.locale.trim() || null,
      },
      this.selectedProfileImageFile(),
    );

    if (result.ok) {
      this.submitted.set(false);
      this.clearSelectedPhoto(false);
      this.form.markAsPristine();
      this.formDirty.set(false);
      this.successMessage.set('Profile changes saved.');
    }
  }

  protected requestPhotoUpload(file: File): void {
    this.photoMessage.set(null);
    this.successMessage.set(null);
    this.selectedProfileImageFile.set(file);
    this.form.markAsDirty();
    this.photoMessage.set('Profile photo selected. Click Save changes to upload it.');
  }

  protected async claimEmailReward(): Promise<void> {
    const result = await this.profileApi.updateRewardEmail(this.rewardEmail());
    this.rewardMessage.set(result.granted ? `Email reward claimed: +${result.creditsGranted} credits.` : 'Email reward already claimed.');
    await this.loadRewardStatus();
  }

  protected async claimFacebookReward(): Promise<void> {
    const result = await this.profileApi.connectFacebook(this.facebookUrl());
    this.rewardMessage.set(result.granted ? `Facebook reward claimed: +${result.creditsGranted} credits.` : 'Facebook reward already claimed.');
    await this.loadRewardStatus();
  }

  protected async claimInstagramReward(): Promise<void> {
    const result = await this.profileApi.connectInstagram(this.instagramUrl());
    this.rewardMessage.set(result.granted ? `Instagram reward claimed: +${result.creditsGranted} credits.` : 'Instagram reward already claimed.');
    await this.loadRewardStatus();
  }

  private async loadRewardStatus(): Promise<void> {
    try {
      this.rewardStatus.set(await this.profileApi.getRewardStatus());
    } catch {
      this.rewardStatus.set({});
    }
  }

  private clearSelectedPhoto(discardStorePreview = true): void {
    this.selectedProfileImageFile.set(null);
    if (discardStorePreview) {
      this.store.discardStagedProfileImagePreview();
    }
    this.resetUploaderPreview();
  }

  private resetUploaderPreview(): void {
    this.previewResetKey.update((value) => value + 1);
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
