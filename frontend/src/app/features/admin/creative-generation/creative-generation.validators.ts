import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

import {
  CreativeOutputFormat,
  CreativeType,
  isImageCreative,
  isImageFormat,
  isVideoCreative,
  isVideoFormat,
} from './models/creative-generation.models';

interface Option<T extends string> {
  readonly value: T;
}

export function supportedGenerationOptionValidator<T extends string>(
  options: readonly Option<T>[],
): ValidatorFn {
  const supportedValues = new Set(options.map((option) => option.value));

  return (control: AbstractControl): ValidationErrors | null => {
    const value = `${control.value ?? ''}`.trim();
    if (!value) {
      return null;
    }

    return supportedValues.has(value as T) ? null : { unsupportedSelection: true };
  };
}

export function generationConfigJsonValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = `${control.value ?? ''}`.trim();
    if (!value) {
      return null;
    }

    try {
      const parsed = JSON.parse(value) as unknown;
      return parsed && typeof parsed === 'object' && !Array.isArray(parsed)
        ? null
        : { jsonObject: true };
    } catch {
      return { jsonObject: true };
    }
  };
}

export function creativeGenerationFormValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const creativeTypeValue = control.get('creativeType')?.value as CreativeType | '';
    const outputFormatValue = control.get('outputFormat')?.value as CreativeOutputFormat | '';
    const creativeType = creativeTypeValue || null;
    const outputFormat = outputFormatValue || null;
    const width = Number(control.get('width')?.value ?? 0);
    const height = Number(control.get('height')?.value ?? 0);
    const duration = Number(control.get('duration')?.value ?? 0);
    const errors: ValidationErrors = {};

    if (creativeType && outputFormat) {
      if (isImageCreative(creativeType) && !isImageFormat(outputFormat)) {
        errors['unsupportedOutputFormat'] = true;
      }

      if (isVideoCreative(creativeType) && !isVideoFormat(outputFormat)) {
        errors['unsupportedOutputFormat'] = true;
      }
    }

    if (isImageCreative(creativeType) && (!width || !height)) {
      errors['imageDimensionsRequired'] = true;
    }

    if (isVideoCreative(creativeType) && !duration) {
      errors['durationRequired'] = true;
    }

    return Object.keys(errors).length ? errors : null;
  };
}
