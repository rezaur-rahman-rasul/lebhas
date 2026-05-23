import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { BrandProfile } from '@app/features/admin/workspace/models/brand-profile.models';

@Component({
  selector: 'app-brand-context-summary',
  standalone: true,
  imports: [BadgeComponent, CardComponent],
  templateUrl: './brand-context-summary.html',
  styleUrl: './brand-context-summary.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandContextSummary {
  readonly brandProfile = input<BrandProfile | null>(null);
  readonly enabled = input(false);
}
