import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';

@Component({
  selector: 'app-ai-monitoring-shell',
  standalone: true,
  imports: [RouterLink, BadgeComponent, EmptyStateComponent, IconComponent, PageHeaderComponent],
  templateUrl: './ai-monitoring-shell.html',
  styleUrl: './ai-monitoring-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiMonitoringShellComponent {
  readonly allowed = input(false);
  readonly eyebrow = input('AI monitoring');
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly icon = input('activity');
  readonly badge = input('Backend-driven');
  readonly accessTitle = input('AI monitoring access is restricted');
  readonly accessDescription = input(
    'You do not have permission to view this AI monitoring area. Ask a workspace owner to update your access.',
  );
}
