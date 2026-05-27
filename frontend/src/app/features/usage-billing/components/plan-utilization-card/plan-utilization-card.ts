import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { IconComponent } from '@app/shared/components/icon/icon';
import { UsageLimitSnapshot } from '../../models/usage-billing.models';
import { UsageProgressBarComponent } from '../usage-progress-bar/usage-progress-bar';

@Component({
  selector: 'app-plan-utilization-card',
  standalone: true,
  imports: [CardComponent, BadgeComponent, IconComponent, UsageProgressBarComponent],
  templateUrl: './plan-utilization-card.html',
  styleUrl: './plan-utilization-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlanUtilizationCardComponent {
  readonly packageName = input<string | null>(null);
  readonly generatedVersionUsage = input<UsageLimitSnapshot | null>(null);
  readonly creditUsage = input<UsageLimitSnapshot | null>(null);
  readonly storageUsage = input<UsageLimitSnapshot | null>(null);
  readonly publicShareAvailable = input<boolean | null>(null);
  readonly approvalAvailable = input<boolean | null>(null);
  readonly videoGenerationAvailable = input<boolean | null>(null);

  protected readonly featureRows = computed(() =>
    [
      { label: 'Public sharing', available: this.publicShareAvailable() },
      { label: 'Approval workflow', available: this.approvalAvailable() },
      { label: 'Video generation', available: this.videoGenerationAvailable() },
    ].filter((row) => row.available !== null),
  );

  protected percent(usage: UsageLimitSnapshot | null): number | null {
    if (!usage?.limit || usage.limit <= 0) {
      return null;
    }

    return Math.min((usage.used / usage.limit) * 100, 100);
  }

  protected isNearLimit(usage: UsageLimitSnapshot | null): boolean {
    const percent = this.percent(usage);
    return percent !== null && percent >= 80 && percent < 100;
  }

  protected isExceeded(usage: UsageLimitSnapshot | null): boolean {
    return !!usage?.limit && usage.used >= usage.limit;
  }
}
