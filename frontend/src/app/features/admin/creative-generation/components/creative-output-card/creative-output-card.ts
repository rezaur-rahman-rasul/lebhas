import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { formatFileSize } from '@app/features/admin/assets/models/asset.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  CreativeOutput,
  creativeGenerationStatusLabel,
  creativeGenerationStatusTone,
  creativeOutputFormatLabel,
  creativeTypeLabel,
  formatOutputDimensions,
  isImageFormat,
} from '../../models/creative-generation.models';
import { promptPlatformLabel } from '@app/features/admin/prompts/models/prompt.models';

@Component({
  selector: 'app-creative-output-card',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ButtonComponent, CardComponent, IconComponent],
  templateUrl: './creative-output-card.html',
  styleUrl: './creative-output-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreativeOutputCard {
  readonly output = input.required<CreativeOutput>();
  readonly canDownload = input(false);

  readonly previewRequested = output<CreativeOutput>();
  readonly downloadRequested = output<CreativeOutput>();
  readonly detailRequested = output<CreativeOutput>();

  protected readonly statusLabel = creativeGenerationStatusLabel;
  protected readonly statusTone = creativeGenerationStatusTone;
  protected readonly creativeTypeLabel = creativeTypeLabel;
  protected readonly outputFormatLabel = creativeOutputFormatLabel;
  protected readonly promptPlatformLabel = promptPlatformLabel;
  protected readonly formatOutputDimensions = formatOutputDimensions;
  protected readonly isImageFormat = isImageFormat;

  protected fileSizeLabel(size: number | null): string {
    return size ? formatFileSize(size) : 'Size pending';
  }
}
