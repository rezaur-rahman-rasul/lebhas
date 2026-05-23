import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  DEFAULT_PROMPT_HISTORY_FILTERS,
  DEFAULT_PROMPT_PAGINATION,
  PromptHistory,
  PromptHistoryFilter,
  PromptHistoryListQuery,
  PromptPagination,
} from '../models';
import { validateProjectId } from '../services/prompt.validation';
import { PromptApiService } from '../services/prompt-api.service';
import {
  failureResult,
  fieldErrorsResult,
  PromptActionResult,
  successResult,
} from './prompt.state';

@Injectable({ providedIn: 'root' })
export class PromptHistoryStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly permissions = inject(PermissionStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly promptApi = inject(PromptApiService);

  private readonly historySignal = signal<readonly PromptHistory[]>([]);
  private readonly selectedProjectIdSignal = signal<string | null>(null);
  private readonly filtersSignal = signal<PromptHistoryFilter>(DEFAULT_PROMPT_HISTORY_FILTERS);
  private readonly paginationSignal = signal<PromptPagination>(DEFAULT_PROMPT_PAGINATION);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly history = this.historySignal.asReadonly();
  readonly selectedProjectId = this.selectedProjectIdSignal.asReadonly();
  readonly filters = this.filtersSignal.asReadonly();
  readonly pagination = this.paginationSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly hasHistory = computed(() => this.historySignal().length > 0);
  readonly canViewHistory = this.permissions.canViewPromptHistory;

  setSelectedProjectId(projectId: string | null): void {
    this.selectedProjectIdSignal.set(projectId);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  async loadHistory(
    projectId?: string,
    filters?: PromptHistoryFilter,
    page?: number,
  ): Promise<PromptActionResult> {
    const resolvedProjectId = projectId ?? this.selectedProjectIdSignal();
    const projectError = validateProjectId(resolvedProjectId);
    if (projectError) {
      return fieldErrorsResult({ projectId: projectError });
    }

    if (resolvedProjectId) {
      this.selectedProjectIdSignal.set(resolvedProjectId);
    }

    if (filters) {
      this.filtersSignal.set(filters);
    }

    if (page !== undefined) {
      this.paginationSignal.update((pagination) => ({ ...pagination, page }));
    }

    if (!this.canViewHistory()) {
      this.historySignal.set([]);
      return failureResult('You do not have permission to view prompt history.');
    }

    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return fieldErrorsResult({ workspaceId: 'Select a workspace before loading prompt history.' });
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const query: PromptHistoryListQuery = {
        ...this.filtersSignal(),
        page: this.paginationSignal().page,
        size: this.paginationSignal().size,
      };

      const pageResult = await this.promptApi.listHistory(
        workspaceId,
        resolvedProjectId!,
        query,
        this.requestContext(),
      );

      this.historySignal.set(pageResult.items);
      this.paginationSignal.set(pageResult.pagination);
      return successResult();
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.errorSignal.set(normalized.message);
      this.notifications.error('Prompt history', normalized.message);
      return failureResult(normalized.message);
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async goToPage(page: number): Promise<PromptActionResult> {
    this.paginationSignal.update((pagination) => ({ ...pagination, page }));
    return this.loadHistory();
  }

  private resolveWorkspaceId(): string | null {
    return this.auth.activeWorkspaceId();
  }

  private requestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}
