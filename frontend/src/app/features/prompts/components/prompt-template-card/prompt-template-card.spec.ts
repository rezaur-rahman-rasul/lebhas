import { ComponentFixture, TestBed } from '@angular/core/testing';

import { mockTemplate } from '../../testing/prompt-intelligence.fixtures';
import { PromptTemplateCard } from './prompt-template-card';

describe('PromptTemplateCard', () => {
  let fixture: ComponentFixture<PromptTemplateCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PromptTemplateCard],
    }).compileComponents();

    fixture = TestBed.createComponent(PromptTemplateCard);
    fixture.componentRef.setInput('template', mockTemplate);
    fixture.detectChanges();
  });

  it('renders template metadata in the list card', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain(mockTemplate.name);
    expect(text).toContain('Instagram');
    expect(text).toContain('English');
    expect(text).toContain(mockTemplate.promptBody);
  });
});
