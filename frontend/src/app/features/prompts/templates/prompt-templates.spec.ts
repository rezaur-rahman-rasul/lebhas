import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { PromptTemplateStore } from '../state/prompt-template.store';
import { createAuthMock, mockTemplate } from '../testing/prompt-intelligence.fixtures';
import { PromptTemplatesPage } from './prompt-templates';

describe('PromptTemplatesPage', () => {
  let fixture: ComponentFixture<PromptTemplatesPage>;
  let templateStore: {
    templates: ReturnType<typeof signal>;
    loading: ReturnType<typeof signal>;
    error: ReturnType<typeof signal>;
    hasTemplates: ReturnType<typeof signal>;
    loadTemplates: ReturnType<typeof vi.fn>;
    clearError: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    templateStore = {
      templates: signal<readonly typeof mockTemplate[]>([]),
      loading: signal(false),
      error: signal<string | null>(null),
      hasTemplates: signal(false),
      loadTemplates: vi.fn().mockResolvedValue({ ok: true, fieldErrors: {} }),
      clearError: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [PromptTemplatesPage],
      providers: [
        provideRouter([]),
        PermissionStore,
        {
          provide: PromptTemplateStore,
          useValue: {
            ...templateStore,
            templates: templateStore.templates.asReadonly(),
            loading: templateStore.loading.asReadonly(),
            error: templateStore.error.asReadonly(),
            hasTemplates: templateStore.hasTemplates.asReadonly(),
          },
        },
        { provide: CurrentUserStore, useValue: createAuthMock() },
        {
          provide: WorkspaceStore,
          useValue: { workspaceLabel: signal('Lebhas Workspace').asReadonly() },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PromptTemplatesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('renders the template list', () => {
    templateStore.templates.set([mockTemplate]);
    templateStore.hasTemplates.set(true);
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-prompt-template-card')).toBeTruthy();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(mockTemplate.name);
  });
});
