import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { SystemHealthEvent, SystemHealthStatus } from '../../models/monitoring.models';
import { ServiceHealthBadgeComponent, serviceHealthLabel } from '../service-health-badge/service-health-badge';

@Component({
  selector: 'app-health-status-card',
  standalone: true,
  imports: [CardComponent, ServiceHealthBadgeComponent],
  templateUrl: './health-status-card.html',
  styleUrl: './health-status-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HealthStatusCardComponent {
  readonly healthEvent = input.required<SystemHealthEvent>();

  protected readonly serviceLabel = computed(() => friendlyServiceLabel(this.healthEvent().serviceName));
  protected readonly serviceExplanation = computed(() =>
    serviceExplanation(this.healthEvent().serviceName, this.healthEvent().status),
  );
  protected readonly statusLabel = computed(() => serviceHealthLabel(this.healthEvent().status));
}

export function friendlyServiceLabel(serviceName: string): string {
  const normalized = serviceName.toLowerCase();
  if (normalized.includes('kafka')) {
    return 'Event processing';
  }
  if (normalized.includes('redis')) {
    return 'Cache/state service';
  }
  if (normalized.includes('r2') || normalized.includes('storage')) {
    return 'Storage service';
  }
  if (normalized.includes('postgres') || normalized.includes('database')) {
    return 'Database';
  }
  if (normalized.includes('payment')) {
    return 'Payment service';
  }
  if (normalized.includes('ai')) {
    return 'AI service';
  }

  return serviceName
    .replace(/[-_]/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function serviceExplanation(serviceName: string, status: SystemHealthStatus | string): string {
  if (status === SystemHealthStatus.Healthy) {
    return `${friendlyServiceLabel(serviceName)} is working normally.`;
  }

  if (status === SystemHealthStatus.Degraded) {
    return `${friendlyServiceLabel(serviceName)} is slower than usual.`;
  }

  if (status === SystemHealthStatus.Down) {
    return `${friendlyServiceLabel(serviceName)} is not available right now.`;
  }

  return `${friendlyServiceLabel(serviceName)} status is not confirmed yet.`;
}
