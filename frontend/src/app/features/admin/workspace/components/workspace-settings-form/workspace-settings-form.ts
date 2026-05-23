import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import {
  Workspace,
  WorkspaceLanguage,
  WorkspaceSettings,
  WorkspaceSettingsFormValue,
  WorkspaceVisibility,
  WORKSPACE_LANGUAGE_OPTIONS,
  WORKSPACE_VISIBILITY_OPTIONS,
} from '../../models/workspace.models';
import { optionalUrlValidator } from '../../workspace-form.validators';

type WorkspaceSettingsControlName =
  | 'name'
  | 'description'
  | 'industry'
  | 'timezone'
  | 'language'
  | 'currency'
  | 'country'
  | 'logoUrl'
  | 'allowCrewDownload'
  | 'allowCrewPublish'
  | 'workspaceVisibility';

@Component({
  selector: 'app-workspace-settings-form',
  standalone: true,
  imports: [ReactiveFormsModule, BadgeComponent, ButtonComponent, CardComponent],
  templateUrl: './workspace-settings-form.html',
  styleUrl: './workspace-settings-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceSettingsFormComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  readonly workspace = input<Workspace | null>(null);
  readonly settings = input<WorkspaceSettings | null>(null);
  readonly readOnly = input(false);
  readonly submitting = input(false);
  readonly serverErrors = input<Readonly<Record<string, string>>>({});
  readonly saved = output<WorkspaceSettingsFormValue>();

  protected readonly submitted = signal(false);
  protected readonly languageOptions = WORKSPACE_LANGUAGE_OPTIONS;
  protected readonly visibilityOptions = WORKSPACE_VISIBILITY_OPTIONS;
  protected readonly form = this.formBuilder.group({
    name: ['', [Validators.required, Validators.maxLength(120)]],
    description: ['', [Validators.maxLength(1000)]],
    industry: ['', [Validators.maxLength(80)]],
    timezone: ['Asia/Dhaka', [Validators.required, Validators.maxLength(80)]],
    language: this.formBuilder.control<WorkspaceLanguage>('ENGLISH', {
      validators: [Validators.required],
    }),
    currency: ['BDT', [Validators.required, Validators.maxLength(3), Validators.pattern(/^[A-Za-z]{3}$/)]],
    country: ['BD', [Validators.required, Validators.maxLength(2), Validators.pattern(/^[A-Za-z]{2}$/)]],
    logoUrl: ['', [Validators.maxLength(300), optionalUrlValidator()]],
    allowCrewDownload: [false],
    allowCrewPublish: [false],
    workspaceVisibility: this.formBuilder.control<WorkspaceVisibility>('PRIVATE', {
      validators: [Validators.required],
    }),
  });

  constructor() {
    effect(() => {
      const workspace = this.workspace();
      const settings = this.settings();

      if (!workspace || !settings) {
        return;
      }

      this.submitted.set(false);
      this.form.reset(
        {
          name: workspace.name,
          description: workspace.description ?? '',
          industry: workspace.industry ?? '',
          timezone: settings.defaultTimezone || workspace.timezone,
          language: settings.defaultLanguage || workspace.language,
          currency: workspace.currency ?? 'BDT',
          country: workspace.country ?? 'BD',
          logoUrl: workspace.logoUrl ?? '',
          allowCrewDownload: settings.allowCrewDownload,
          allowCrewPublish: settings.allowCrewPublish,
          workspaceVisibility: settings.workspaceVisibility,
        },
        { emitEvent: false },
      );
    });

    effect(() => {
      if (this.readOnly()) {
        this.form.disable({ emitEvent: false });
      } else {
        this.form.enable({ emitEvent: false });
      }
    });
  }

  protected submit(): void {
    this.submitted.set(true);
    this.form.markAllAsTouched();

    if (this.readOnly() || this.form.invalid) {
      return;
    }

    const value = this.form.getRawValue();
    this.saved.emit({
      ...value,
      timezone: value.timezone.trim(),
      currency: value.currency.trim().toUpperCase(),
      country: value.country.trim().toUpperCase(),
      logoUrl: value.logoUrl.trim(),
      description: value.description.trim(),
      industry: value.industry.trim(),
    });
  }

  protected fieldError(controlName: WorkspaceSettingsControlName): string {
    const control = this.form.controls[controlName];
    const serverError = this.serverErrors()[controlName] ?? this.serverAlias(controlName);

    if (serverError) {
      return serverError;
    }

    if (!this.submitted() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'This field is required.';
    }

    if (control.hasError('url')) {
      return 'Enter a valid URL starting with http:// or https://.';
    }

    if (controlName === 'currency' && control.hasError('pattern')) {
      return 'Use a 3-letter currency code like BDT.';
    }

    if (controlName === 'country' && control.hasError('pattern')) {
      return 'Use a 2-letter country code like BD.';
    }

    if (control.hasError('maxlength')) {
      return `Keep this under ${control.getError('maxlength').requiredLength} characters.`;
    }

    return '';
  }

  protected inputClasses(extraClasses = ''): string {
    return `h-11 w-full rounded-md border border-border bg-white px-3 text-sm text-ink shadow-sm outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-100 ${extraClasses}`.trim();
  }

  protected selectClasses(): string {
    return 'h-11 w-full rounded-md border border-border bg-white px-3 text-sm text-ink shadow-sm outline-none transition focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
  }

  protected textareaClasses(): string {
    return 'w-full rounded-md border border-border bg-white px-3 py-3 text-sm text-ink shadow-sm outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
  }

  private serverAlias(controlName: WorkspaceSettingsControlName): string {
    if (controlName === 'timezone') {
      return this.serverErrors()['defaultTimezone'] ?? '';
    }

    if (controlName === 'language') {
      return this.serverErrors()['defaultLanguage'] ?? '';
    }

    return '';
  }
}
