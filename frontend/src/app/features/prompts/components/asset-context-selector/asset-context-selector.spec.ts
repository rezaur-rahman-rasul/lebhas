import { ComponentFixture, TestBed } from '@angular/core/testing';

import {
  mockOtherProjectAsset,
  mockProjectAsset,
} from '../../testing/prompt-intelligence.fixtures';
import { AssetContextSelector } from './asset-context-selector';

describe('AssetContextSelector', () => {
  let fixture: ComponentFixture<AssetContextSelector>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AssetContextSelector],
    }).compileComponents();

    fixture = TestBed.createComponent(AssetContextSelector);
    fixture.componentRef.setInput('assets', [mockProjectAsset]);
    fixture.componentRef.setInput('selectedAssets', []);
    fixture.detectChanges();
  });

  it('only renders assets passed into the selector', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain(mockProjectAsset.displayName);
    expect(element.textContent).not.toContain(mockOtherProjectAsset.displayName);
  });

  it('emits the selected project asset', () => {
    const toggled = vi.fn();
    fixture.componentInstance.assetToggled.subscribe(toggled);

    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    const button = Array.from(buttons).find((node) => node.textContent?.includes('Select'))!;
    button.click();

    expect(toggled).toHaveBeenCalledWith(mockProjectAsset);
  });
});
