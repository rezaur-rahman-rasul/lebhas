import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/button/button';

@Component({
  selector: 'app-final-cta',
  standalone: true,
  imports: [ButtonComponent],
  templateUrl: './final-cta.html',
  styleUrl: './final-cta.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FinalCtaComponent {
  readonly authenticated = input(false);
  readonly actionRequested = output<void>();

  protected readonly backgroundImageUrl = '/assets/workflow-dashboard.png';
}
