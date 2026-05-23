import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AiLoadingState } from './ai-loading-state';

describe('AiLoadingState', () => {
  let fixture: ComponentFixture<AiLoadingState>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiLoadingState],
    }).compileComponents();

    fixture = TestBed.createComponent(AiLoadingState);
    fixture.detectChanges();
  });

  it('renders an accessible busy state for AI processing', () => {
    const element = fixture.nativeElement as HTMLElement;
    const status = element.querySelector('[role="status"]');

    expect(status).toBeTruthy();
    expect(status?.getAttribute('aria-busy')).toBe('true');
    expect(element.textContent).toContain('Thinking through your campaign context');
  });
});
