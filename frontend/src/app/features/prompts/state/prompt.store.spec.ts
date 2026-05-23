import { TestBed } from '@angular/core/testing';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { PromptApiService } from '../services/prompt-api.service';
import {
  createAuthMock,
  mockBuilderSettings,
  mockProjectContext,
  mockWorkspaceId,
} from '../testing/prompt-intelligence.fixtures';
import { PromptStore } from './prompt.store';

describe('PromptStore', () => {
  let store: PromptStore;
  let promptApi: {
    enhancePrompt: ReturnType<typeof vi.fn>;
    generateSuggestions: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    promptApi = {
      enhancePrompt: vi.fn(),
      generateSuggestions: vi.fn(),
    };

    TestBed.configureTestingModule({
      providers: [
        PromptStore,
        PermissionStore,
        { provide: PromptApiService, useValue: promptApi },
        { provide: CurrentUserStore, useValue: createAuthMock() },
        {
          provide: NotificationStateService,
          useValue: { success: vi.fn(), error: vi.fn() },
        },
      ],
    });

    store = TestBed.inject(PromptStore);
    store.setSelectedProjectContext(mockProjectContext);
    store.patchBuilderSettings(mockBuilderSettings);
    store.setSourcePrompt('Valid source prompt for intelligence.');
  });

  it('calls the enhance API with project context', async () => {
    promptApi.enhancePrompt.mockResolvedValue({
      enhancedPrompt: 'Enhanced campaign prompt',
      reasoningSummary: null,
      suggestedMissingFields: [],
      aiProvider: 'openai',
      model: 'gpt-4.1-mini',
      tokenUsage: 42,
    });

    const result = await store.enhancePrompt();

    expect(result.ok).toBe(true);
    expect(promptApi.enhancePrompt).toHaveBeenCalledWith(
      mockWorkspaceId,
      mockProjectContext.projectId,
      expect.objectContaining({ customPrompt: 'Valid source prompt for intelligence.' }),
      expect.anything(),
    );
    expect(store.enhancedPrompt()?.enhancedPrompt).toBe('Enhanced campaign prompt');
  });

  it('calls the suggestions API with project context', async () => {
    promptApi.generateSuggestions.mockResolvedValue({
      ctaSuggestions: ['Shop now'],
      headlineSuggestions: [],
      offerSuggestions: [],
      creativeAngleSuggestions: [],
      campaignToneSuggestions: [],
      businessCategorySuggestions: [],
      reasoningSummary: null,
      aiProvider: 'openai',
      model: 'gpt-4.1-mini',
      tokenUsage: 24,
    });

    const result = await store.getSuggestions(['CTA_SUGGESTIONS']);

    expect(result.ok).toBe(true);
    expect(promptApi.generateSuggestions).toHaveBeenCalledWith(
      mockWorkspaceId,
      mockProjectContext.projectId,
      expect.objectContaining({ suggestionTypes: ['CTA_SUGGESTIONS'] }),
      expect.anything(),
    );
    expect(store.hasSuggestions()).toBe(true);
  });

  it('surfaces AI API failures', async () => {
    promptApi.enhancePrompt.mockRejectedValue({ error: { message: 'Enhancement failed' } });

    const result = await store.enhancePrompt();

    expect(result.ok).toBe(false);
    expect(store.error()).toBeTruthy();
    expect(store.aiLoading()).toBe(false);
  });

  it('blocks crew users without prompt intelligence permission', async () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        PromptStore,
        PermissionStore,
        { provide: PromptApiService, useValue: promptApi },
        {
          provide: CurrentUserStore,
          useValue: createAuthMock({ role: 'CREW', permissions: ['PROMPT_HISTORY_VIEW'] }),
        },
        {
          provide: NotificationStateService,
          useValue: { success: vi.fn(), error: vi.fn() },
        },
      ],
    });

    const crewStore = TestBed.inject(PromptStore);
    crewStore.setSelectedProjectContext(mockProjectContext);
    crewStore.patchBuilderSettings(mockBuilderSettings);
    crewStore.setSourcePrompt('Valid source prompt for intelligence.');

    const result = await crewStore.enhancePrompt();

    expect(result.ok).toBe(false);
    expect(promptApi.enhancePrompt).not.toHaveBeenCalled();
    expect(crewStore.canUsePromptBuilder()).toBe(false);
  });

  it('sets AI loading while enhance is in flight', async () => {
    let resolveEnhance: (value: unknown) => void = () => undefined;
    promptApi.enhancePrompt.mockReturnValue(
      new Promise((resolve) => {
        resolveEnhance = resolve;
      }),
    );

    const pending = store.enhancePrompt();
    expect(store.aiLoading()).toBe(true);

    resolveEnhance({
      enhancedPrompt: 'Enhanced',
      reasoningSummary: null,
      suggestedMissingFields: [],
      aiProvider: null,
      model: null,
      tokenUsage: null,
    });
    await pending;

    expect(store.aiLoading()).toBe(false);
  });
});
