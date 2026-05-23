import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';

export type PromptContextKind = 'brand' | 'product' | 'project';

@Component({
  selector: 'app-prompt-context-card',
  standalone: true,
  imports: [BadgeComponent, CardComponent],
  templateUrl: './prompt-context-card.html',
  styleUrl: './prompt-context-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptContextCard {
  readonly kind = input.required<PromptContextKind>();
  readonly title = input.required<string>();
  readonly subtitle = input<string | null>(null);
  readonly empty = input(false);

  readonly businessType = input<string | null>(null);
  readonly industry = input<string | null>(null);
  readonly targetAudience = input<string | null>(null);
  readonly brandVoice = input<string | null>(null);

  readonly description = input<string | null>(null);
  readonly category = input<string | null>(null);
  readonly sellingPoints = input<string | null>(null);

  readonly campaignObjective = input<string | null>(null);
  readonly targetPlatform = input<string | null>(null);
  readonly campaignType = input<string | null>(null);
}
