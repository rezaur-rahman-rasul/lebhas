import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-activity-empty-state',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './activity-empty-state.html',
  styleUrl: './activity-empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityEmptyStateComponent {
  readonly icon = input('activity');
  readonly title = input('No activity yet');
  readonly description = input('Workspace actions will appear here.');
}
