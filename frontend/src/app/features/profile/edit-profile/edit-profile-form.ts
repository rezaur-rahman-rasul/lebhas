import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { ProfileLoadingStateComponent } from '../components/profile-loading-state/profile-loading-state';
import { UpdateProfileRequest } from '../models/profile.models';
import { ProfileStore } from '../state/profile.store';

@Component({
  selector: 'app-edit-profile-form-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    ProfileLoadingStateComponent,
  ],
  templateUrl: './edit-profile-form.html',
  styleUrl: './edit-profile-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditProfileFormPage {
  private readonly fb = new FormBuilder();
  private readonly router = inject(Router);
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(ProfileStore);

  protected readonly savedMessage = signal<string | null>(null);
  protected readonly accessDenied = computed(() => !this.permissions.canEditOwnProfile());
  protected readonly profile = this.store.profile;

  protected readonly timezones = [
    'Asia/Dhaka',
    'UTC',
    'Asia/Kolkata',
    'Asia/Singapore',
    'Europe/London',
    'America/New_York',
  ];
  protected readonly locales = ['en-US', 'en-GB', 'bn-BD'];

  protected readonly form = this.fb.nonNullable.group({
    firstName: ['', [Validators.required, Validators.maxLength(80)]],
    lastName: ['', [Validators.maxLength(80)]],
    displayName: ['', [Validators.required, Validators.maxLength(120)]],
    phoneNumber: ['', [Validators.maxLength(32), Validators.pattern(/^[+0-9()\-\s]*$/)]],
    jobTitle: ['', [Validators.maxLength(120)]],
    timezone: ['Asia/Dhaka', [Validators.maxLength(80)]],
    locale: ['en-US', [Validators.maxLength(20)]],
  });

  constructor() {
    effect(() => {
      if (this.accessDenied()) {
        return;
      }

      const profile = this.profile();
      if (!profile) {
        void this.store.loadMyProfile();
        return;
      }

      this.form.reset({
        firstName: profile.firstName,
        lastName: profile.lastName,
        displayName: profile.displayName,
        phoneNumber: profile.phoneNumber ?? '',
        jobTitle: profile.jobTitle ?? '',
        timezone: profile.timezone ?? 'Asia/Dhaka',
        locale: profile.locale ?? 'en-US',
      });
    });
  }

  protected cancel(): void {
    void this.router.navigate(['/profile']);
  }

  protected retry(): void {
    this.store.clearError();
    void this.store.loadMyProfile();
  }

  protected async submit(): Promise<void> {
    this.savedMessage.set(null);

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload: UpdateProfileRequest = {
      firstName: value.firstName.trim(),
      lastName: value.lastName.trim(),
      displayName: value.displayName.trim(),
      phoneNumber: nullableTrim(value.phoneNumber),
      jobTitle: nullableTrim(value.jobTitle),
      timezone: safeSelection(value.timezone, this.timezones),
      locale: safeSelection(value.locale, this.locales),
    };

    const result = await this.store.updateMyProfile(payload);
    if (result.ok) {
      this.savedMessage.set('Profile updated successfully.');
      void this.router.navigate(['/profile']);
    }
  }
}

function nullableTrim(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function safeSelection(value: string, allowed: readonly string[]): string | null {
  return allowed.includes(value) ? value : null;
}
