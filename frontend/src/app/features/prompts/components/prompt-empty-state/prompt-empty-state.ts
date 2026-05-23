import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-prompt-empty-state',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './prompt-empty-state.html',
  styleUrl: './prompt-empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptEmptyState {
  readonly icon = input('sparkles');
  readonly title = input.required<string>();
  readonly description = input.required<string>();
}
