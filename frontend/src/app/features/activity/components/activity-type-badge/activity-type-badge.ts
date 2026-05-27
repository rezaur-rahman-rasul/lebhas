import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';
import { ActivityType } from '../../models/activity.models';

@Component({
  selector: 'app-activity-type-badge',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './activity-type-badge.html',
  styleUrl: './activity-type-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityTypeBadgeComponent {
  readonly activityType = input<ActivityType | string>(ActivityType.BrandCreated);

  protected readonly label = computed(() => friendlyActivityType(this.activityType()));
  protected readonly icon = computed(() => activityTypeIcon(this.activityType()));
}

export function friendlyActivityType(type: ActivityType | string): string {
  switch (type) {
    case ActivityType.BrandCreated:
      return 'Brand created';
    case ActivityType.ProductCreated:
      return 'Product or service created';
    case ActivityType.ProjectCreated:
      return 'Project created';
    case ActivityType.AssetUploaded:
      return 'Asset uploaded';
    case ActivityType.CreativeRequestCreated:
      return 'Creative request created';
    case ActivityType.GenerationCompleted:
      return 'Creative generated';
    case ActivityType.ApprovalAction:
      return 'Approval updated';
    case ActivityType.DownloadCompleted:
      return 'Download completed';
    case ActivityType.ShareCreated:
      return 'Share link created';
    case ActivityType.PaymentCompleted:
      return 'Payment completed';
    case ActivityType.SubscriptionChanged:
      return 'Subscription updated';
    case ActivityType.AiProviderSwitched:
      return 'AI routing changed';
    case ActivityType.RoutingPolicyChanged:
      return 'Routing policy updated';
    default:
      return String(type)
        .toLowerCase()
        .replace(/_/g, ' ')
        .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }
}

export function activityTypeIcon(type: ActivityType | string): string {
  switch (type) {
    case ActivityType.BrandCreated:
      return 'badge-check';
    case ActivityType.ProductCreated:
      return 'package-open';
    case ActivityType.ProjectCreated:
      return 'folder-kanban';
    case ActivityType.AssetUploaded:
      return 'upload-cloud';
    case ActivityType.CreativeRequestCreated:
      return 'pencil-line';
    case ActivityType.GenerationCompleted:
      return 'sparkles';
    case ActivityType.ApprovalAction:
      return 'shield-check';
    case ActivityType.DownloadCompleted:
      return 'download';
    case ActivityType.ShareCreated:
      return 'share-2';
    case ActivityType.PaymentCompleted:
      return 'credit-card';
    case ActivityType.SubscriptionChanged:
      return 'package-check';
    case ActivityType.AiProviderSwitched:
      return 'arrow-right';
    case ActivityType.RoutingPolicyChanged:
      return 'settings';
    default:
      return 'activity';
  }
}
