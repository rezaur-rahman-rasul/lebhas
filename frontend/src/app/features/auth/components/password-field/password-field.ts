import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-password-field',
  standalone: true,
  imports: [ReactiveFormsModule, IconComponent],
  templateUrl: './password-field.html',
  styleUrl: './password-field.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PasswordFieldComponent {
  readonly label = input.required<string>();
  readonly placeholder = input('Enter password');
  readonly autocomplete = input('current-password');
  readonly testId = input<string | null>(null);
  readonly control = input.required<FormControl<string>>();
  readonly submitted = input(false);
  readonly externalError = input<string | null>(null);

  protected readonly revealed = signal(false);

  protected readonly errorMessage = computed(() => {
    const providedError = this.externalError();
    if (providedError) {
      return providedError;
    }

    const control = this.control();
    if (!this.submitted() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Password is required.';
    }

    if (control.hasError('minlength')) {
      return 'Password must be at least 8 characters.';
    }

    return '';
  });
}
