import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { Asset } from '../../models/asset.models';
import { AssetCard } from '../asset-card/asset-card';

@Component({
  selector: 'app-asset-grid',
  standalone: true,
  imports: [AssetCard],
  templateUrl: './asset-grid.html',
  styleUrl: './asset-grid.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetGrid {
  readonly assets = input.required<readonly Asset[]>();
  readonly canDownload = input(false);
  readonly canDelete = input(false);
  readonly deletingAssetId = input<string | null>(null);

  readonly previewRequested = output<Asset>();
  readonly downloadRequested = output<Asset>();
  readonly deleteRequested = output<Asset>();
  readonly detailRequested = output<Asset>();
}
