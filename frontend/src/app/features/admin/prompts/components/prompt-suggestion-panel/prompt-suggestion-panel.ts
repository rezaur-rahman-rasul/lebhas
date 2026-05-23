import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { PromptSuggestionSection } from '../../models/prompt.models';

@Component({
  selector: 'app-prompt-suggestion-panel',
  standalone: true,
  imports: [ButtonComponent, CardComponent, EmptyStateComponent],
  templateUrl: './prompt-suggestion-panel.html',
  styleUrl: './prompt-suggestion-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptSuggestionPanel {
  readonly sections = input<readonly PromptSuggestionSection[]>([]);
  readonly loading = input(false);
  readonly reasoningSummary = input<string | null>(null);

  readonly copyRequested = output<string>();
  readonly insertRequested = output<string>();
  readonly replaceRequested = output<string>();
  readonly savedRequested = output<string>();
}
