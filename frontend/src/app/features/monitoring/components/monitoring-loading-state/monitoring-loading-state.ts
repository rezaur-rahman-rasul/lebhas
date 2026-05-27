import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-monitoring-loading-state',
  standalone: true,
  templateUrl: './monitoring-loading-state.html',
  styleUrl: './monitoring-loading-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MonitoringLoadingStateComponent {
  readonly label = input('Loading monitoring data');
  readonly cardCount = input(4);

  protected rows(): readonly number[] {
    return Array.from({ length: this.cardCount() }, (_, index) => index);
  }
}
