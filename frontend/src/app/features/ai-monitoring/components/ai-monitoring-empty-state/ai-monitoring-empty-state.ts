import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';

@Component({
  selector: 'app-ai-monitoring-empty-state',
  standalone: true,
  imports: [ButtonComponent, EmptyStateComponent],
  templateUrl: './ai-monitoring-empty-state.html',
  styleUrl: './ai-monitoring-empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiMonitoringEmptyStateComponent {
  readonly title = input('No AI usage data yet');
  readonly description = input(
    'Once creatives are generated, analytics will appear here.',
  );
  readonly retryLabel = input<string | null>(null);
  readonly retry = output<void>();
}
