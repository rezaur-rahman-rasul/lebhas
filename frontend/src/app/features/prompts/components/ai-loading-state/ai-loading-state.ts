import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-ai-loading-state',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './ai-loading-state.html',
  styleUrl: './ai-loading-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiLoadingState {
  readonly title = input('Thinking through your campaign context');
  readonly description = input('Prompt intelligence is running. Actions stay disabled until this finishes.');
  readonly compact = input(false);
}
