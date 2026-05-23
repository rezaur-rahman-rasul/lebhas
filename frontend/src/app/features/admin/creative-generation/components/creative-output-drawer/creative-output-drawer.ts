import { DatePipe, KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { formatFileSize } from '@app/features/admin/assets/models/asset.models';
import { promptPlatformLabel } from '@app/features/admin/prompts/models/prompt.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  CreativeGenerationRequest,
  CreativeOutput,
  creativeGenerationStatusLabel,
  creativeGenerationStatusTone,
  creativeOutputFormatLabel,
  creativeTypeLabel,
  formatOutputDimensions,
  formatOutputDuration,
  isImageFormat,
} from '../../models/creative-generation.models';

@Component({
  selector: 'app-creative-output-drawer',
  standalone: true,
  imports: [DatePipe, KeyValuePipe, BadgeComponent, ButtonComponent, IconComponent],
  templateUrl: './creative-output-drawer.html',
  styleUrl: './creative-output-drawer.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreativeOutputDrawer {
  readonly output = input<CreativeOutput | null>(null);
  readonly request = input<CreativeGenerationRequest | null>(null);
  readonly open = input(false);
  readonly canDownload = input(false);

  readonly closed = output<void>();
  readonly previewRequested = output<CreativeOutput>();
  readonly downloadRequested = output<CreativeOutput>();

  protected readonly statusLabel = creativeGenerationStatusLabel;
  protected readonly statusTone = creativeGenerationStatusTone;
  protected readonly creativeTypeLabel = creativeTypeLabel;
  protected readonly outputFormatLabel = creativeOutputFormatLabel;
  protected readonly promptPlatformLabel = promptPlatformLabel;
  protected readonly formatOutputDimensions = formatOutputDimensions;
  protected readonly formatOutputDuration = formatOutputDuration;
  protected readonly isImageFormat = isImageFormat;

  protected fileSizeLabel(size: number | null): string {
    return size ? formatFileSize(size) : 'Size pending';
  }
}
