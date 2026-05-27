import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { UsageBillingLog } from '../../models/usage-billing.models';

@Component({
  selector: 'app-usage-log-card',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, CardComponent, BadgeComponent],
  templateUrl: './usage-log-card.html',
  styleUrl: './usage-log-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsageLogCardComponent {
  readonly log = input.required<UsageBillingLog>();

  protected readonly usageLabel = computed(() => this.toTitleCase(this.log().usageType));

  protected toTitleCase(value: string | null): string {
    return (value || 'Usage')
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }
}
