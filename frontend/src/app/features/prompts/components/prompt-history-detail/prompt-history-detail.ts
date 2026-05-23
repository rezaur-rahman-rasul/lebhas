import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import {
  campaignObjectiveLabel,
  promptHistoryStatusLabel,
  promptHistoryStatusTone,
  promptLanguageLabel,
  promptPlatformLabel,
  suggestionTypeLabel,
} from '../../models/prompt-options';
import { PromptHistory } from '../../models/prompt.models';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';

interface ContextSummaryItem {
  readonly label: string;
  readonly value: string;
}

@Component({
  selector: 'app-prompt-history-detail',
  standalone: true,
  imports: [DatePipe, BadgeComponent, CardComponent],
  templateUrl: './prompt-history-detail.html',
  styleUrl: './prompt-history-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PromptHistoryDetail {
  readonly entry = input.required<PromptHistory>();

  protected readonly promptPlatformLabel = promptPlatformLabel;
  protected readonly promptLanguageLabel = promptLanguageLabel;
  protected readonly campaignObjectiveLabel = campaignObjectiveLabel;
  protected readonly suggestionTypeLabel = suggestionTypeLabel;
  protected readonly promptHistoryStatusLabel = promptHistoryStatusLabel;
  protected readonly promptHistoryStatusTone = promptHistoryStatusTone;

  protected readonly contextSummary = computed(() => this.buildContextSummary(this.entry()));

  private buildContextSummary(entry: PromptHistory): readonly ContextSummaryItem[] {
    const items: ContextSummaryItem[] = [
      { label: 'Platform', value: promptPlatformLabel(entry.platform) },
      { label: 'Language', value: promptLanguageLabel(entry.language) },
      { label: 'Campaign objective', value: campaignObjectiveLabel(entry.campaignObjective) },
      { label: 'Business type', value: entry.businessType?.trim() || '—' },
      { label: 'Suggestion type', value: suggestionTypeLabel(entry.suggestionType) },
    ];

    const snapshot = entry.brandContextSnapshot;
    if (snapshot) {
      for (const [key, value] of Object.entries(snapshot)) {
        items.push({
          label: formatContextLabel(key),
          value: formatContextValue(value),
        });
      }
    }

    return items;
  }
}

function formatContextLabel(key: string): string {
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/[_-]+/g, ' ')
    .trim()
    .replace(/^\w/, (char) => char.toUpperCase());
}

function formatContextValue(value: unknown): string {
  if (value === null || value === undefined) {
    return '—';
  }

  if (typeof value === 'string') {
    return value.trim() || '—';
  }

  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value);
  }

  try {
    return JSON.stringify(value);
  } catch {
    return '—';
  }
}
