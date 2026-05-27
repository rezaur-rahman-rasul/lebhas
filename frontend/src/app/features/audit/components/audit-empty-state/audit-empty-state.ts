import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-audit-empty-state',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './audit-empty-state.html',
  styleUrl: './audit-empty-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditEmptyStateComponent {
  readonly icon = input('shield-check');
  readonly title = input('No audit events yet');
  readonly description = input('Important system actions will appear here.');
}
