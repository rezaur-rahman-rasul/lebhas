import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { SpinnerComponent } from '../spinner/spinner';

@Component({
  selector: 'app-loading-state, app-loading',
  standalone: true,
  imports: [SpinnerComponent],
  templateUrl: './app-loading-state.html',
  styleUrl: './app-loading-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingComponent {
  readonly label = input('Loading');
  readonly size = input<'sm' | 'md' | 'lg'>('md');
  readonly fullscreen = input(false);
  readonly polite = input(true);
}

