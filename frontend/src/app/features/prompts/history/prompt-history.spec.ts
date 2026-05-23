import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideRouter } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ProjectStore } from '@app/features/projects/project.store';
import { PromptHistoryStore } from '../state/prompt-history.store';
import {
  createAuthMock,
  createItemsSignal,
  mockHistoryEntry,
  mockProject,
  mockProjectId,
} from '../testing/prompt-intelligence.fixtures';
import { PromptHistoryPage } from './prompt-history';

describe('PromptHistoryPage', () => {
  let fixture: ComponentFixture<PromptHistoryPage>;
  let historyStore: {
    history: ReturnType<typeof signal>;
    pagination: ReturnType<typeof signal>;
    loading: ReturnType<typeof signal>;
    error: ReturnType<typeof signal>;
    hasHistory: ReturnType<typeof signal>;
    loadHistory: ReturnType<typeof vi.fn>;
    clearError: ReturnType<typeof vi.fn>;
    goToPage: ReturnType<typeof vi.fn>;
    setSelectedProjectId: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    historyStore = {
      history: signal<readonly typeof mockHistoryEntry[]>([]),
      pagination: signal({
        page: 0,
        size: 20,
        totalItems: 0,
        totalPages: 0,
        first: true,
        last: true,
      }),
      loading: signal(false),
      error: signal<string | null>(null),
      hasHistory: signal(false),
      loadHistory: vi.fn().mockResolvedValue({ ok: true, fieldErrors: {} }),
      clearError: vi.fn(),
      goToPage: vi.fn(),
      setSelectedProjectId: vi.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [PromptHistoryPage],
      providers: [
        provideRouter([]),
        PermissionStore,
        {
          provide: PromptHistoryStore,
          useValue: {
            ...historyStore,
            history: historyStore.history.asReadonly(),
            pagination: historyStore.pagination.asReadonly(),
            loading: historyStore.loading.asReadonly(),
            error: historyStore.error.asReadonly(),
            hasHistory: historyStore.hasHistory.asReadonly(),
          },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => mockProjectId } } },
        },
        { provide: CurrentUserStore, useValue: createAuthMock() },
        {
          provide: WorkspaceStore,
          useValue: { workspaceLabel: signal('Lebhas Workspace').asReadonly() },
        },
        {
          provide: ProjectStore,
          useValue: {
            load: vi.fn().mockResolvedValue(undefined),
            items: createItemsSignal([mockProject]),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(PromptHistoryPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  it('loads history for the route project', () => {
    expect(historyStore.setSelectedProjectId).toHaveBeenCalledWith(mockProjectId);
    expect(historyStore.loadHistory).toHaveBeenCalledWith(mockProjectId);
  });

  it('renders the history list', () => {
    historyStore.history.set([mockHistoryEntry]);
    historyStore.hasHistory.set(true);
    historyStore.pagination.set({
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
      first: true,
      last: true,
    });
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-prompt-history-card')).toBeTruthy();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain(
      mockHistoryEntry.sourcePrompt.slice(0, 20),
    );
  });

  it('opens the detail modal for a history entry', () => {
    historyStore.history.set([mockHistoryEntry]);
    historyStore.hasHistory.set(true);
    fixture.detectChanges();

    const buttons = fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>;
    const button = Array.from(buttons).find((node) => node.textContent?.includes('View detail'))!;
    button.click();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-prompt-history-detail')).toBeTruthy();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Prompt history detail');
  });

  it('renders an empty state when no history exists', () => {
    historyStore.history.set([]);
    historyStore.hasHistory.set(false);
    historyStore.loading.set(false);
    historyStore.error.set(null);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No prompt history yet');
  });
});
