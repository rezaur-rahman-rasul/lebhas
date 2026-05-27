import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { UserRole } from '@app/features/auth/models/user.models';
import { UserProfileView } from '../../models/profile.models';
import { ProfileAvatarComponent } from '../profile-avatar/profile-avatar';

@Component({
  selector: 'app-profile-summary-card',
  standalone: true,
  imports: [ButtonComponent, CardComponent, ProfileAvatarComponent],
  templateUrl: './profile-summary-card.html',
  styleUrl: './profile-summary-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileSummaryCardComponent {
  readonly profile = input.required<UserProfileView>();
  readonly role = input<UserRole | string | null>(null);
  readonly workspaceName = input<string | null>(null);
  readonly showActions = input(true);

  readonly editProfile = output<void>();
  readonly manageSettings = output<void>();
  readonly changePassword = output<void>();

  protected readonly fullName = computed(() => {
    const profile = this.profile();
    return profile.displayName || [profile.firstName, profile.lastName].filter(Boolean).join(' ') || 'User';
  });
  protected readonly roleLabel = computed(() => friendlyRole(this.role()));
}

function friendlyRole(role: UserRole | string | null): string {
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
