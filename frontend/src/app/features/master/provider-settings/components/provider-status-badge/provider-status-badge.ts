import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';

@Component({
  selector: 'app-provider-status-badge',
  standalone: true,
  imports: [BadgeComponent],
  templateUrl: './provider-status-badge.html',
  styleUrl: './provider-status-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProviderStatusBadgeComponent {
  readonly active = input(false);
}
