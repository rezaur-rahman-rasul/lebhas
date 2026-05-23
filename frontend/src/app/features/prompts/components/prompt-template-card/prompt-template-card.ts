import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import {
  campaignObjectiveLabel,
  promptLanguageLabel,
  promptPlatformLabel,
  promptTemplateStatusLabel,
  promptTemplateStatusTone,
} from '../../models/prompt-options';
import { PromptTemplate } from '../../models/prompt.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';

@Component({
  selector: 'app-prompt-template-card',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ButtonComponent, CardComponent],
  templateUrl: './prompt-template-card.html',
  styleUrl: './prompt-template-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptTemplateCard {
  readonly template = input.required<PromptTemplate>();
  readonly canManage = input(false);

  readonly editRequested = output<PromptTemplate>();
  readonly deleteRequested = output<PromptTemplate>();

  protected readonly promptPlatformLabel = promptPlatformLabel;
  protected readonly campaignObjectiveLabel = campaignObjectiveLabel;
  protected readonly promptLanguageLabel = promptLanguageLabel;
  protected readonly promptTemplateStatusLabel = promptTemplateStatusLabel;
  protected readonly promptTemplateStatusTone = promptTemplateStatusTone;
}
