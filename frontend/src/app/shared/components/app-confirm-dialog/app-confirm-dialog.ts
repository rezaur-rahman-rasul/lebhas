import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ModalComponent } from '../app-dialog/app-dialog';

@Component({
  selector: 'app-confirm-dialog, app-modal-shell',
  standalone: true,
  imports: [ModalComponent],
  templateUrl: './app-confirm-dialog.html',
  styleUrl: './app-confirm-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalShellComponent {
  readonly open = input(false);
  readonly title = input.required<string>();
  readonly description = input('');
  readonly closeOnBackdrop = input(true);
  readonly closed = output<void>();
}
