import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import {
  campaignObjectiveLabel,
  promptLanguageLabel,
  promptPlatformLabel,
  promptTemplateStatusLabel,
  promptTemplateStatusTone,
  PromptTemplate,
} from '../../models/prompt.models';

@Component({
  selector: 'app-prompt-template-card',
  standalone: true,
  imports: [BadgeComponent, ButtonComponent, CardComponent],
  templateUrl: './prompt-template-card.html',
  styleUrl: './prompt-template-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptTemplateCard {
  readonly template = input.required<PromptTemplate>();
  readonly canManage = input(false);
  readonly canUse = input(false);

  readonly useRequested = output<PromptTemplate>();
  readonly editRequested = output<PromptTemplate>();
  readonly deleteRequested = output<PromptTemplate>();

  protected readonly promptPlatformLabel = promptPlatformLabel;
  protected readonly campaignObjectiveLabel = campaignObjectiveLabel;
  protected readonly promptLanguageLabel = promptLanguageLabel;
  protected readonly promptTemplateStatusLabel = promptTemplateStatusLabel;
  protected readonly promptTemplateStatusTone = promptTemplateStatusTone;
}
