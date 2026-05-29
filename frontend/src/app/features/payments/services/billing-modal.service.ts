import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class BillingModalService {
  private readonly openSignal = signal(false);

  readonly open = this.openSignal.asReadonly();

  show(): void {
    this.openSignal.set(true);
  }

  hide(): void {
    this.openSignal.set(false);
  }
}

