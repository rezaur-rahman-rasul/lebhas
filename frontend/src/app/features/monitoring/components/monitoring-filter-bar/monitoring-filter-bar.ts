import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { MonitoringAlertType, MonitoringSeverity } from '../../models/monitoring.models';
import { friendlyAlertType } from '../monitoring-alert-card/monitoring-alert-card';
import { monitoringSeverityLabel } from '../monitoring-severity-badge/monitoring-severity-badge';

@Component({
  selector: 'app-monitoring-filter-bar',
  standalone: true,
  imports: [ButtonComponent],
  templateUrl: './monitoring-filter-bar.html',
  styleUrl: './monitoring-filter-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonitoringFilterBarComponent {
  readonly selectedAlertType = input<MonitoringAlertType | string | null>(null);
  readonly selectedSeverity = input<MonitoringSeverity | string | null>(null);
  readonly unresolvedOnly = input(true);
  readonly loading = input(false);

  readonly selectedAlertTypeChange = output<MonitoringAlertType | string | null>();
  readonly selectedSeverityChange = output<MonitoringSeverity | string | null>();
  readonly unresolvedOnlyChange = output<boolean>();
  readonly refresh = output<void>();
  readonly clear = output<void>();

  protected readonly alertTypes = Object.values(MonitoringAlertType);
  protected readonly severities = Object.values(MonitoringSeverity);
  protected readonly friendlyAlertType = friendlyAlertType;
  protected readonly monitoringSeverityLabel = monitoringSeverityLabel;
}
