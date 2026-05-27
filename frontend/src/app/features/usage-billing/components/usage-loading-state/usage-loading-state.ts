import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { LoadingComponent } from '@app/shared/components/app-loading-state/app-loading-state';

@Component({
  selector: 'app-usage-loading-state',
  standalone: true,
  imports: [LoadingComponent],
  templateUrl: './usage-loading-state.html',
  styleUrl: './usage-loading-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsageLoadingStateComponent {
  readonly label = input('Loading usage and billing data');
  readonly cardCount = input(4);
  readonly listCount = input(3);

  protected readonly cardItems = computed(() =>
    Array.from({ length: Math.max(1, this.cardCount()) }, (_, index) => index),
  );

  protected readonly listItems = computed(() =>
    Array.from({ length: Math.max(1, this.listCount()) }, (_, index) => index),
  );
}
