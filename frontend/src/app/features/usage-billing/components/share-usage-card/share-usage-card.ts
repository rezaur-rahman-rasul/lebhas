import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { ShareUsageLog } from '../../models/usage-billing.models';

@Component({
  selector: 'app-share-usage-card',
  standalone: true,
  imports: [DatePipe, CardComponent, BadgeComponent],
  templateUrl: './share-usage-card.html',
  styleUrl: './share-usage-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShareUsageCardComponent {
  readonly log = input.required<ShareUsageLog>();
  readonly status = input<string | null>(null);

  protected readonly statusLabel = computed(() => this.toTitleCase(this.status()));
  protected readonly statusTone = computed(() => (this.status()?.toLowerCase() === 'expired' ? 'neutral' : 'brand'));

  protected toTitleCase(value: string | null): string {
    return (value || 'Accessed')
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }
}
