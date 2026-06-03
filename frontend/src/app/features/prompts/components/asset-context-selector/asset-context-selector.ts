import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import {
  Asset,
  assetCategoryLabel,
  assetTypeLabel,
  isImageAsset,
  isVideoAsset,
} from '@app/features/assets/models/asset.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PromptEmptyState } from '../prompt-empty-state/prompt-empty-state';

@Component({
  selector: 'app-asset-context-selector',
  standalone: true,
  imports: [BadgeComponent, ButtonComponent, CardComponent, IconComponent, PromptEmptyState],
  templateUrl: './asset-context-selector.html',
  styleUrl: './asset-context-selector.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetContextSelector {
  readonly assets = input<readonly Asset[]>([]);
  readonly selectedAssets = input<readonly Asset[]>([]);
  readonly disabled = input(false);
  readonly loading = input(false);

  readonly assetToggled = output<Asset>();
  readonly assetRemoved = output<string>();

  protected readonly assetCategoryLabel = assetCategoryLabel;
  protected readonly assetTypeLabel = assetTypeLabel;

  protected isSelected(assetId: string): boolean {
    return this.selectedAssets().some((asset) => asset.id === assetId);
  }

  protected assetIcon(asset: Asset): string {
    if (isImageAsset(asset)) {
      return 'image';
    }

    if (isVideoAsset(asset)) {
      return 'video';
    }

    return asset.assetType === 'DOCUMENT' ? 'file-text' : 'file';
  }
}
