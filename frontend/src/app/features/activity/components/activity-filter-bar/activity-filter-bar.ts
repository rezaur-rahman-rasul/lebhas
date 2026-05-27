import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { ActivityType, DateRangeFilter } from '../../models/activity.models';
import { friendlyActivityType } from '../activity-type-badge/activity-type-badge';

@Component({
  selector: 'app-activity-filter-bar',
  standalone: true,
  imports: [ButtonComponent],
  templateUrl: './activity-filter-bar.html',
  styleUrl: './activity-filter-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ActivityFilterBarComponent {
  readonly selectedActivityType = input<ActivityType | string | null>(null);
  readonly selectedActor = input<string | null>(null);
  readonly selectedDateRange = input<DateRangeFilter | null>(null);
  readonly loading = input(false);

  readonly selectedActivityTypeChange = output<ActivityType | string | null>();
  readonly selectedActorChange = output<string | null>();
  readonly selectedDateRangeChange = output<DateRangeFilter | null>();
  readonly refresh = output<void>();
  readonly clear = output<void>();

  protected readonly activityTypes = Object.values(ActivityType);

  protected updateType(value: string): void {
    this.selectedActivityTypeChange.emit(value || null);
  }

  protected updateActor(value: string): void {
    this.selectedActorChange.emit(value.trim() || null);
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

  protected typeLabel(value: string): string {
    return friendlyActivityType(value);
  }
}
