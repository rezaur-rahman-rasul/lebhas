import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { ProfileApiService } from '../../services/profile-api.service';
import { safeProfileImageUrl } from '../../services/profile-security';
import { SecurityActivityView, SupportUserProfileView } from '../../models/profile.models';
import { ProfileAvatarComponent } from '../profile-avatar/profile-avatar';
import { ProfileEmptyStateComponent } from '../profile-empty-state/profile-empty-state';
import { ProfileLoadingStateComponent } from '../profile-loading-state/profile-loading-state';
import { SecurityActivityCardComponent } from '../security-activity-card/security-activity-card';

@Component({
  selector: 'app-support-profile-view',
  standalone: true,
  imports: [
    CardComponent,
    ButtonComponent,
    BadgeComponent,
    ProfileAvatarComponent,
    ProfileEmptyStateComponent,
    ProfileLoadingStateComponent,
    SecurityActivityCardComponent,
  ],
  templateUrl: './support-profile-view.html',
  styleUrl: './support-profile-view.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SupportProfileViewComponent {
  readonly userId = input<string | null>(null);
  readonly showSecurityActivity = input(true);

  private readonly api = inject(ProfileApiService);
  protected readonly permissions = inject(PermissionStore);

  private readonly profileSignal = signal<SupportUserProfileView | null>(null);
  private readonly securityActivitySignal = signal<readonly SecurityActivityView[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  protected readonly profile = this.profileSignal.asReadonly();
  protected readonly securityActivity = this.securityActivitySignal.asReadonly();
  protected readonly loading = this.loadingSignal.asReadonly();
  protected readonly error = this.errorSignal.asReadonly();
  protected readonly accessDenied = computed(() => !this.permissions.canViewUserProfileSupport());
  protected readonly avatarUrl = computed(() => safeProfileImageUrl(this.profileSignal()));
  protected readonly recentSecurityActivity = computed(() => this.securityActivitySignal().slice(0, 4));
  protected readonly displayName = computed(() => {
    const profile = this.profileSignal();
    return profile?.displayName || [profile?.firstName, profile?.lastName].filter(Boolean).join(' ') || 'User profile';
  });
  protected readonly emailLabel = computed(() => {
    const profile = this.profileSignal();
    return profile?.emailVisible === false ? 'Hidden by permission' : profile?.email || 'Email unavailable';
  });
  protected readonly phoneLabel = computed(() => {
    const profile = this.profileSignal();
    return profile?.phoneVisible === false ? 'Hidden by permission' : profile?.phoneNumber || 'Phone unavailable';
  });

  constructor() {
    effect(() => {
      const userId = this.userId();
      if (!userId || this.accessDenied()) {
        this.profileSignal.set(null);
        this.securityActivitySignal.set([]);
        return;
      }

      void this.load(userId);
    });
  }

  protected refresh(): void {
    const userId = this.userId();
    if (!userId || this.accessDenied()) {
      return;
    }

    void this.load(userId);
  }

  private async load(userId: string): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const profile = await this.api.getMasterUserProfile(userId);
      this.profileSignal.set(profile);

      if (this.showSecurityActivity()) {
        this.securityActivitySignal.set(await this.api.getMasterUserSecurityActivity(userId));
      }
    } catch {
      this.errorSignal.set('This user profile could not be loaded. Please try again.');
    } finally {
      this.loadingSignal.set(false);
    }
  }
}
