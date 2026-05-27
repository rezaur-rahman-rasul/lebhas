import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { ProviderHealthStatus } from '../../models/ai-monitoring.models';

@Component({
  selector: 'app-provider-status-badge',
  standalone: true,
  imports: [BadgeComponent],
  templateUrl: './provider-status-badge.html',
  styleUrl: './provider-status-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderStatusBadgeComponent {
  readonly status = input.required<ProviderHealthStatus | string>();

  protected readonly label = computed(() =>
    String(this.status())
      .toLowerCase()
      .replace(/_/g, ' ')
      .replace(/\b\w/g, (letter) => letter.toUpperCase()),
  );

  protected readonly tone = computed(() => {
    switch (this.status()) {
      case ProviderHealthStatus.Healthy:
        return 'brand';
      case ProviderHealthStatus.Degraded:
      case ProviderHealthStatus.Cooldown:
        return 'blue';
      case ProviderHealthStatus.Down:
        return 'red';
      default:
        return 'neutral';
    }
  });
}
