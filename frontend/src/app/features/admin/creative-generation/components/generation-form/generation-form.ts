import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output, signal } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

import { Asset, formatFileSize, isImageAsset } from '@app/features/admin/assets/models/asset.models';
import { PromptHistory } from '@app/features/admin/prompts/models/prompt.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { CreativeTypeSelector } from '../creative-type-selector/creative-type-selector';
import { PlatformFormatSelector } from '../platform-format-selector/platform-format-selector';

@Component({
  selector: 'app-generation-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    DatePipe,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    IconComponent,
    CreativeTypeSelector,
    PlatformFormatSelector,
  ],
  templateUrl: './generation-form.html',
  styleUrl: './generation-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenerationForm {
  readonly form = input.required<FormGroup>();
  readonly promptHistory = input<readonly PromptHistory[]>([]);
  readonly availableAssets = input<readonly Asset[]>([]);
  readonly selectedAssets = input<readonly Asset[]>([]);
  readonly loading = input(false);
  readonly disabled = input(false);
  readonly canSubmit = input(false);

  readonly submitted = output<void>();
  readonly promptHistorySelected = output<string | null>();
  readonly assetSearchSubmitted = output<string>();
  readonly assetToggled = output<Asset>();
  readonly assetRemoved = output<string>();
  readonly sizeSelected = output<{ width: number; height: number }>();

  protected readonly searchValue = signal('');
  protected readonly formatFileSize = formatFileSize;
  protected readonly isImageAsset = isImageAsset;

  protected isSelected(assetId: string): boolean {
    return this.selectedAssets().some((asset) => asset.id === assetId);
  }

  protected submitAssetSearch(): void {
    this.assetSearchSubmitted.emit(this.searchValue().trim());
  }
}
