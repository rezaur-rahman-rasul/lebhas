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
    const iconPadding = this.icon() ? 'pl-10' : 'pl-3';
    return `${iconPadding} h-12 w-full rounded-xl border border-border bg-surface pr-3 text-base text-ink shadow-sm outline-none transition placeholder:text-muted focus:border-brand-500 focus:ring-4 focus:ring-brand-100/60 disabled:bg-panel`;
  }
}
