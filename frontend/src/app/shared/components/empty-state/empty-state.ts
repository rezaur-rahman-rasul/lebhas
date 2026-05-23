import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { IconComponent } from '../icon/icon';

@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './empty-state.html',
  styleUrl: './empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  readonly icon = input('sparkles');
  readonly title = input.required<string>();
  readonly description = input.required<string>();
}
