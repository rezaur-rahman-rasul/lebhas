import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';

@Component({
  selector: 'app-usage-empty-state',
  standalone: true,
  imports: [ButtonComponent, EmptyStateComponent],
  templateUrl: './usage-empty-state.html',
  styleUrl: './usage-empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsageEmptyStateComponent {
  readonly title = input('No usage history yet');
  readonly description = input(
    'Once creatives are generated or downloaded, usage will appear here.',
  );
  readonly icon = input('receipt-text');
  readonly retryLabel = input<string | null>(null);
  readonly retry = output<void>();
}
