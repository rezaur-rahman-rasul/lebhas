import { ComponentFixture, TestBed } from '@angular/core/testing';

import { mockHistoryEntry } from '../../testing/prompt-intelligence.fixtures';
import { PromptHistoryCard } from './prompt-history-card';

describe('PromptHistoryCard', () => {
  let fixture: ComponentFixture<PromptHistoryCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PromptHistoryCard],
    }).compileComponents();

    fixture = TestBed.createComponent(PromptHistoryCard);
    fixture.componentRef.setInput('entry', mockHistoryEntry);
    fixture.detectChanges();
  });

  it('renders history previews and AI metadata', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain(mockHistoryEntry.sourcePrompt.slice(0, 20));
    expect(text).toContain(mockHistoryEntry.enhancedPrompt!.slice(0, 20));
    expect(text).toContain(mockHistoryEntry.aiProvider!);
    expect(text).toContain(mockHistoryEntry.model!);
    expect(text).toContain('Instagram');
    expect(text).toContain('English');
  });
});
