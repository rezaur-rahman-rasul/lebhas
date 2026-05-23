import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PROMPT_MAX_LENGTH, PROMPT_MIN_LENGTH } from '../../models/prompt.models';
import { PromptEditor } from './prompt-editor';

describe('PromptEditor', () => {
  let fixture: ComponentFixture<PromptEditor>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PromptEditor],
    }).compileComponents();

    fixture = TestBed.createComponent(PromptEditor);
    fixture.componentRef.setInput('sourcePrompt', '');
    fixture.detectChanges();
  });

  it('shows minimum length guidance below the threshold', () => {
    fixture.componentRef.setInput('sourcePrompt', 'abc');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain(String(PROMPT_MIN_LENGTH));
  });

  it('shows maximum length guidance above the limit', () => {
    fixture.componentRef.setInput('sourcePrompt', 'x'.repeat(PROMPT_MAX_LENGTH + 1));
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain(String(PROMPT_MAX_LENGTH));
  });

  it('disables copy and reset actions while processing', () => {
    fixture.componentRef.setInput('sourcePrompt', 'Valid source prompt text');
    fixture.componentRef.setInput('enhancedPrompt', 'Enhanced output');
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const buttons = Array.from(
      fixture.nativeElement.querySelectorAll('button'),
    ) as HTMLButtonElement[];
    expect(buttons.every((button) => button.disabled)).toBe(true);
  });
});
