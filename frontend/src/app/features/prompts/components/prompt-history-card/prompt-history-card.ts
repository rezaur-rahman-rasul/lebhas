import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import {
  promptLanguageLabel,
  promptPlatformLabel,
  promptPreviewText,
  promptHistoryStatusLabel,
  promptHistoryStatusTone,
} from '../../models/prompt-options';
import { PromptHistory } from '../../models/prompt.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';

@Component({
  selector: 'app-prompt-history-card',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ButtonComponent, CardComponent],
  templateUrl: './prompt-history-card.html',
  styleUrl: './prompt-history-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptHistoryCard {
  readonly entry = input.required<PromptHistory>();
  readonly selected = input(false);

  readonly detailRequested = output<PromptHistory>();

  protected readonly promptPlatformLabel = promptPlatformLabel;
  protected readonly promptLanguageLabel = promptLanguageLabel;
  protected readonly promptHistoryStatusLabel = promptHistoryStatusLabel;
  protected readonly promptHistoryStatusTone = promptHistoryStatusTone;
  protected readonly promptPreviewText = promptPreviewText;
}
