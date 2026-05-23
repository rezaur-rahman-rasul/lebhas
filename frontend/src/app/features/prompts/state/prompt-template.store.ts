import { HttpContext } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  CreatePromptTemplateRequest,
  DEFAULT_PROMPT_TEMPLATE_FILTERS,
  PromptTemplate,
  PromptTemplateFilter,
  UpdatePromptTemplateRequest,
} from '../models';
import { PromptApiService } from '../services/prompt-api.service';
import { validateTemplatePayload } from '../services/prompt.validation';
import {
  failureResult,
  fieldErrorsResult,
  PromptActionResult,
  successResult,
} from './prompt.state';

@Injectable({ providedIn: 'root' })
export class PromptTemplateStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly permissions = inject(PermissionStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly promptApi = inject(PromptApiService);

  private readonly templatesSignal = signal<readonly PromptTemplate[]>([]);
  private readonly filtersSignal = signal<PromptTemplateFilter>(DEFAULT_PROMPT_TEMPLATE_FILTERS);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly templates = this.templatesSignal.asReadonly();
  readonly filters = this.filtersSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly hasTemplates = computed(() => this.templatesSignal().length > 0);
  readonly canManageTemplates = this.permissions.canManageTemplates;
  readonly canViewTemplates = this.permissions.canViewPromptTemplates;

  clearError(): void {
    this.errorSignal.set(null);
  }

  async loadTemplates(filters?: PromptTemplateFilter): Promise<PromptActionResult> {
    if (filters) {
      this.filtersSignal.set(filters);
    }

    if (!this.canViewTemplates()) {
      this.templatesSignal.set([]);
      return failureResult('You do not have permission to view prompt templates.');
    }

    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return fieldErrorsResult({ workspaceId: 'Select a workspace before loading templates.' });
    }

    return this.runLoader(async () => {
      const templates = await this.promptApi.listTemplates(
        workspaceId,
        this.filtersSignal(),
        this.requestContext(),
      );
      this.templatesSignal.set(templates);
    });
  }

  async createTemplate(payload: CreatePromptTemplateRequest): Promise<PromptActionResult> {
    return this.saveTemplate(payload);
  }

  async updateTemplate(
    templateId: string,
    payload: UpdatePromptTemplateRequest,
  ): Promise<PromptActionResult> {
    return this.saveTemplate(payload, templateId);
  }

  async deleteTemplate(templateId: string): Promise<PromptActionResult> {
    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return fieldErrorsResult({ workspaceId: 'Select a workspace before deleting templates.' });
    }

    if (!this.canManageTemplates()) {
      return failureResult('You do not have permission to delete prompt templates.');
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await this.promptApi.deleteTemplate(workspaceId, templateId, this.requestContext());
      this.templatesSignal.update((templates) =>
        templates.filter((template) => template.id !== templateId),
      );
      this.notifications.success('Template deleted', 'The prompt template has been removed.');
      return successResult();
    } catch (error) {
      return this.handleFailure(error, 'Delete template failed');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private async saveTemplate(
    payload: CreatePromptTemplateRequest,
    templateId?: string,
  ): Promise<PromptActionResult> {
    const fieldErrors = validateTemplatePayload(payload);
    if (Object.keys(fieldErrors).length > 0) {
      return fieldErrorsResult(fieldErrors);
    }

    if (!this.canManageTemplates()) {
      return failureResult('You do not have permission to manage prompt templates.');
    }

    const workspaceId = this.resolveWorkspaceId();
    if (!workspaceId) {
      return fieldErrorsResult({ workspaceId: 'Select a workspace before saving templates.' });
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const template = templateId
        ? await this.promptApi.updateTemplate(workspaceId, templateId, payload, this.requestContext())
        : await this.promptApi.createTemplate(workspaceId, payload, this.requestContext());

      this.templatesSignal.update((templates) => upsertTemplate(templates, template));
      this.notifications.success(
        templateId ? 'Template updated' : 'Template created',
        `${template.name} is available in this workspace.`,
      );
      return successResult();
    } catch (error) {
      return this.handleFailure(error, templateId ? 'Update template failed' : 'Create template failed');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private async runLoader(task: () => Promise<void>): Promise<PromptActionResult> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await task();
      return successResult();
    } catch (error) {
      return this.handleFailure(error, 'Prompt templates');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  private handleFailure(error: unknown, title: string): PromptActionResult {
    const normalized = normalizeHttpError(error);
    this.errorSignal.set(normalized.message);
    this.notifications.error(title, normalized.message);
    return failureResult(normalized.message);
  }

  private resolveWorkspaceId(): string | null {
    return this.auth.activeWorkspaceId();
  }

  private requestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}

function upsertTemplate(
  templates: readonly PromptTemplate[],
  template: PromptTemplate,
): readonly PromptTemplate[] {
  const index = templates.findIndex((item) => item.id === template.id);
  if (index === -1) {
    return [template, ...templates];
  }

  return templates.map((item, itemIndex) => (itemIndex === index ? template : item));
}
