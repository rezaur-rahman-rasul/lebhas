import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

interface PromptOption<T extends string> {
  readonly value: T;
}

export function supportedPromptOptionValidator<T extends string>(
  options: readonly PromptOption<T>[],
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
