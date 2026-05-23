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
  selector: 'app-asset-card',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ButtonComponent, CardComponent, IconComponent],
  templateUrl: './asset-card.html',
  styleUrl: './asset-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetCard {
  readonly asset = input.required<Asset>();
  readonly canDownload = input(false);
  readonly canDelete = input(false);
  readonly deleting = input(false);

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
