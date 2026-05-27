import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AuditSeverity, DateRangeFilter } from '../../models/audit.models';

@Component({
  selector: 'app-audit-filter-bar',
  standalone: true,
  imports: [ButtonComponent],
  templateUrl: './audit-filter-bar.html',
  styleUrl: './audit-filter-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuditFilterBarComponent {
  readonly selectedModule = input<string | null>(null);
  readonly selectedSeverity = input<AuditSeverity | string | null>(null);
  readonly selectedAction = input<string | null>(null);
  readonly selectedActor = input<string | null>(null);
  readonly selectedDateRange = input<DateRangeFilter | null>(null);
  readonly loading = input(false);

  readonly selectedModuleChange = output<string | null>();
  readonly selectedSeverityChange = output<AuditSeverity | string | null>();
  readonly selectedActionChange = output<string | null>();
  readonly selectedActorChange = output<string | null>();
  readonly selectedDateRangeChange = output<DateRangeFilter | null>();
  readonly refresh = output<void>();
  readonly clear = output<void>();

  protected readonly severities = Object.values(AuditSeverity);

  protected updateText(value: string): string | null {
    return value.trim() || null;
  }

  protected updateFrom(value: string): void {
    const current = this.selectedDateRange();
    this.selectedDateRangeChange.emit({
      from: value || null,
      to: current?.to ?? null,
    });
  }

  protected updateTo(value: string): void {
    const current = this.selectedDateRange();
    this.selectedDateRangeChange.emit({
      from: current?.from ?? null,
      to: value || null,
    });
  }
}
