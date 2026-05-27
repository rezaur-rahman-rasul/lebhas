import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-audit-loading-state',
  standalone: true,
  templateUrl: './audit-loading-state.html',
  styleUrl: './audit-loading-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditLoadingStateComponent {
  readonly label = input('Loading audit logs');
  protected readonly rows = Array.from({ length: 6 }, (_, index) => index);
}
