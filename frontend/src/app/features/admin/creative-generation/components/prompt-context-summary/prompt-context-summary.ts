import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { PromptHistory } from '@app/features/admin/prompts/models/prompt.models';

@Component({
  selector: 'app-prompt-context-summary',
  standalone: true,
  imports: [BadgeComponent, CardComponent],
  templateUrl: './prompt-context-summary.html',
  styleUrl: './prompt-context-summary.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptContextSummary {
  readonly promptHistory = input<PromptHistory | null>(null);
  readonly sourcePrompt = input('');
  readonly enhancedPrompt = input('');
}
