import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { SpinnerComponent } from '../spinner/spinner';

@Component({
  selector: 'app-loading',
  standalone: true,
  imports: [SpinnerComponent],
  templateUrl: './loading.html',
  styleUrl: './loading.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoadingComponent {
  readonly label = input('Loading');
  readonly size = input<'sm' | 'md' | 'lg'>('md');
  readonly fullscreen = input(false);
}

