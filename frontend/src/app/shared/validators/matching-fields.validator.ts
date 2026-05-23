import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function matchingFieldsValidator(
  firstControlName: string,
  secondControlName: string,
): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const firstControl = control.get(firstControlName);
    const secondControl = control.get(secondControlName);

    if (!firstControl || !secondControl) {
      return null;
    }

    return firstControl.value === secondControl.value ? null : { mismatch: true };
  };
}
