import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { ActivityFeed } from '../../models/activity.models';
import { ActivityTypeBadgeComponent, friendlyActivityType } from '../activity-type-badge/activity-type-badge';

@Component({
  selector: 'app-activity-card',
  standalone: true,
  imports: [ButtonComponent, CardComponent, ActivityTypeBadgeComponent],
  templateUrl: './activity-card.html',
  styleUrl: './activity-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityCardComponent {
  readonly activity = input.required<ActivityFeed>();
  readonly showDetailAction = input(true);

  readonly detailSelected = output<ActivityFeed>();

  protected readonly actorLabel = computed(() => this.activity().actorName || 'Workspace member');
  protected readonly typeLabel = computed(() => friendlyActivityType(this.activity().activityType));
  protected readonly referenceLabel = computed(() => referenceSummary(this.activity()));
  protected readonly metadataSummary = computed(() => safeMetadataSummary(this.activity().metadataJson));
}

export function referenceSummary(activity: ActivityFeed): string {
  if (activity.referenceType && activity.referenceId) {
    return `${friendlyReference(activity.referenceType)} ${activity.referenceId}`;
  }

  if (activity.referenceType) {
    return friendlyReference(activity.referenceType);
  }

  return 'Workspace activity';
}

export function friendlyReference(referenceType: string): string {
  return referenceType
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function safeMetadataSummary(metadataJson: string | null): string {
  if (!metadataJson) {
    return 'No extra details';
  }

  try {
    const parsed = JSON.parse(metadataJson) as unknown;
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return 'Extra details available';
    }

    const entries = Object.entries(parsed as Record<string, unknown>)
      .filter(([key]) => !isSensitiveKey(key))
      .slice(0, 3)
      .map(([key, value]) => `${friendlyReference(key)}: ${safeValue(value)}`);

    return entries.length ? entries.join(' · ') : 'Details are protected';
  } catch {
    return 'Extra details available';
  }
}

function safeValue(value: unknown): string {
  if (value === null || value === undefined || value === '') {
    return 'Unavailable';
  }

  if (typeof value === 'string' || typeof value === 'number' || typeof value === 'boolean') {
    return String(value).slice(0, 80);
  }

  return 'Available';
}

function isSensitiveKey(key: string): boolean {
  return /(secret|token|password|credential|api[-_]?key|webhook|jwt|session|authorization)/i.test(key);
}
