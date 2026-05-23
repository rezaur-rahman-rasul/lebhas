import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { Asset, formatFileSize, isImageAsset } from '@app/features/admin/assets/models/asset.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-selected-assets-summary',
  standalone: true,
  imports: [BadgeComponent, CardComponent, IconComponent],
  templateUrl: './selected-assets-summary.html',
  styleUrl: './selected-assets-summary.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SelectedAssetsSummary {
  readonly assets = input<readonly Asset[]>([]);
  readonly removable = input(false);

  readonly assetRemoved = output<string>();

  protected readonly formatFileSize = formatFileSize;
  protected readonly isImageAsset = isImageAsset;
}
