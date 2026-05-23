import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

import { CardComponent } from '@app/shared/components/card/card';
import {
  CAMPAIGN_OBJECTIVE_OPTIONS,
  PLATFORM_OPTIONS,
  PROMPT_LANGUAGE_OPTIONS,
} from '@app/features/admin/prompts/models/prompt.models';
import {
  CREATIVE_OUTPUT_FORMAT_OPTIONS,
  IMAGE_SIZE_OPTIONS,
} from '../../models/creative-generation.models';

@Component({
  selector: 'app-platform-format-selector',
  standalone: true,
  imports: [ReactiveFormsModule, CardComponent],
  templateUrl: './platform-format-selector.html',
  styleUrl: './platform-format-selector.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlatformFormatSelector {
  readonly form = input.required<FormGroup>();
  readonly disabled = input(false);

  readonly sizeSelected = output<{ width: number; height: number }>();

  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly objectiveOptions = CAMPAIGN_OBJECTIVE_OPTIONS;
  protected readonly languageOptions = PROMPT_LANGUAGE_OPTIONS;
  protected readonly outputFormatOptions = CREATIVE_OUTPUT_FORMAT_OPTIONS;
  protected readonly imageSizeOptions = IMAGE_SIZE_OPTIONS;
}
