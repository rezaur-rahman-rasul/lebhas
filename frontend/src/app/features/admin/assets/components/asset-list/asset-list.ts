import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  Asset,
  assetCategoryLabel,
  assetFileTypeLabel,
  assetStatusLabel,
  assetStatusTone,
  formatFileSize,
} from '../../models/asset.models';

@Component({
  selector: 'app-asset-list',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ButtonComponent, CardComponent, IconComponent],
  templateUrl: './asset-list.html',
  styleUrl: './asset-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetList {
  readonly assets = input.required<readonly Asset[]>();
  readonly canDownload = input(false);
  readonly canDelete = input(false);
  readonly deletingAssetId = input<string | null>(null);

  readonly previewRequested = output<Asset>();
  readonly downloadRequested = output<Asset>();
  readonly deleteRequested = output<Asset>();
  readonly detailRequested = output<Asset>();

  protected readonly assetCategoryLabel = assetCategoryLabel;
  protected readonly assetFileTypeLabel = assetFileTypeLabel;
  protected readonly assetStatusLabel = assetStatusLabel;
  protected readonly assetStatusTone = assetStatusTone;
  protected readonly formatFileSize = formatFileSize;
}
