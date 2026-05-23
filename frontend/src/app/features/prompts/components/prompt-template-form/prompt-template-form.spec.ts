import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PromptTemplateForm } from './prompt-template-form';

describe('PromptTemplateForm', () => {
  let fixture: ComponentFixture<PromptTemplateForm>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PromptTemplateForm],
    }).compileComponents();

    fixture = TestBed.createComponent(PromptTemplateForm);
    fixture.detectChanges();
  });

  it('does not submit when required fields are missing', () => {
    const submitted = vi.fn();
    fixture.componentInstance.submitted.subscribe(submitted);

    fixture.componentInstance['submit']();

    expect(submitted).not.toHaveBeenCalled();
    expect(fixture.componentInstance['form'].controls.name.touched).toBe(true);
    expect(fixture.componentInstance['form'].controls.promptBody.touched).toBe(true);
  });
});
