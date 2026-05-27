import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import { AiFailureLog, AiFailureType } from '../../models/ai-monitoring.models';

@Component({
  selector: 'app-failure-reason-card',
  standalone: true,
  imports: [CardComponent, IconComponent, BadgeComponent],
  templateUrl: './failure-reason-card.html',
  styleUrl: './failure-reason-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FailureReasonCardComponent {
  readonly failure = input.required<AiFailureLog>();

  protected readonly failureType = computed(() => friendlyFailureType(this.failure().failureType));

  protected readonly reason = computed(() => friendlyFailureReason(this.failure()));

  protected readonly fallbackMessage = computed(() =>
    this.failure().fallbackTriggered ? 'Backup tool was used.' : 'No backup tool was used.',
  );
}

function friendlyFailureType(type: AiFailureType | string): string {
  return String(type)
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function friendlyFailureReason(failure: AiFailureLog): string {
  switch (failure.failureType) {
    case AiFailureType.Timeout:
      return 'This tool took too long to respond.';
    case AiFailureType.RateLimit:
      return 'This tool is temporarily busy.';
    case AiFailureType.ProviderDown:
      return 'This tool is currently unavailable.';
    case AiFailureType.InvalidResponse:
      return 'This tool returned an unusable result.';
    case AiFailureType.QualityFailure:
      return 'Output quality was not good enough.';
    case AiFailureType.CostLimitExceeded:
      return 'This request exceeded the allowed AI cost.';
    case AiFailureType.Unknown:
    default:
      return 'Something went wrong while generating this creative.';
  }
}
