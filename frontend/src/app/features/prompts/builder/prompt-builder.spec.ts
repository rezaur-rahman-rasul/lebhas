import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { AssetStore } from '@app/features/assets/state/asset.store';
import { BrandStore } from '@app/features/brands/brand.store';
import { ProductServiceStore } from '@app/features/product-services/product-service.store';
import { ProjectStore } from '@app/features/projects/project.store';
import { PromptApiService } from '../services/prompt-api.service';
import { PromptStore } from '../state/prompt.store';
import {
  createAuthMock,
  createItemsSignal,
  mockBrand,
  mockBuilderSettings,
  mockProduct,
  mockProject,
  mockProjectAsset,
  mockProjectId,
} from '../testing/prompt-intelligence.fixtures';
import { PromptBuilderPage } from './prompt-builder';

async function waitForBuilderReady(fixture: ComponentFixture<PromptBuilderPage>): Promise<void> {
  await vi.waitUntil(() => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    return !text.includes('Loading project context');
  });
  fixture.detectChanges();
}

describe('PromptBuilderPage', () => {
  let fixture: ComponentFixture<PromptBuilderPage>;
  let promptStore: PromptStore;
  let promptApi: { enhancePrompt: ReturnType<typeof vi.fn>; generateSuggestions: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    promptApi = {
      enhancePrompt: vi.fn(),
      generateSuggestions: vi.fn(),
    };
    const brandItems = createItemsSignal([mockBrand]);
    const productItems = createItemsSignal([mockProduct]);
    const projectItems = createItemsSignal([mockProject]);

    await TestBed.configureTestingModule({
      imports: [PromptBuilderPage],
      providers: [
        provideRouter([]),
        PermissionStore,
        PromptStore,
        { provide: PromptApiService, useValue: promptApi },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: { paramMap: { get: () => mockProjectId } },
          },
        },
        { provide: CurrentUserStore, useValue: createAuthMock() },
        {
          provide: WorkspaceStore,
          useValue: { workspaceLabel: signal('Lebhas Workspace').asReadonly() },
        },
        {
          provide: BrandStore,
          useValue: { load: vi.fn().mockResolvedValue(undefined), items: brandItems },
        },
        {
          provide: ProductServiceStore,
          useValue: { load: vi.fn().mockResolvedValue(undefined), items: productItems },
        },
        {
          provide: ProjectStore,
          useValue: { load: vi.fn().mockResolvedValue(undefined), items: projectItems },
        },
        {
          provide: AssetStore,
          useValue: {
            loadProjectAssets: vi.fn().mockResolvedValue(undefined),
            filteredAssets: createItemsSignal([mockProjectAsset]),
            loading: signal(false).asReadonly(),
          },
        },
        {
          provide: NotificationStateService,
          useValue: { success: vi.fn(), error: vi.fn() },
        },
      ],
    }).compileComponents();

    promptStore = TestBed.inject(PromptStore);
    fixture = TestBed.createComponent(PromptBuilderPage);
    fixture.detectChanges();
    await fixture.whenStable();
    await waitForBuilderReady(fixture);
  });

  it('loads project context for the route project', () => {
    expect(promptStore.selectedProjectContext()?.projectId).toBe(mockProjectId);
    expect(promptStore.selectedProjectContext()?.brandName).toBe(mockBrand.name);
    expect(promptStore.selectedProjectContext()?.productName).toBe(mockProduct.name);
    expect(promptStore.selectedProjectContext()?.projectName).toBe(mockProject.name);
  });

  it('renders brand, product, and project context cards', () => {
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';

    expect(text).toContain(mockBrand.name);
    expect(text).toContain(mockProduct.name);
    expect(text).toContain(mockProject.name);
    expect(text).toContain('Brand context');
    expect(text).toContain('Product / service context');
    expect(text).toContain('Project / campaign context');
  });

  it('shows AI loading state while processing', async () => {
    promptStore.setSourcePrompt('Valid source prompt for intelligence.');
    promptStore.patchBuilderSettings(mockBuilderSettings);

    let resolveEnhance: (value: unknown) => void = () => undefined;
    promptApi.enhancePrompt.mockReturnValue(
      new Promise((resolve) => {
        resolveEnhance = resolve;
      }),
    );

    const pending = promptStore.enhancePrompt();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelectorAll('app-ai-loading-state').length).toBeGreaterThan(0);

    resolveEnhance({
      enhancedPrompt: 'Enhanced',
      reasoningSummary: null,
      suggestedMissingFields: [],
      aiProvider: null,
      model: null,
      tokenUsage: null,
    });
    await pending;
    fixture.detectChanges();
  });

  it('renders an error banner after AI failure', async () => {
    promptStore.setSourcePrompt('Valid source prompt for intelligence.');
    promptStore.patchBuilderSettings(mockBuilderSettings);
    promptApi.enhancePrompt.mockRejectedValue(
      new HttpErrorResponse({ error: { message: 'Enhancement failed' }, status: 500 }),
    );

    await promptStore.enhancePrompt();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Enhancement failed');
  });
});

describe('PromptBuilderPage permissions', () => {
  it('shows access denied for crew without prompt builder permission', async () => {
    await TestBed.configureTestingModule({
      imports: [PromptBuilderPage],
      providers: [
        provideRouter([]),
        PermissionStore,
        {
          provide: PromptStore,
          useValue: {
            setSelectedProjectContext: vi.fn(),
            patchBuilderSettings: vi.fn(),
            selectedProjectContext: signal(null).asReadonly(),
            aiLoading: signal(false).asReadonly(),
            sourcePrompt: signal('').asReadonly(),
            enhancedPrompt: signal(null).asReadonly(),
            builderSettings: signal({}).asReadonly(),
            canEnhancePrompt: signal(false).asReadonly(),
            error: signal(null).asReadonly(),
            hasSourcePrompt: signal(false).asReadonly(),
            hasEnhancedPrompt: signal(false).asReadonly(),
            hasSuggestions: signal(false).asReadonly(),
            selectedAssets: signal([]).asReadonly(),
            suggestions: signal(null).asReadonly(),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => mockProjectId } } },
        },
        {
          provide: CurrentUserStore,
          useValue: createAuthMock({ role: 'CREW', permissions: [] }),
        },
        {
          provide: WorkspaceStore,
          useValue: { workspaceLabel: signal('Lebhas Workspace').asReadonly() },
        },
        {
          provide: BrandStore,
          useValue: { load: vi.fn(), items: createItemsSignal([]) },
        },
        {
          provide: ProductServiceStore,
          useValue: { load: vi.fn(), items: createItemsSignal([]) },
        },
        {
          provide: ProjectStore,
          useValue: { load: vi.fn(), items: createItemsSignal([]) },
        },
        {
          provide: AssetStore,
          useValue: {
            loadProjectAssets: vi.fn(),
            filteredAssets: createItemsSignal([]),
            loading: signal(false).asReadonly(),
          },
        },
        {
          provide: NotificationStateService,
          useValue: { success: vi.fn(), error: vi.fn() },
        },
      ],
    }).compileComponents();

    const fixture = TestBed.createComponent(PromptBuilderPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      'Prompt builder access required',
    );
  });
});
