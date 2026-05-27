import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { MonitoringAlert, MonitoringAlertType } from '../../models/monitoring.models';
import { MonitoringSeverityBadgeComponent } from '../monitoring-severity-badge/monitoring-severity-badge';

@Component({
  selector: 'app-monitoring-alert-card',
  standalone: true,
  imports: [CardComponent, MonitoringSeverityBadgeComponent],
  templateUrl: './monitoring-alert-card.html',
  styleUrl: './monitoring-alert-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonitoringAlertCardComponent {
  readonly alert = input.required<MonitoringAlert>();

  protected readonly typeLabel = computed(() => friendlyAlertType(this.alert().alertType));
  protected readonly description = computed(() =>
    friendlyAlertDescription(this.alert().alertType, this.alert().description),
  );
  protected readonly workspaceLabel = computed(() => this.alert().workspaceName || this.alert().workspaceId || 'All workspaces');
  protected readonly providerLabel = computed(() => this.alert().relatedProviderName || 'No provider linked');
}

export function friendlyAlertType(type: MonitoringAlertType | string): string {
  switch (type) {
    case MonitoringAlertType.HighFailureRate:
      return 'Too many recent failures';
    case MonitoringAlertType.PaymentFailureSpike:
      return 'Payment issues increased';
    case MonitoringAlertType.StorageLimitWarning:
      return 'Storage needs attention';
    case MonitoringAlertType.CreditAbuseWarning:
      return 'Credit usage needs review';
    case MonitoringAlertType.AiProviderDown:
      return 'One AI tool is currently failing';
    case MonitoringAlertType.HighLatency:
      return 'Service is slower than usual';
    case MonitoringAlertType.GenerationSpike:
      return 'Creative generation increased';
    case MonitoringAlertType.RedisFailure:
      return 'Cache/state service issue';
    case MonitoringAlertType.KafkaFailure:
      return 'Event processing issue';
    default:
      return String(type)
        .toLowerCase()
        .replace(/_/g, ' ')
        .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }
}

function friendlyAlertDescription(type: MonitoringAlertType | string, description: string): string {
  switch (type) {
    case MonitoringAlertType.KafkaFailure:
      return 'Event processing is slower than usual.';
    case MonitoringAlertType.AiProviderDown:
      return 'One AI tool is currently failing.';
    case MonitoringAlertType.PaymentFailureSpike:
      return 'A payment confirmation could not be verified.';
    case MonitoringAlertType.RedisFailure:
      return 'Cache/state service issue.';
    default:
      return description;
  }
}
