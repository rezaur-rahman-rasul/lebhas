import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { FormGroup, ReactiveFormsModule } from '@angular/forms';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import {
  CAMPAIGN_OBJECTIVE_OPTIONS,
  CREATIVE_STYLE_OPTIONS,
  PLATFORM_OPTIONS,
  PROMPT_LANGUAGE_OPTIONS,
  PROMPT_TONE_OPTIONS,
  PromptTemplate,
} from '../../models/prompt.models';

@Component({
  selector: 'app-prompt-editor',
  standalone: true,
  imports: [ReactiveFormsModule, BadgeComponent, CardComponent],
  templateUrl: './prompt-editor.html',
  styleUrl: './prompt-editor.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptEditor {
  readonly editorForm = input.required<FormGroup>();
  readonly templates = input<readonly PromptTemplate[]>([]);
  readonly canUseTemplates = input(false);
  readonly selectedAssetCount = input(0);

  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly campaignObjectiveOptions = CAMPAIGN_OBJECTIVE_OPTIONS;
  protected readonly creativeStyleOptions = CREATIVE_STYLE_OPTIONS;
  protected readonly languageOptions = PROMPT_LANGUAGE_OPTIONS;
  protected readonly toneOptions = PROMPT_TONE_OPTIONS;
}
