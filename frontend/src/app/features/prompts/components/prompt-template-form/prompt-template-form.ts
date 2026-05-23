import { ChangeDetectionStrategy, Component, computed, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  CAMPAIGN_OBJECTIVE_OPTIONS,
  PLATFORM_OPTIONS,
  PROMPT_LANGUAGE_OPTIONS,
  PROMPT_TEMPLATE_STATUS_OPTIONS,
} from '../../models/prompt-options';
import {
  CampaignObjective,
  CreatePromptTemplateRequest,
  PROMPT_MAX_LENGTH,
  PROMPT_MIN_LENGTH,
  PROMPT_TEMPLATE_NAME_MAX_LENGTH,
  PromptLanguage,
  PromptPlatform,
  PromptTemplate,
  PromptTemplateStatus,
} from '../../models';
import { supportedPromptOptionValidator } from '../../prompt-form.validators';
import { ButtonComponent } from '@app/shared/components/button/button';

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
  readonly disabled = input(false);
  readonly fieldErrors = input<Readonly<Record<string, string>>>({});

  readonly submitted = output<CreatePromptTemplateRequest>();
  readonly cancelled = output<void>();

  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly campaignObjectiveOptions = CAMPAIGN_OBJECTIVE_OPTIONS;
  protected readonly languageOptions = PROMPT_LANGUAGE_OPTIONS;
  protected readonly statusOptions = PROMPT_TEMPLATE_STATUS_OPTIONS;
  protected readonly minLength = PROMPT_MIN_LENGTH;
  protected readonly maxLength = PROMPT_MAX_LENGTH;

  protected readonly form = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(PROMPT_TEMPLATE_NAME_MAX_LENGTH)],
    }),
    description: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(500)] }),
    category: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(80)] }),
    platform: new FormControl<PromptPlatform | ''>('', {
      nonNullable: true,
      validators: [Validators.required, supportedPromptOptionValidator(PLATFORM_OPTIONS)],
    }),
    campaignObjective: new FormControl<CampaignObjective | ''>('', {
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
    promptBody: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(PROMPT_MIN_LENGTH), Validators.maxLength(PROMPT_MAX_LENGTH)],
    }),
    isDefault: new FormControl(false, { nonNullable: true }),
    status: new FormControl<PromptTemplateStatus>('ACTIVE', {
      nonNullable: true,
      validators: [supportedPromptOptionValidator(PROMPT_TEMPLATE_STATUS_OPTIONS)],
    }),
  });

  protected readonly promptBodyLength = computed(() => this.form.controls.promptBody.value.length);

  constructor() {
    effect(() => {
      const template = this.template();
      const readOnly = this.disabled();

      this.form.reset(
        template
          ? {
              name: template.name,
              description: template.description ?? '',
              category: template.category ?? '',
              platform: template.platform,
              campaignObjective: template.campaignObjective,
              businessType: template.businessType ?? '',
              language: template.language,
              promptBody: template.promptBody,
              isDefault: template.isDefault,
              status: template.status,
            }
          : {
              name: '',
              description: '',
              category: '',
              platform: '',
              campaignObjective: '',
              businessType: '',
              language: 'ENGLISH',
              promptBody: '',
              isDefault: false,
              status: 'ACTIVE',
            },
        { emitEvent: false },
      );

      if (readOnly) {
        this.form.disable({ emitEvent: false });
      } else {
        this.form.enable({ emitEvent: false });
      }
    });
  }

  protected submit(): void {
    if (this.disabled() || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    this.submitted.emit({
      name: value.name.trim(),
      category: value.category.trim() || null,
      description: value.description.trim() || null,
      platform: value.platform as PromptPlatform,
      campaignObjective: value.campaignObjective as CampaignObjective,
      businessType: value.businessType.trim() || null,
      language: value.language as PromptLanguage,
      promptBody: value.promptBody.trim(),
      isDefault: value.isDefault,
      status: value.status,
    });
  }
}
