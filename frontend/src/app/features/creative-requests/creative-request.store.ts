import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  CreateCreativeRequestPayload,
  CreativeRequest,
  CreativeRequestActionResult,
} from './creative-request.models';
import { CreativeRequestApiService } from './creative-request-api.service';

@Injectable({ providedIn: 'root' })
export class CreativeRequestStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly api = inject(CreativeRequestApiService);

  private readonly requestsSignal = signal<readonly CreativeRequest[]>([]);
  private readonly selectedRequestSignal = signal<CreativeRequest | null>(null);
  private readonly projectIdSignal = signal<string | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly savingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly requests = this.requestsSignal.asReadonly();
  readonly selectedRequest = this.selectedRequestSignal.asReadonly();
  readonly projectId = this.projectIdSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly saving = this.savingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly hasRequests = computed(() => this.requestsSignal().length > 0);

  selectRequest(request: CreativeRequest | null): void {
    this.selectedRequestSignal.set(request);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  async loadByProject(projectId: string): Promise<CreativeRequestActionResult> {
    const workspaceId = this.auth.activeWorkspaceId();
    this.projectIdSignal.set(projectId);

    if (!workspaceId) {
      return { ok: false, fieldErrors: { workspaceId: 'Select a workspace before loading requests.' } };
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const requests = await this.api.listByProject(workspaceId, projectId);
      this.requestsSignal.set(requests);
      this.syncSelectedRequest();
      return { ok: true, fieldErrors: {} };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.errorSignal.set(normalized.message);
      this.notifications.error('Creative requests', normalized.message);
      return { ok: false, message: normalized.message, fieldErrors: {} };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async loadOne(creativeRequestId: string): Promise<CreativeRequestActionResult> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return { ok: false, fieldErrors: { workspaceId: 'Select a workspace before loading this request.' } };
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const request = await this.api.get(workspaceId, creativeRequestId);
      this.selectedRequestSignal.set(request);
      this.requestsSignal.update((requests) => upsertRequest(requests, request));
      return { ok: true, fieldErrors: {} };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.errorSignal.set(normalized.message);
      this.notifications.error('Creative request', normalized.message);
      return { ok: false, message: normalized.message, fieldErrors: {} };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async create(
    projectId: string,
    payload: CreateCreativeRequestPayload,
  ): Promise<CreativeRequestActionResult> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return { ok: false, fieldErrors: { workspaceId: 'Select a workspace before creating a request.' } };
    }

    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const request = await this.api.create(workspaceId, projectId, payload);
      this.requestsSignal.update((requests) => upsertRequest(requests, request));
      this.selectedRequestSignal.set(request);
      this.notifications.success('Creative request created', 'Generation has been queued for this campaign.');
      return { ok: true, fieldErrors: {} };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.errorSignal.set(normalized.message);
      this.notifications.error('Create request failed', normalized.message);
      return { ok: false, message: normalized.message, fieldErrors: {} };
    } finally {
      this.savingSignal.set(false);
    }
  }

  private syncSelectedRequest(): void {
    const selected = this.selectedRequestSignal();
    if (!selected) {
      return;
    }

    this.selectedRequestSignal.set(
      this.requestsSignal().find((request) => request.id === selected.id) ?? null,
    );
  }
}

function upsertRequest(
  requests: readonly CreativeRequest[],
  request: CreativeRequest,
): readonly CreativeRequest[] {
  const index = requests.findIndex((item) => item.id === request.id);
  if (index === -1) {
    return [request, ...requests];
  }

  return requests.map((item, itemIndex) => (itemIndex === index ? request : item));
}
