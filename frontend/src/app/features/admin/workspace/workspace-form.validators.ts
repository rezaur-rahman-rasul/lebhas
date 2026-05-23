import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

const HEX_COLOR_PATTERN = /^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$/;
const ALLOWED_URL_PROTOCOLS = new Set(['http:', 'https:']);

export function optionalUrlValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = `${control.value ?? ''}`.trim();
    if (!value) {
      return null;
    }

    try {
      const url = new URL(value);
      return ALLOWED_URL_PROTOCOLS.has(url.protocol) ? null : { url: true };
    } catch {
      return { url: true };
    }
  };
}

export function hexColorValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = `${control.value ?? ''}`.trim();
    if (!value) {
      return null;
    }

    return HEX_COLOR_PATTERN.test(value) ? null : { color: true };
  };
}

export function selectionRequiredValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    return Array.isArray(value) && value.length > 0 ? null : { selectionRequired: true };
  };
}

export function nullIfBlank(value: string | null | undefined): string | null {
  const normalized = `${value ?? ''}`.trim();
  return normalized ? normalized : null;
}
