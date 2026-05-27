import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-activity-loading-state',
  standalone: true,
  templateUrl: './activity-loading-state.html',
  styleUrl: './activity-loading-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityLoadingStateComponent {
  readonly label = input('Loading workspace activity');
  protected readonly rows = Array.from({ length: 5 }, (_, index) => index);
}
