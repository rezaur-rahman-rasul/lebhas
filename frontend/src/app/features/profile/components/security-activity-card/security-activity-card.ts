import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';
import { SecurityActivityType, SecurityActivityView } from '../../models/profile.models';

@Component({
  selector: 'app-security-activity-card',
  standalone: true,
  imports: [CardComponent, IconComponent],
  templateUrl: './security-activity-card.html',
  styleUrl: './security-activity-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SecurityActivityCardComponent {
  readonly activity = input.required<SecurityActivityView>();

  protected readonly activityLabel = computed(() => securityActivityLabel(this.activity().activityType));
  protected readonly statusLabel = computed(() => (this.activity().success ? 'Successful' : 'Needs attention'));
  protected readonly iconName = computed(() => (this.activity().success ? 'shield-check' : 'shield-alert'));
  protected readonly iconClasses = computed(() =>
    this.activity().success
      ? 'grid size-10 shrink-0 place-items-center rounded-md bg-success-500/10 text-success-300'
      : 'grid size-10 shrink-0 place-items-center rounded-md bg-alert-500/10 text-alert-200',
  );
  protected readonly badgeClasses = computed(() =>
    this.activity().success
      ? 'inline-flex w-fit rounded-full bg-success-500/10 px-2.5 py-1 text-xs font-medium text-success-300'
      : 'inline-flex w-fit rounded-full bg-alert-500/10 px-2.5 py-1 text-xs font-medium text-alert-200',
  );
}

export function securityActivityLabel(value: SecurityActivityType | string): string {
  switch (value) {
    case SecurityActivityType.LoginSuccess:
      return 'Signed in';
    case SecurityActivityType.LoginFailed:
      return 'Sign-in attempt failed';
    case SecurityActivityType.Logout:
      return 'Signed out';
    case SecurityActivityType.PasswordChanged:
      return 'Password changed';
    case SecurityActivityType.PasswordChangeFailed:
      return 'Password change failed';
    case SecurityActivityType.ProfileUpdated:
      return 'Profile updated';
    case SecurityActivityType.ProfileImageUpdated:
      return 'Profile photo updated';
    case SecurityActivityType.ProfileImageRemoved:
      return 'Profile photo removed';
    case SecurityActivityType.RefreshTokenRotated:
      return 'Session refreshed';
    case SecurityActivityType.SessionRevoked:
      return 'Session revoked';
    default:
      return 'Security activity';
  }
}
