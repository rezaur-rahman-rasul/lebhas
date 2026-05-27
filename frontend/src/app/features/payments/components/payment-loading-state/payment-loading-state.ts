import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-payment-loading-state',
  standalone: true,
  templateUrl: './payment-loading-state.html',
  styleUrl: './payment-loading-state.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentLoadingStateComponent {
  readonly label = input('Loading payment information');
  readonly cardCount = input(3);
  readonly listCount = input(4);

  protected readonly skeletonCards = Array.from({ length: 6 }, (_, index) => index);
  protected readonly skeletonRows = Array.from({ length: 8 }, (_, index) => index);
}
