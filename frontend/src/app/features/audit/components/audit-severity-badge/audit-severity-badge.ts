import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';
import { AuditSeverity } from '../../models/audit.models';

@Component({
  selector: 'app-audit-severity-badge',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './audit-severity-badge.html',
  styleUrl: './audit-severity-badge.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditSeverityBadgeComponent {
  readonly severity = input<AuditSeverity | string>(AuditSeverity.Info);

  protected readonly label = computed(() => auditSeverityLabel(this.severity()));
  protected readonly icon = computed(() => auditSeverityIcon(this.severity()));
  protected readonly classes = computed(() => {
    const base =
      'inline-flex max-w-full items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium';

    switch (this.severity()) {
      case AuditSeverity.Critical:
        return `${base} border-alert-500/30 bg-alert-500/10 text-alert-700 dark:text-alert-300`;
      case AuditSeverity.Error:
        return `${base} border-alert-500/25 bg-alert-500/10 text-alert-700 dark:text-alert-300`;
      case AuditSeverity.Warning:
        return `${base} border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300`;
      default:
        return `${base} border-border bg-panel text-muted`;
    }
  });
}

export function auditSeverityLabel(severity: AuditSeverity | string): string {
  switch (severity) {
    case AuditSeverity.Warning:
      return 'Warning';
    case AuditSeverity.Error:
      return 'Error';
    case AuditSeverity.Critical:
      return 'Critical';
    default:
      return 'Info';
  }
}

function auditSeverityIcon(severity: AuditSeverity | string): string {
  switch (severity) {
    case AuditSeverity.Warning:
      return 'triangle-alert';
    case AuditSeverity.Error:
    case AuditSeverity.Critical:
      return 'circle-alert';
    default:
      return 'info';
  }
}
