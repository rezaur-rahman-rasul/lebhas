import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';
import { ActivityFeed } from '../../models/activity.models';
import { activityTypeIcon, friendlyActivityType } from '../activity-type-badge/activity-type-badge';
import { referenceSummary } from '../activity-card/activity-card';

@Component({
  selector: 'app-timeline-item',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './timeline-item.html',
  styleUrl: './timeline-item.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class TimelineItemComponent {
  readonly activity = input.required<ActivityFeed>();
  readonly isLast = input(false);

  protected readonly icon = computed(() => activityTypeIcon(this.activity().activityType));
  protected readonly typeLabel = computed(() => friendlyActivityType(this.activity().activityType));
  protected readonly actorLabel = computed(() => this.activity().actorName || 'Workspace member');
  protected readonly referenceLabel = computed(() => referenceSummary(this.activity()));
}
