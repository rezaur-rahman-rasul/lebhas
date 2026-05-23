import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { PROMPT_MAX_LENGTH, PROMPT_MIN_LENGTH } from '../../models/prompt.models';

@Component({
  selector: 'app-prompt-editor',
  standalone: true,
  imports: [BadgeComponent, ButtonComponent, CardComponent],
  templateUrl: './prompt-editor.html',
  styleUrl: './prompt-editor.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptEditor {
  readonly sourcePrompt = input.required<string>();
  readonly enhancedPrompt = input<string | null>(null);
  readonly disabled = input(false);
  readonly showValidation = input(true);
  readonly sourcePromptError = input<string | null>(null);

  readonly sourcePromptChange = output<string>();
  readonly copyEnhanced = output<void>();
  readonly reset = output<void>();

  protected readonly minLength = PROMPT_MIN_LENGTH;
  protected readonly maxLength = PROMPT_MAX_LENGTH;

  protected readonly characterCount = computed(() => this.sourcePrompt().length);
  protected readonly canCopyEnhanced = computed(
    () => Boolean(this.enhancedPrompt()?.trim()) && !this.disabled(),
  );
}
