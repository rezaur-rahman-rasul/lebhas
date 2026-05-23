import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { roleBadgeTone } from '@app/core/auth/permissions';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { ProjectStore } from '@app/features/projects/project.store';
import {
  CAMPAIGN_OBJECTIVE_OPTIONS,
  CampaignObjective,
  DEFAULT_PROMPT_HISTORY_FILTERS,
  PLATFORM_OPTIONS,
  PROMPT_HISTORY_STATUS_OPTIONS,
  PROMPT_SUGGESTION_TYPE_OPTIONS,
  PromptHistory,
  PromptHistoryFilter,
  PromptHistoryStatus,
  PromptPlatform,
  SuggestionType,
} from '../models';
import { PromptHistoryStore } from '../state/prompt-history.store';
import { PromptEmptyState } from '../components/prompt-empty-state/prompt-empty-state';
import { PromptHistoryCard } from '../components/prompt-history-card/prompt-history-card';
import { PromptHistoryDetail } from '../components/prompt-history-detail/prompt-history-detail';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';

@Component({
  selector: 'app-prompt-history-page',
  standalone: true,
  imports: [
    RouterLink,
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    ModalShellComponent,
    PageHeaderComponent,
    PromptEmptyState,
    PromptHistoryCard,
    PromptHistoryDetail,
  ],
  templateUrl: './prompt-history.html',
  styleUrl: './prompt-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptHistoryPage {
  protected readonly store = inject(PromptHistoryStore);
  private readonly permissions = inject(PermissionStore);
  private readonly auth = inject(CurrentUserStore);
  protected readonly workspace = inject(WorkspaceStore);
  private readonly projectStore = inject(ProjectStore);
  private readonly route = inject(ActivatedRoute);

  private readonly detailOpenSignal = signal(false);
  private readonly selectedEntrySignal = signal<PromptHistory | null>(null);
  private readonly initializingSignal = signal(true);

  protected readonly detailOpen = this.detailOpenSignal.asReadonly();
  protected readonly selectedEntry = this.selectedEntrySignal.asReadonly();
  protected readonly initializing = this.initializingSignal.asReadonly();

  protected readonly projectId = computed(() => this.route.snapshot.paramMap.get('projectId') ?? '');
  protected readonly project = computed(
    () => this.projectStore.items().find((item) => item.id === this.projectId()) ?? null,
  );

  protected readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));
  protected readonly canViewHistory = this.permissions.canViewPromptHistory;
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'User');
  protected readonly roleTone = computed(() => roleBadgeTone(this.auth.currentRole()));
  protected readonly historyCountLabel = computed(
    () =>
      `${this.store.pagination().totalItems} entr${this.store.pagination().totalItems === 1 ? 'y' : 'ies'}`,
  );
  protected readonly skeletonItems = [0, 1, 2, 3, 4, 5];

  protected readonly filterForm = new FormGroup({
    platform: new FormControl<PromptPlatform | ''>('', { nonNullable: true }),
    campaignObjective: new FormControl<CampaignObjective | ''>('', { nonNullable: true }),
    suggestionType: new FormControl<SuggestionType | ''>('', { nonNullable: true }),
    status: new FormControl<PromptHistoryStatus | ''>('', { nonNullable: true }),
  });

  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly objectiveOptions = CAMPAIGN_OBJECTIVE_OPTIONS;
  protected readonly suggestionTypeOptions = PROMPT_SUGGESTION_TYPE_OPTIONS;
  protected readonly statusOptions = PROMPT_HISTORY_STATUS_OPTIONS;

  constructor() {
    void this.initialize();
  }

  protected async initialize(): Promise<void> {
    this.initializingSignal.set(true);

    const workspaceId = this.auth.activeWorkspaceId();
    const projectId = this.projectId();

    if (workspaceId && projectId) {
      await this.projectStore.load(workspaceId);
      this.store.setSelectedProjectId(projectId);
      await this.store.loadHistory(projectId);
    }

    this.initializingSignal.set(false);
  }

  protected openDetail(entry: PromptHistory): void {
    this.selectedEntrySignal.set(entry);
    this.detailOpenSignal.set(true);
  }

  protected closeDetail(): void {
    this.detailOpenSignal.set(false);
    this.selectedEntrySignal.set(null);
  }

  protected async applyFilters(): Promise<void> {
    await this.store.loadHistory(this.projectId(), this.buildFilters(), 0);
  }

  protected async resetFilters(): Promise<void> {
    this.filterForm.reset(
      {
        platform: '',
        campaignObjective: '',
        suggestionType: '',
        status: '',
      },
      { emitEvent: false },
    );
    await this.store.loadHistory(this.projectId(), DEFAULT_PROMPT_HISTORY_FILTERS, 0);
  }

  protected reloadHistory(): void {
    void this.store.loadHistory(this.projectId(), this.buildFilters());
  }

  protected clearError(): void {
    this.store.clearError();
  }

  protected previousPage(): void {
    void this.store.goToPage(this.store.pagination().page - 1);
  }

  protected nextPage(): void {
    void this.store.goToPage(this.store.pagination().page + 1);
  }

  private buildFilters(): PromptHistoryFilter {
    const value = this.filterForm.getRawValue();
    return {
      ...DEFAULT_PROMPT_HISTORY_FILTERS,
      platform: value.platform || null,
      campaignObjective: value.campaignObjective || null,
      suggestionType: value.suggestionType || null,
      status: value.status || null,
    };
  }
}
