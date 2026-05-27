import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';
import { NotificationType } from '../../models/notification.models';

@Component({
  selector: 'app-notification-type-icon',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './notification-type-icon.html',
  styleUrl: './notification-type-icon.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationTypeIconComponent {
  readonly notificationType = input<NotificationType | string>(NotificationType.SystemAlert);
  readonly size = input(18);

  protected readonly icon = computed(() => {
    switch (this.notificationType()) {
      case NotificationType.CreativeRequestCreated:
        return 'sparkles';
      case NotificationType.GenerationStarted:
        return 'play';
      case NotificationType.GenerationCompleted:
      case NotificationType.ApprovalApproved:
      case NotificationType.DownloadCompleted:
      case NotificationType.PaymentSucceeded:
      case NotificationType.AiProviderRecovered:
        return 'circle-check';
      case NotificationType.GenerationFailed:
      case NotificationType.ApprovalRejected:
      case NotificationType.PaymentFailed:
      case NotificationType.AiProviderFailed:
      case NotificationType.SystemAlert:
        return 'triangle-alert';
      case NotificationType.ApprovalRequested:
        return 'list-checks';
      case NotificationType.ShareLinkCreated:
        return 'share-2';
      case NotificationType.SubscriptionChanged:
        return 'package-check';
      case NotificationType.CreditLow:
        return 'wallet-cards';
      case NotificationType.StorageLimitExceeded:
        return 'cloud-upload';
      case NotificationType.ProfileUpdated:
      case NotificationType.ProfileImageUpdated:
        return 'user-round';
      case NotificationType.PasswordChanged:
      case NotificationType.SessionRevoked:
      case NotificationType.SecurityActivityDetected:
        return 'shield-check';
      default:
        return 'bell';
    }
  });

  protected readonly label = computed(() => friendlyNotificationType(this.notificationType()));
}

export function friendlyNotificationType(type: NotificationType | string): string {
  switch (type) {
    case NotificationType.AiProviderFailed:
      return 'One AI tool is currently failing.';
    case NotificationType.PaymentFailed:
      return 'A payment needs attention.';
    case NotificationType.StorageLimitExceeded:
      return 'Storage limit reached.';
    case NotificationType.GenerationFailed:
      return 'Creative generation needs attention.';
    case NotificationType.GenerationCompleted:
      return 'Creative generation completed.';
    case NotificationType.PaymentSucceeded:
      return 'Payment completed.';
    case NotificationType.CreditLow:
      return 'Credits are running low.';
    case NotificationType.ProfileUpdated:
      return 'Profile updated.';
    case NotificationType.ProfileImageUpdated:
      return 'Profile photo updated.';
    case NotificationType.PasswordChanged:
      return 'Password changed.';
    case NotificationType.SessionRevoked:
      return 'Session revoked.';
    case NotificationType.SecurityActivityDetected:
      return 'Security activity detected.';
    default:
      return String(type)
        .toLowerCase()
        .replace(/_/g, ' ')
        .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }
}
