import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { IconComponent } from '../icon/icon';

@Component({
  selector: 'app-error-state',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './app-error-state.html',
  styleUrl: './app-error-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppErrorStateComponent {
  readonly title = input('Something went wrong');
  readonly description = input('Please try again or contact support if the issue continues.');
  readonly icon = input('circle-alert');
}
