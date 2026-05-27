import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';
import { SystemHealthStatus } from '../../models/monitoring.models';

@Component({
  selector: 'app-service-health-badge',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './service-health-badge.html',
  styleUrl: './service-health-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ServiceHealthBadgeComponent {
  readonly status = input<SystemHealthStatus | string>(SystemHealthStatus.Unknown);

  protected readonly label = computed(() => serviceHealthLabel(this.status()));
  protected readonly icon = computed(() => serviceHealthIcon(this.status()));
  protected readonly classes = computed(() => {
    const base =
      'inline-flex max-w-full items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium';

    switch (this.status()) {
      case SystemHealthStatus.Healthy:
        return `${base} border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300`;
      case SystemHealthStatus.Degraded:
        return `${base} border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300`;
      case SystemHealthStatus.Down:
        return `${base} border-alert-500/30 bg-alert-500/10 text-alert-700 dark:text-alert-300`;
      default:
        return `${base} border-border bg-panel text-muted`;
    }
  });
}

export function serviceHealthLabel(status: SystemHealthStatus | string): string {
  switch (status) {
    case SystemHealthStatus.Healthy:
      return 'Healthy';
    case SystemHealthStatus.Degraded:
      return 'Needs attention';
    case SystemHealthStatus.Down:
      return 'Not available';
    default:
      return 'Unknown';
  }
}

function serviceHealthIcon(status: SystemHealthStatus | string): string {
  switch (status) {
    case SystemHealthStatus.Healthy:
      return 'circle-check';
    case SystemHealthStatus.Degraded:
      return 'triangle-alert';
    case SystemHealthStatus.Down:
      return 'circle-alert';
    default:
      return 'info';
  }
}
