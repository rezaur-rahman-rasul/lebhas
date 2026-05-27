import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  ApprovalActionResult,
  ApprovalDecisionPayload,
  ApprovalItem,
} from './approval.models';
import { ApprovalApiService } from './approval-api.service';

@Injectable({ providedIn: 'root' })
export class ApprovalStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly api = inject(ApprovalApiService);

  private readonly approvalsSignal = signal<readonly ApprovalItem[]>([]);
  private readonly selectedApprovalSignal = signal<ApprovalItem | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly decidingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly approvals = this.approvalsSignal.asReadonly();
  readonly selectedApproval = this.selectedApprovalSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly deciding = this.decidingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly hasApprovals = computed(() => this.approvalsSignal().length > 0);

  selectApproval(approval: ApprovalItem | null): void {
    this.selectedApprovalSignal.set(approval);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  async load(): Promise<ApprovalActionResult> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return { ok: false, fieldErrors: { workspaceId: 'Select a workspace before loading approvals.' } };
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const approvals = await this.api.list(workspaceId);
      this.approvalsSignal.set(approvals);
      this.syncSelectedApproval();
      return { ok: true, fieldErrors: {} };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.errorSignal.set(normalized.message);
      return { ok: false, message: normalized.message, fieldErrors: {} };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async decide(approvalId: string, payload: ApprovalDecisionPayload): Promise<ApprovalActionResult> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return { ok: false, fieldErrors: { workspaceId: 'Select a workspace before reviewing approvals.' } };
    }

    this.decidingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const approval = await this.api.decide(workspaceId, approvalId, payload);
      this.approvalsSignal.update((approvals) => upsertApproval(approvals, approval));
      this.selectedApprovalSignal.set(approval);
      this.notifications.success('Approval updated', 'Your review decision has been saved.');
      return { ok: true, fieldErrors: {} };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.errorSignal.set(normalized.message);
      this.notifications.error('Approval failed', normalized.message);
      return { ok: false, message: normalized.message, fieldErrors: {} };
    } finally {
      this.decidingSignal.set(false);
    }
  }

  private syncSelectedApproval(): void {
    const selected = this.selectedApprovalSignal();
    if (!selected) {
      return;
    }

    this.selectedApprovalSignal.set(
      this.approvalsSignal().find((approval) => approval.id === selected.id) ?? null,
    );
  }
}

function upsertApproval(
  approvals: readonly ApprovalItem[],
  approval: ApprovalItem,
): readonly ApprovalItem[] {
  const index = approvals.findIndex((item) => item.id === approval.id);
  if (index === -1) {
    return [approval, ...approvals];
  }

  return approvals.map((item, itemIndex) => (itemIndex === index ? approval : item));
}
