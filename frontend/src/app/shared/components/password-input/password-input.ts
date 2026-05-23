import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { IconComponent } from '../icon/icon';

@Component({
  selector: 'app-password-input',
  standalone: true,
  imports: [ReactiveFormsModule, IconComponent],
  templateUrl: './password-input.html',
  styleUrl: './password-input.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PasswordInputComponent {
  readonly label = input.required<string>();
  readonly placeholder = input('Password');
  readonly autocomplete = input('current-password');
  readonly testId = input<string | null>(null);
  readonly control = input.required<FormControl<string>>();
  readonly submitted = input(false);
  readonly error = input('');

  protected readonly revealed = signal(false);
  protected readonly errorMessage = computed(() => {
    if (this.error()) {
      return this.error();
    }

    const control = this.control();
    if (!this.submitted() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Enter your password.';
    }

    if (control.hasError('minlength')) {
      return 'Password must be at least 8 characters.';
    }

    return '';
  });
}

