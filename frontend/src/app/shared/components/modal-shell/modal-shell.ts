import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { IconComponent } from '../icon/icon';

@Component({
  selector: 'app-modal-shell',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './modal-shell.html',
  styleUrl: './modal-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalShellComponent {
  readonly open = input(false);
  readonly title = input.required<string>();
  readonly description = input('');
  readonly closed = output<void>();
}
