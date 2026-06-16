import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { IconComponent } from '../icon/icon';

@Component({
  selector: 'app-input',
  standalone: true,
  imports: [ReactiveFormsModule, IconComponent],
  templateUrl: './input.html',
  styleUrl: './input.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InputComponent {
  readonly label = input('');
  readonly type = input('text');
  readonly placeholder = input('');
  readonly autocomplete = input('off');
  readonly value = input('');
  readonly icon = input<string | null>(null);
  readonly error = input('');
  readonly disabled = input(false);
  readonly testId = input<string | null>(null);
  readonly control = input<FormControl<string> | null>(null);
  readonly modalPrimary = input(false);
  readonly valueChange = output<string>();

  protected controlRef(): FormControl<string> {
    return this.control() as FormControl<string>;
  }

  protected inputClasses(): string {
    const iconPadding = this.icon() ? '!pl-10' : '';
    return `${iconPadding} h-11 w-full rounded-md border border-border bg-white px-3 text-sm font-semibold text-ink outline-none transition placeholder:text-slate-400 focus:border-brand-500 focus:ring-2 focus:ring-brand-100 disabled:cursor-not-allowed disabled:bg-slate-50`;
  }
}
