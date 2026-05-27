import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';
import { AiProviderHealth } from '../../models/ai-monitoring.models';
import { ProviderStatusBadgeComponent } from '../provider-status-badge/provider-status-badge';

@Component({
  selector: 'app-ai-health-card',
  standalone: true,
  imports: [CardComponent, IconComponent, ProviderStatusBadgeComponent],
  templateUrl: './ai-health-card.html',
  styleUrl: './ai-health-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiHealthCardComponent {
  readonly health = input.required<AiProviderHealth>();

  protected readonly message = computed(
    () =>
      this.health().recommendedAction ||
      friendlyMonitoringMessage(this.health().failureReason) ||
      'Provider health is being monitored.',
  );
}

function friendlyMonitoringMessage(message: string | null): string | null {
  if (!message) {
    return null;
  }

  const normalized = message.toLowerCase();

  if (normalized.includes('latency') && normalized.includes('p95')) {
    return 'This provider is slower than usual.';
  }

  if (normalized.includes('failure rate') && normalized.includes('threshold')) {
    return 'This tool failed too many times recently.';
  }

  if (normalized.includes('cost anomaly')) {
    return 'AI cost increased unusually for this workspace.';
  }

  return message;
}
