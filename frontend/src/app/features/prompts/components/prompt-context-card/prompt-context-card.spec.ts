import { ComponentFixture, TestBed } from '@angular/core/testing';

import { mockBrand, mockProduct, mockProject } from '../../testing/prompt-intelligence.fixtures';
import { PromptContextCard } from './prompt-context-card';

describe('PromptContextCard', () => {
  it('renders brand context fields', async () => {
    const fixture = await createFixture();
    fixture.componentRef.setInput('kind', 'brand');
    fixture.componentRef.setInput('title', mockBrand.name);
    fixture.componentRef.setInput('businessType', mockBrand.businessType);
    fixture.componentRef.setInput('industry', mockBrand.industry);
    fixture.componentRef.setInput('targetAudience', mockBrand.targetAudience);
    fixture.componentRef.setInput('brandVoice', mockBrand.brandVoice);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Brand context');
    expect(text).toContain(mockBrand.name);
    expect(text).toContain(mockBrand.industry!);
    expect(text).toContain(mockBrand.brandVoice!);
  });

  it('renders product context fields', async () => {
    const fixture = await createFixture();
    fixture.componentRef.setInput('kind', 'product');
    fixture.componentRef.setInput('title', mockProduct.name);
    fixture.componentRef.setInput('description', mockProduct.description);
    fixture.componentRef.setInput('category', mockProduct.category);
    fixture.componentRef.setInput('sellingPoints', mockProduct.sellingPoints);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Product / service context');
    expect(text).toContain(mockProduct.description!);
    expect(text).toContain(mockProduct.sellingPoints!);
  });

  it('renders project context fields', async () => {
    const fixture = await createFixture();
    fixture.componentRef.setInput('kind', 'project');
    fixture.componentRef.setInput('title', mockProject.name);
    fixture.componentRef.setInput('campaignObjective', mockProject.campaignObjective);
    fixture.componentRef.setInput('targetPlatform', mockProject.targetPlatform);
    fixture.componentRef.setInput('campaignType', mockProject.campaignType);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Project / campaign context');
    expect(text).toContain(mockProject.campaignObjective!);
    expect(text).toContain(mockProject.targetPlatform!);
  });
});

async function createFixture(): Promise<ComponentFixture<PromptContextCard>> {
  await TestBed.configureTestingModule({
    imports: [PromptContextCard],
  }).compileComponents();

  const fixture = TestBed.createComponent(PromptContextCard);
  fixture.componentRef.setInput('title', 'Context');
  return fixture;
}
