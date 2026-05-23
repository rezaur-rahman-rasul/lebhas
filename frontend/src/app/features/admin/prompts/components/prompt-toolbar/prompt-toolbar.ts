import { ChangeDetectionStrategy, Component, output, input } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';

export type PromptToolbarAction =
  | 'enhance'
  | 'rewrite'
  | 'suggestions'
  | 'cta'
  | 'headline'
  | 'offer'
  | 'angle'
  | 'tone'
  | 'category';

@Component({
  selector: 'app-prompt-toolbar',
  standalone: true,
  imports: [ButtonComponent, CardComponent],
  templateUrl: './prompt-toolbar.html',
  styleUrl: './prompt-toolbar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptToolbar {
  readonly loading = input(false);
  readonly disabled = input(false);

  readonly actionTriggered = output<PromptToolbarAction>();

  protected trigger(action: PromptToolbarAction): void {
    this.actionTriggered.emit(action);
  }
}
