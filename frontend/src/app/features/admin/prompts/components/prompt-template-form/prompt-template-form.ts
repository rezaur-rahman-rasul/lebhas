import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/button/button';
import {
  CAMPAIGN_OBJECTIVE_OPTIONS,
  PLATFORM_OPTIONS,
  PROMPT_LANGUAGE_OPTIONS,
  PROMPT_TEMPLATE_NAME_MAX_LENGTH,
  PROMPT_TEMPLATE_STATUS_OPTIONS,
  PromptLanguage,
  PromptPlatform,
  PromptTemplate,
  PromptTemplatePayload,
  PromptTemplateStatus,
} from '../../models/prompt.models';
import { supportedPromptOptionValidator } from '../../prompt-form.validators';

@Component({
  selector: 'app-prompt-template-form',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent],
  templateUrl: './prompt-template-form.html',
  styleUrl: './prompt-template-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptTemplateForm {
  readonly template = input<PromptTemplate | null>(null);
  readonly loading = input(false);
  readonly fieldErrors = input<Readonly<Record<string, string>>>({});

  readonly submitted = output<PromptTemplatePayload>();
  readonly cancelled = output<void>();

  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly campaignObjectiveOptions = CAMPAIGN_OBJECTIVE_OPTIONS;
  protected readonly languageOptions = PROMPT_LANGUAGE_OPTIONS;
  protected readonly statusOptions = PROMPT_TEMPLATE_STATUS_OPTIONS;
  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(PROMPT_TEMPLATE_NAME_MAX_LENGTH)],
    }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    platform: new FormControl<PromptPlatform | ''>('', {
      nonNullable: true,
      validators: [Validators.required, supportedPromptOptionValidator(PLATFORM_OPTIONS)],
    }),
    campaignObjective: new FormControl<PromptTemplatePayload['campaignObjective'] | ''>('', {
      nonNullable: true,
      validators: [
        Validators.required,
        supportedPromptOptionValidator(CAMPAIGN_OBJECTIVE_OPTIONS),
      ],
    }),
    businessType: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(80)] }),
    language: new FormControl<PromptLanguage | ''>('', {
      nonNullable: true,
      validators: [Validators.required, supportedPromptOptionValidator(PROMPT_LANGUAGE_OPTIONS)],
    }),
    templateText: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(5), Validators.maxLength(5000)],
    }),
    isSystemDefault: new FormControl(false, { nonNullable: true }),
    status: new FormControl<PromptTemplateStatus>('ACTIVE', {
      nonNullable: true,
      validators: [supportedPromptOptionValidator(PROMPT_TEMPLATE_STATUS_OPTIONS)],
    }),
  });

  constructor() {
    effect(() => {
      const template = this.template();
      this.form.reset(
        template
          ? {
              name: template.name,
              description: template.description ?? '',
              platform: template.platform,
              campaignObjective: template.campaignObjective,
              businessType: template.businessType ?? '',
              language: template.language,
              templateText: template.templateText,
              isSystemDefault: template.isSystemDefault,
              status: template.status,
            }
          : {
              name: '',
              description: '',
              platform: '',
              campaignObjective: '',
              businessType: '',
              language: '',
              templateText: '',
              isSystemDefault: false,
              status: 'ACTIVE',
            },
        { emitEvent: false },
      );
    });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitted.emit({
      name: value.name.trim(),
      description: value.description.trim() || null,
      platform: value.platform as PromptTemplatePayload['platform'],
      campaignObjective: value.campaignObjective as PromptTemplatePayload['campaignObjective'],
      businessType: value.businessType.trim() || null,
      language: value.language as PromptTemplatePayload['language'],
      templateText: value.templateText.trim(),
      isSystemDefault: value.isSystemDefault,
      status: value.status,
    });
  }
}
