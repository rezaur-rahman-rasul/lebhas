import { ChangeDetectionStrategy, Component } from '@angular/core';

import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-credit-lifecycle-timeline',
  standalone: true,
  imports: [CardComponent, IconComponent],
  templateUrl: './credit-lifecycle-timeline.html',
  styleUrl: './credit-lifecycle-timeline.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreditLifecycleTimelineComponent {
  protected readonly steps = [
    {
      title: 'Reserve Credits',
      description: 'Credits are temporarily held when generation starts.',
      icon: 'lock-keyhole',
    },
    {
      title: 'Generate Creative',
      description: 'The AI tools create the requested creative.',
      icon: 'sparkles',
    },
    {
      title: 'Finalize on Success',
      description: 'Credits are used when generation completes successfully.',
      icon: 'check-circle-2',
    },
    {
      title: 'Refund on Failure',
      description: 'Credits are returned if generation fails.',
      icon: 'rotate-ccw',
    },
  ] as const;
}
