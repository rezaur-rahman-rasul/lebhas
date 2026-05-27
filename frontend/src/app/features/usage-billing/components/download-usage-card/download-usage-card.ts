import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { DownloadUsageLog } from '../../models/usage-billing.models';

@Component({
  selector: 'app-download-usage-card',
  standalone: true,
  imports: [DatePipe, CardComponent, BadgeComponent],
  templateUrl: './download-usage-card.html',
  styleUrl: './download-usage-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DownloadUsageCardComponent {
  readonly log = input.required<DownloadUsageLog>();

  protected readonly downloadLabel = computed(() => this.toTitleCase(this.log().downloadType));
  protected readonly browserSummary = computed(() => this.summarizeUserAgent(this.log().userAgent));

  protected toTitleCase(value: string | null): string {
    return (value || 'Download')
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase());
  }

  private summarizeUserAgent(userAgent: string | null): string | null {
    if (!userAgent) {
      return null;
    }

    const lower = userAgent.toLowerCase();
    const browser = lower.includes('firefox')
      ? 'Firefox'
      : lower.includes('edg')
        ? 'Edge'
        : lower.includes('chrome')
          ? 'Chrome'
          : lower.includes('safari')
            ? 'Safari'
            : 'Browser';
    const device = lower.includes('mobile') ? 'Mobile' : 'Desktop';

    return `${device} ${browser}`;
  }
}
