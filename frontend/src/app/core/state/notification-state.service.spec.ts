import { TestBed } from '@angular/core/testing';

import { NotificationStateService } from './notification-state.service';

describe('NotificationStateService', () => {
  it('deduplicates identical module and message toasts inside five seconds', () => {
    TestBed.configureTestingModule({});
    const service = TestBed.inject(NotificationStateService);

    service.error('Usage & Billing', 'No AI cost usage has been reported yet.');
    service.error('Usage & Billing', 'No AI cost usage has been reported yet.');

    expect(service.notifications().length).toBe(1);
  });
});
