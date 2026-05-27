import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LoadingComponent } from '@app/shared/components/app-loading-state/app-loading-state';

@Component({
  selector: 'app-ai-monitoring-loading-state',
  standalone: true,
  imports: [LoadingComponent],
  templateUrl: './ai-monitoring-loading-state.html',
  styleUrl: './ai-monitoring-loading-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiMonitoringLoadingStateComponent {
  readonly label = input('Loading AI monitoring data');
  readonly cardCount = input(4);

  protected readonly skeletonItems = computed(() =>
    Array.from({ length: Math.max(1, this.cardCount()) }, (_, index) => index),
  );
}
