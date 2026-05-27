import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';
import { MonitoringSeverity } from '../../models/monitoring.models';

@Component({
  selector: 'app-monitoring-severity-badge',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './monitoring-severity-badge.html',
  styleUrl: './monitoring-severity-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonitoringSeverityBadgeComponent {
  readonly severity = input<MonitoringSeverity | string>(MonitoringSeverity.Info);

  protected readonly label = computed(() => monitoringSeverityLabel(this.severity()));
  protected readonly icon = computed(() => monitoringSeverityIcon(this.severity()));
  protected readonly classes = computed(() => {
    const base =
      'inline-flex max-w-full items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium';

    switch (this.severity()) {
      case MonitoringSeverity.Critical:
        return `${base} border-alert-500/30 bg-alert-500/10 text-alert-700 dark:text-alert-300`;
      case MonitoringSeverity.Error:
        return `${base} border-alert-500/25 bg-alert-500/10 text-alert-700 dark:text-alert-300`;
      case MonitoringSeverity.Warning:
        return `${base} border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300`;
      default:
        return `${base} border-border bg-panel text-muted`;
    }
  });
}

export function monitoringSeverityLabel(severity: MonitoringSeverity | string): string {
  switch (severity) {
    case MonitoringSeverity.Warning:
      return 'Warning';
    case MonitoringSeverity.Error:
      return 'Error';
    case MonitoringSeverity.Critical:
      return 'Critical';
    default:
      return 'Info';
  }
}

function monitoringSeverityIcon(severity: MonitoringSeverity | string): string {
  switch (severity) {
    case MonitoringSeverity.Warning:
      return 'triangle-alert';
    case MonitoringSeverity.Error:
    case MonitoringSeverity.Critical:
      return 'circle-alert';
    default:
      return 'info';
  }
}
