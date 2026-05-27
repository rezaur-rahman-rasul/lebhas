import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { BillingCycle } from '../../models/payment.models';

@Component({
  selector: 'app-billing-cycle-toggle',
  standalone: true,
  templateUrl: './billing-cycle-toggle.html',
  styleUrl: './billing-cycle-toggle.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BillingCycleToggleComponent {
  readonly selected = input<BillingCycle | string>(BillingCycle.Monthly);
  readonly disabled = input(false);
  readonly selectedChange = output<BillingCycle>();

  protected readonly cycles = [BillingCycle.Monthly, BillingCycle.Yearly] as const;

  protected select(cycle: BillingCycle): void {
    if (!this.disabled()) {
      this.selectedChange.emit(cycle);
    }
  }

  protected label(cycle: BillingCycle): string {
    return cycle === BillingCycle.Monthly ? 'Monthly' : 'Yearly';
  }
}
