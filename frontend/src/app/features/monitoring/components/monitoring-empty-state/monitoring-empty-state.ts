import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-monitoring-empty-state',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './monitoring-empty-state.html',
  styleUrl: './monitoring-empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonitoringEmptyStateComponent {
  readonly icon = input('circle-check');
  readonly title = input('No monitoring alerts right now');
  readonly description = input('Everything looks normal.');
}
