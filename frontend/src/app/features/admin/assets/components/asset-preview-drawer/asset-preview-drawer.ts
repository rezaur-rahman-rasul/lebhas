import { DatePipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
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
  selector: 'app-asset-preview-drawer',
  standalone: true,
  imports: [DatePipe, KeyValuePipe, BadgeComponent, ButtonComponent, IconComponent],
  templateUrl: './asset-preview-drawer.html',
  styleUrl: './asset-preview-drawer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetPreviewDrawer {
  readonly open = input(false);
  readonly asset = input<Asset | null>(null);
  readonly previewUrl = input<string | null>(null);
  readonly previewLoading = input(false);
  readonly downloading = input(false);
  readonly deleting = input(false);
  readonly canDownload = input(false);
  readonly canDelete = input(false);

  readonly closed = output<void>();
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
