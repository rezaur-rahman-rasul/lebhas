import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  AssetFilter,
  AssetFileType,
  AssetStatus,
  AssetViewMode,
  ASSET_CATEGORY_OPTIONS,
  ASSET_FILE_TYPE_OPTIONS,
  ASSET_STATUS_OPTIONS,
  DEFAULT_ASSET_FILTERS,
} from '../../models/asset.models';

interface FilterFormValue {
  readonly search: string;
  readonly assetCategory: AssetFilter['assetCategory'] | '';
  readonly fileType: AssetFileType | '';
  readonly status: AssetStatus | '';
  readonly tag: string;
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
  readonly selectedFolderName = input('All assets');
  readonly disabled = input(false);

  readonly filtersApplied = output<AssetFilter>();
  readonly filtersReset = output<void>();
  readonly viewModeChanged = output<AssetViewMode>();

  protected readonly categoryOptions = ASSET_CATEGORY_OPTIONS;
  protected readonly fileTypeOptions = ASSET_FILE_TYPE_OPTIONS;
  protected readonly statusOptions = ASSET_STATUS_OPTIONS;
  protected readonly filterForm = new FormGroup({
    search: new FormControl('', { nonNullable: true }),
    assetCategory: new FormControl<AssetFilter['assetCategory'] | ''>(''),
    fileType: new FormControl<AssetFileType | ''>(''),
    status: new FormControl<AssetStatus | ''>(''),
    tag: new FormControl('', { nonNullable: true }),
  });

  constructor() {
    effect(() => {
      const filters = this.filters();
      this.filterForm.patchValue(
        {
          search: filters.search,
          assetCategory: filters.assetCategory ?? '',
          fileType: filters.fileType ?? '',
          status: filters.status ?? '',
          tag: filters.tag,
        },
        { emitEvent: false },
      );
    });
  }

  protected apply(): void {
    const rawValue = this.filterForm.getRawValue() as FilterFormValue;
    this.filtersApplied.emit({
      ...DEFAULT_ASSET_FILTERS,
      ...this.filters(),
      folderId: this.filters().folderId,
      search: rawValue.search.trim(),
      assetCategory: rawValue.assetCategory || null,
      fileType: rawValue.fileType || null,
      status: rawValue.status || null,
      tag: rawValue.tag.trim(),
    });
  }

  protected reset(): void {
    this.filterForm.reset(
      {
        search: '',
        assetCategory: '',
        fileType: '',
        status: '',
        tag: '',
      },
      { emitEvent: false },
    );
    this.filtersReset.emit();
  }
}
