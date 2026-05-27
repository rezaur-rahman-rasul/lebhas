import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { AiMonitoringDateRange } from '../../models/ai-monitoring.models';

export interface AiAnalyticsFilterOption {
  readonly id: string;
  readonly label: string;
}

@Component({
  selector: 'app-ai-analytics-filter-bar',
  standalone: true,
  imports: [ButtonComponent],
  templateUrl: './ai-analytics-filter-bar.html',
  styleUrl: './ai-analytics-filter-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AiAnalyticsFilterBarComponent {
  readonly dateRange = input<AiMonitoringDateRange>({ from: null, to: null });
  readonly providers = input<readonly AiAnalyticsFilterOption[]>([]);
  readonly layers = input<readonly AiAnalyticsFilterOption[]>([]);
  readonly selectedProvider = input<string | null>(null);
  readonly selectedLayer = input<string | null>(null);
  readonly loading = input(false);

  readonly dateRangeChange = output<AiMonitoringDateRange>();
  readonly selectedProviderChange = output<string | null>();
  readonly selectedLayerChange = output<string | null>();
  readonly refresh = output<void>();
  readonly clear = output<void>();

  protected updateFrom(value: string): void {
    this.dateRangeChange.emit({ ...this.dateRange(), from: value || null });
  }

  protected updateTo(value: string): void {
    this.dateRangeChange.emit({ ...this.dateRange(), to: value || null });
  }

  protected updateProvider(value: string): void {
    this.selectedProviderChange.emit(value || null);
  }

  protected updateLayer(value: string): void {
    this.selectedLayerChange.emit(value || null);
  }
}
