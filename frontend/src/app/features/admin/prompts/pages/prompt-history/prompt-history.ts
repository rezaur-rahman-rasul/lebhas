import { KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  CAMPAIGN_OBJECTIVE_OPTIONS,
  CampaignObjective,
  DEFAULT_PROMPT_HISTORY_FILTERS,
  PLATFORM_OPTIONS,
  PROMPT_HISTORY_STATUS_OPTIONS,
  PROMPT_SUGGESTION_TYPE_OPTIONS,
  PromptHistory,
  PromptHistoryStatus,
  PromptHistoryFilter,
  PromptPlatform,
  SuggestionType,
  suggestionTypeLabel,
} from '../../models/prompt.models';
import { PromptStore } from '../../state/prompt.store';
import { PromptHistoryList } from '../../components/prompt-history-list/prompt-history-list';

@Component({
  selector: 'app-prompt-history-page',
  standalone: true,
  imports: [
    KeyValuePipe,
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    ModalShellComponent,
    PageHeaderComponent,
    PromptHistoryList,
  ],
  templateUrl: './prompt-history.html',
  styleUrl: './prompt-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptHistoryPage {
  protected readonly store = inject(PromptStore);

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
  protected readonly historyCountLabel = computed(
    () => `${this.store.historyPagination().totalItems} entries`,
  );
  protected readonly suggestionTypeLabel = suggestionTypeLabel;

  constructor() {
    void this.store.loadHistory();
  }

  protected async applyFilters(): Promise<void> {
    await this.store.loadHistory(this.buildFilters(), 0);
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
    await this.store.loadHistory(DEFAULT_PROMPT_HISTORY_FILTERS, 0);
  }

  protected async openDetail(history: PromptHistory): Promise<void> {
    this.store.setSelectedHistory(history);
    await this.store.loadHistoryDetail(history.id);
  }

  protected closeDetail(): void {
    this.store.setSelectedHistory(null);
  }

  protected previousPage(): void {
    void this.store.loadHistory(undefined, this.store.historyPagination().page - 1);
  }

  protected nextPage(): void {
    void this.store.loadHistory(undefined, this.store.historyPagination().page + 1);
  }

  protected reloadHistory(): void {
    void this.store.loadHistory();
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
