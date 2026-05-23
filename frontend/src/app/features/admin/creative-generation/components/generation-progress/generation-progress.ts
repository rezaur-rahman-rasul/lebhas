import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  CreativeGenerationRequest,
  creativeGenerationStatusLabel,
  creativeGenerationStatusTone,
  generationProgressPercent,
} from '../../models/creative-generation.models';

@Component({
  selector: 'app-generation-progress',
  standalone: true,
  imports: [DatePipe, BadgeComponent, ButtonComponent, CardComponent, IconComponent],
  templateUrl: './generation-progress.html',
  styleUrl: './generation-progress.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenerationProgress {
  readonly request = input<CreativeGenerationRequest | null>(null);
  readonly loading = input(false);
  readonly canRetry = input(false);
  readonly canCancel = input(false);

  readonly retryRequested = output<void>();
  readonly cancelRequested = output<void>();
  readonly refreshRequested = output<void>();

  protected readonly statusLabel = creativeGenerationStatusLabel;
  protected readonly statusTone = creativeGenerationStatusTone;
  protected readonly progressPercent = generationProgressPercent;
}
