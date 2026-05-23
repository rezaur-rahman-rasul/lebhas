import { ComponentFixture, TestBed } from '@angular/core/testing';

import { mockHistoryEntry } from '../../testing/prompt-intelligence.fixtures';
import { PromptHistoryDetail } from './prompt-history-detail';

describe('PromptHistoryDetail', () => {
  let fixture: ComponentFixture<PromptHistoryDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PromptHistoryDetail],
    }).compileComponents();

    fixture = TestBed.createComponent(PromptHistoryDetail);
    fixture.componentRef.setInput('entry', mockHistoryEntry);
    fixture.detectChanges();
  });

  it('renders full prompts, context summary, and metadata', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain(mockHistoryEntry.sourcePrompt);
    expect(text).toContain(mockHistoryEntry.enhancedPrompt!);
    expect(text).toContain('Context summary');
    expect(text).toContain('Lebhas Studio');
    expect(text).toContain(String(mockHistoryEntry.tokenUsage));
    expect(text).toContain(mockHistoryEntry.aiProvider!);
    expect(text).toContain(mockHistoryEntry.model!);
  });
});
