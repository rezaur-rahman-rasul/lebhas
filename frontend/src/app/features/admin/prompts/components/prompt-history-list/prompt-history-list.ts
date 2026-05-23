import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import {
  campaignObjectiveLabel,
  promptHistoryStatusLabel,
  promptHistoryStatusTone,
  promptLanguageLabel,
  promptPlatformLabel,
  PromptHistory,
  PromptPagination,
  suggestionTypeLabel,
} from '../../models/prompt.models';

@Component({
  selector: 'app-prompt-history-list',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ButtonComponent, CardComponent],
  templateUrl: './prompt-history-list.html',
  styleUrl: './prompt-history-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptHistoryList {
  readonly history = input<readonly PromptHistory[]>([]);
  readonly pagination = input.required<PromptPagination>();
  readonly loading = input(false);

  readonly detailRequested = output<PromptHistory>();
  readonly previousRequested = output<void>();
  readonly nextRequested = output<void>();

  protected readonly promptPlatformLabel = promptPlatformLabel;
  protected readonly promptLanguageLabel = promptLanguageLabel;
  protected readonly campaignObjectiveLabel = campaignObjectiveLabel;
  protected readonly suggestionTypeLabel = suggestionTypeLabel;
  protected readonly promptHistoryStatusLabel = promptHistoryStatusLabel;
  protected readonly promptHistoryStatusTone = promptHistoryStatusTone;
}
