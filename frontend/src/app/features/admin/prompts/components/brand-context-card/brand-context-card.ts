import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { BrandProfile } from '@app/features/admin/workspace/models/brand-profile.models';

@Component({
  selector: 'app-brand-context-card',
  standalone: true,
  imports: [BadgeComponent, CardComponent],
  templateUrl: './brand-context-card.html',
  styleUrl: './brand-context-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandContextCard {
  readonly brandProfile = input<BrandProfile | null>(null);
  readonly enabled = input(false);
  readonly disabled = input(false);

  readonly toggled = output<boolean>();
}
