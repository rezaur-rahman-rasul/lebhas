import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';

@Component({
  selector: 'app-routing-mode-badge',
  standalone: true,
  imports: [BadgeComponent],
  templateUrl: './routing-mode-badge.html',
  styleUrl: './routing-mode-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoutingModeBadgeComponent {
  readonly mode = input<string | null>(null);

  protected readonly label = computed(() => this.mode() || 'Routing mode pending');
}
