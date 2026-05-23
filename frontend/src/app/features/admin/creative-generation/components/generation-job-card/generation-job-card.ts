import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import {
  creativeGenerationStatusLabel,
  creativeGenerationStatusTone,
  GenerationJob,
} from '../../models/creative-generation.models';

@Component({
  selector: 'app-generation-job-card',
  standalone: true,
  imports: [DatePipe, BadgeComponent, CardComponent],
  templateUrl: './generation-job-card.html',
  styleUrl: './generation-job-card.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenerationJobCard {
  readonly job = input.required<GenerationJob>();

  protected readonly statusLabel = creativeGenerationStatusLabel;
  protected readonly statusTone = creativeGenerationStatusTone;
}
