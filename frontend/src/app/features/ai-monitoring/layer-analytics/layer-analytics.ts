import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { EmptyStateComponent } from '@app/shared/components/app-empty-state/app-empty-state';
import { AppErrorStateComponent } from '@app/shared/components/app-error-state/app-error-state';
import { PageHeaderComponent } from '@app/shared/components/app-page-header/app-page-header';
import { BadgeComponent } from '@app/shared/components/app-status-badge/app-status-badge';
import {
  AiAnalyticsFilterBarComponent,
  AiAnalyticsFilterOption,
} from '../components/ai-analytics-filter-bar/ai-analytics-filter-bar';
import { AiMonitoringEmptyStateComponent } from '../components/ai-monitoring-empty-state/ai-monitoring-empty-state';
import { AiMonitoringLoadingStateComponent } from '../components/ai-monitoring-loading-state/ai-monitoring-loading-state';
import { LayerCostCardComponent } from '../components/layer-cost-card/layer-cost-card';
import { AiLayerAnalytics } from '../models/ai-monitoring.models';
import { AiMonitoringStore } from '../state/ai-monitoring.store';

@Component({
  selector: 'app-layer-analytics-page',
  standalone: true,
  imports: [
    CurrencyPipe,
    DecimalPipe,
    RouterLink,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    AppErrorStateComponent,
    PageHeaderComponent,
    BadgeComponent,
    AiAnalyticsFilterBarComponent,
    AiMonitoringEmptyStateComponent,
    AiMonitoringLoadingStateComponent,
    LayerCostCardComponent,
  ],
  templateUrl: './layer-analytics.html',
  styleUrl: './layer-analytics.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LayerAnalyticsPage implements OnInit {
  protected readonly permissions = inject(PermissionStore);
  protected readonly store = inject(AiMonitoringStore);

  protected readonly providerOptions = computed<readonly AiAnalyticsFilterOption[]>(() =>
    uniqueOptions(
      this.store.layerAnalytics().map((layer) => ({
        id: layer.providerId,
        label: layer.providerName,
      })),
    ),
  );

  protected readonly layerOptions = computed<readonly AiAnalyticsFilterOption[]>(() =>
    uniqueOptions(
      this.store.layerAnalytics().map((layer) => ({
        id: layer.layerId,
        label: layer.layerName,
      })),
    ),
  );

  protected readonly filteredLayers = computed(() => {
    const selectedProvider = this.store.selectedProvider();
    const selectedLayer = this.store.selectedLayer();

    return this.store
      .layerAnalytics()
      .filter((layer) => !selectedProvider || layer.providerId === selectedProvider)
      .filter((layer) => !selectedLayer || layer.layerId === selectedLayer);
  });

  protected readonly totalExecutions = computed(() =>
    this.filteredLayers().reduce((total, layer) => total + layer.totalExecutions, 0),
  );

  protected readonly successfulExecutions = computed(() =>
    this.filteredLayers().reduce((total, layer) => total + layer.successfulExecutions, 0),
  );

  protected readonly failureCount = computed(() =>
    this.filteredLayers().reduce((total, layer) => total + layer.failedExecutions, 0),
  );

  protected readonly totalCost = computed(() =>
    this.filteredLayers().reduce(
      (total, layer) => total + layer.avgExecutionCostUsd * layer.totalExecutions,
      0,
    ),
  );

  protected readonly averageExecutionTime = computed(() =>
    average(this.filteredLayers().map((layer) => layer.avgExecutionTimeMs)),
  );

  protected readonly averageQuality = computed(() =>
    average(this.filteredLayers().map((layer) => layer.avgQualityScore)),
  );

  protected readonly costlyLayers = computed(() =>
    topIssueLayers(this.filteredLayers(), (layer) => layer.avgExecutionCostUsd * layer.totalExecutions),
  );

  protected readonly slowLayers = computed(() =>
    topIssueLayers(this.filteredLayers(), (layer) => layer.avgExecutionTimeMs),
  );

  protected readonly failingLayers = computed(() =>
    this.filteredLayers()
      .filter((layer) => layer.failedExecutions > 0)
      .sort((a, b) => b.failedExecutions - a.failedExecutions)
      .slice(0, 3),
  );

  ngOnInit(): void {
    if (this.permissions.canViewLayerAnalytics()) {
      void this.store.loadLayerAnalytics();
    }
  }

  protected refresh(): void {
    if (this.permissions.canViewLayerAnalytics()) {
      void this.store.loadLayerAnalytics();
    }
  }

  protected layerCost(layer: AiLayerAnalytics): number {
    return layer.avgExecutionCostUsd * layer.totalExecutions;
  }
}

function average(values: readonly number[]): number | null {
  const validValues = values.filter((value) => Number.isFinite(value));
  return validValues.length
    ? validValues.reduce((total, value) => total + value, 0) / validValues.length
    : null;
}

function uniqueOptions(options: readonly AiAnalyticsFilterOption[]): readonly AiAnalyticsFilterOption[] {
  const seen = new Set<string>();
  return options.filter((option) => {
    if (seen.has(option.id)) {
      return false;
    }

    seen.add(option.id);
    return true;
  });
}

function topIssueLayers(
  layers: readonly AiLayerAnalytics[],
  selector: (layer: AiLayerAnalytics) => number,
): readonly AiLayerAnalytics[] {
  return [...layers]
    .filter((layer) => selector(layer) > 0)
    .sort((a, b) => selector(b) - selector(a))
    .slice(0, 3);
}
