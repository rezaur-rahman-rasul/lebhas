import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  AssetFilter,
  AssetStatus,
  AssetType,
  AssetViewMode,
  ASSET_CATEGORY_OPTIONS,
  ASSET_STATUS_OPTIONS,
  ASSET_TYPE_OPTIONS,
  DEFAULT_ASSET_FILTERS,
} from '../../models/asset.models';

interface FilterFormValue {
  readonly search: string;
  readonly assetCategory: AssetFilter['assetCategory'] | '';
  readonly assetType: AssetType | '';
  readonly status: AssetStatus | '';
}

@Component({
  selector: 'app-asset-filter-bar',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, IconComponent],
  templateUrl: './asset-filter-bar.html',
  styleUrl: './asset-filter-bar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetFilterBar {
  readonly filters = input.required<AssetFilter>();
  readonly viewMode = input.required<AssetViewMode>();
  readonly contextLabel = input('Project assets');
  readonly disabled = input(false);

  readonly filtersApplied = output<AssetFilter>();
  readonly filtersReset = output<void>();
  readonly viewModeChanged = output<AssetViewMode>();

  protected readonly categoryOptions = ASSET_CATEGORY_OPTIONS;
  protected readonly assetTypeOptions = ASSET_TYPE_OPTIONS;
  protected readonly statusOptions = ASSET_STATUS_OPTIONS;
  protected readonly filterForm = new FormGroup({
    search: new FormControl('', { nonNullable: true }),
    assetCategory: new FormControl<AssetFilter['assetCategory'] | ''>(''),
    assetType: new FormControl<AssetType | ''>(''),
    status: new FormControl<AssetStatus | ''>(''),
  });

  constructor() {
    effect(() => {
      const filters = this.filters();
      this.filterForm.patchValue(
        {
          search: filters.search,
          assetCategory: filters.assetCategory ?? '',
          assetType: filters.assetType ?? '',
          status: filters.status ?? '',
        },
        { emitEvent: false },
      );
    });
  }

  protected apply(): void {
    const rawValue = this.filterForm.getRawValue() as FilterFormValue;
    this.filtersApplied.emit({
      ...DEFAULT_ASSET_FILTERS,
      search: rawValue.search.trim(),
      assetCategory: rawValue.assetCategory || null,
      assetType: rawValue.assetType || null,
      status: rawValue.status || null,
    });
  }

  protected reset(): void {
    this.filterForm.reset(
      {
        search: '',
        assetCategory: '',
        assetType: '',
        status: '',
      },
      { emitEvent: false },
    );
    this.filtersReset.emit();
  }
}
