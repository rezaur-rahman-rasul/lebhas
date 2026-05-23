import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { BrandProfile, UpdateBrandProfilePayload } from '../../models/brand-profile.models';
import { hexColorValidator, optionalUrlValidator } from '../../workspace-form.validators';

type BrandProfileControlName =
  | 'brandName'
  | 'businessType'
  | 'industry'
  | 'targetAudience'
  | 'brandVoice'
  | 'preferredCTA'
  | 'primaryColor'
  | 'secondaryColor'
  | 'website'
  | 'facebookUrl'
  | 'instagramUrl'
  | 'linkedinUrl'
  | 'tiktokUrl'
  | 'description';

@Component({
  selector: 'app-brand-profile-form',
  standalone: true,
  imports: [ReactiveFormsModule, BadgeComponent, ButtonComponent, CardComponent],
  templateUrl: './brand-profile-form.html',
  styleUrl: './brand-profile-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandProfileFormComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;

  readonly profile = input<BrandProfile | null>(null);
  readonly readOnly = input(false);
  readonly submitting = input(false);
  readonly serverErrors = input<Readonly<Record<string, string>>>({});
  readonly saved = output<UpdateBrandProfilePayload>();

  protected readonly submitted = signal(false);
  protected readonly form = this.formBuilder.group({
    brandName: ['', [Validators.required, Validators.maxLength(120)]],
    businessType: ['', [Validators.maxLength(80)]],
    industry: ['', [Validators.required, Validators.maxLength(80)]],
    targetAudience: ['', [Validators.required, Validators.maxLength(160)]],
    brandVoice: ['', [Validators.required, Validators.maxLength(120)]],
    preferredCTA: ['', [Validators.required, Validators.maxLength(120)]],
    primaryColor: ['', [hexColorValidator()]],
    secondaryColor: ['', [hexColorValidator()]],
    website: ['', [Validators.maxLength(300), optionalUrlValidator()]],
    facebookUrl: ['', [Validators.maxLength(300), optionalUrlValidator()]],
    instagramUrl: ['', [Validators.maxLength(300), optionalUrlValidator()]],
    linkedinUrl: ['', [Validators.maxLength(300), optionalUrlValidator()]],
    tiktokUrl: ['', [Validators.maxLength(300), optionalUrlValidator()]],
    description: ['', [Validators.required, Validators.maxLength(1000)]],
  });

  constructor() {
    effect(() => {
      const profile = this.profile();
      if (!profile) {
        return;
      }

      this.submitted.set(false);
      this.form.reset(
        {
          brandName: profile.brandName ?? '',
          businessType: profile.businessType ?? '',
          industry: profile.industry ?? '',
          targetAudience: profile.targetAudience ?? '',
          brandVoice: profile.brandVoice ?? '',
          preferredCTA: profile.preferredCTA ?? '',
          primaryColor: profile.primaryColor ?? '',
          secondaryColor: profile.secondaryColor ?? '',
          website: profile.website ?? '',
          facebookUrl: profile.facebookUrl ?? '',
          instagramUrl: profile.instagramUrl ?? '',
          linkedinUrl: profile.linkedinUrl ?? '',
          tiktokUrl: profile.tiktokUrl ?? '',
          description: profile.description ?? '',
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
      brandName: value.brandName.trim(),
      businessType: value.businessType.trim() || null,
      industry: value.industry.trim() || null,
      targetAudience: value.targetAudience.trim() || null,
      brandVoice: value.brandVoice.trim() || null,
      preferredCTA: value.preferredCTA.trim() || null,
      primaryColor: value.primaryColor.trim() || null,
      secondaryColor: value.secondaryColor.trim() || null,
      website: value.website.trim() || null,
      facebookUrl: value.facebookUrl.trim() || null,
      instagramUrl: value.instagramUrl.trim() || null,
      linkedinUrl: value.linkedinUrl.trim() || null,
      tiktokUrl: value.tiktokUrl.trim() || null,
      description: value.description.trim() || null,
    });
  }

  protected fieldError(controlName: BrandProfileControlName): string {
    const control = this.form.controls[controlName];
    const serverError =
      this.serverErrors()[controlName] ??
      (controlName === 'preferredCTA' ? this.serverErrors()['preferredCta'] : '');

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

    if (control.hasError('color')) {
      return 'Use a valid hex color like #0F766E.';
    }

    if (control.hasError('maxlength')) {
      return `Keep this under ${control.getError('maxlength').requiredLength} characters.`;
    }

    return '';
  }

  protected inputClasses(): string {
    return 'h-11 w-full rounded-md border border-border bg-white px-3 text-sm text-ink shadow-sm outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
  }

  protected textareaClasses(): string {
    return 'w-full rounded-md border border-border bg-white px-3 py-3 text-sm text-ink shadow-sm outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-100';
  }
}
