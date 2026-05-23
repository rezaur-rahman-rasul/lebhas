import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { Asset, formatFileSize, isImageAsset } from '@app/features/admin/assets/models/asset.models';

@Component({
  selector: 'app-asset-context-picker',
  standalone: true,
  imports: [BadgeComponent, ButtonComponent, CardComponent, EmptyStateComponent, IconComponent],
  templateUrl: './asset-context-picker.html',
  styleUrl: './asset-context-picker.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetContextPicker {
  readonly assets = input<readonly Asset[]>([]);
  readonly selectedAssets = input<readonly Asset[]>([]);
  readonly loading = input(false);
  readonly disabled = input(false);

  readonly searchSubmitted = output<string>();
  readonly assetToggled = output<Asset>();
  readonly assetRemoved = output<string>();

  protected readonly searchValue = signal('');
  protected readonly formatFileSize = formatFileSize;

  protected isSelected(assetId: string): boolean {
    return this.selectedAssets().some((asset) => asset.id === assetId);
  }

  protected isImageAsset = isImageAsset;

  protected submitSearch(): void {
    this.searchSubmitted.emit(this.searchValue().trim());
  }
}
