import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { GeneratedVersionStore } from '@app/features/generated-versions/generated-version.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  ApprovalDecision,
  ApprovalItem,
  approvalStatusLabel,
  approvalStatusTone,
} from '../../approval.models';
import { ApprovalStore } from '../../approval.store';

@Component({
  selector: 'app-approval-queue-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    ModalShellComponent,
    PageHeaderComponent,
  ],
  templateUrl: './approval-queue.html',
  styleUrl: './approval-queue.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ApprovalQueuePage {
  protected readonly store = inject(ApprovalStore);
  protected readonly workspace = inject(WorkspaceStore);
  private readonly permissions = inject(PermissionStore);
  private readonly generatedVersions = inject(GeneratedVersionStore);

  private readonly commentOpenSignal = signal(false);
  private readonly shareOpenSignal = signal(false);
  private readonly pendingDecisionSignal = signal<ApprovalDecision | null>(null);
  private readonly shareLinkSignal = signal<{ readonly url: string; readonly expiresAt?: string | null } | null>(null);
  private readonly shareLoadingSignal = signal(false);
  private readonly downloadingIdSignal = signal<string | null>(null);

  protected readonly commentOpen = this.commentOpenSignal.asReadonly();
  protected readonly shareOpen = this.shareOpenSignal.asReadonly();
  protected readonly pendingDecision = this.pendingDecisionSignal.asReadonly();
  protected readonly shareLink = this.shareLinkSignal.asReadonly();
  protected readonly shareLoading = this.shareLoadingSignal.asReadonly();
  protected readonly downloadingId = this.downloadingIdSignal.asReadonly();

  protected readonly noteControl = new FormControl('', { nonNullable: true });
  protected readonly approvalStatusLabel = approvalStatusLabel;
  protected readonly approvalStatusTone = approvalStatusTone;

  protected readonly approvalAvailable = computed(() => {
    const policy = this.workspace.featurePolicy();
    if (policy?.approvalAvailable === false) {
      return false;
    }

    return this.workspace.isFeatureEnabled('approvals') && this.workspace.isFeatureEnabled('approval.workflow');
  });
  protected readonly shareAvailable = computed(() => {
    const policy = this.workspace.featurePolicy();
    if (policy?.shareAvailable === false) {
      return false;
    }

    return this.workspace.isFeatureEnabled('sharing') && this.workspace.isFeatureEnabled('generatedVersions.share');
  });
  protected readonly hasActiveSubscription = computed(() => {
    const status = this.workspace.subscription()?.status?.toLowerCase();
    return !status || ['active', 'trialing'].includes(status);
  });
  protected readonly approvalUnavailableMessage = computed(() =>
    this.approvalAvailable() && this.hasActiveSubscription()
      ? null
      : 'Approval workflow is not available in your current package. Upgrade your package to enable approvals.',
  );
  protected readonly shareUnavailableMessage = computed(() =>
    this.shareAvailable() && this.hasActiveSubscription()
      ? null
      : 'Public sharing is not available in your current package. Upgrade your package to enable share links.',
  );

  constructor() {
    void this.store.load();
  }

  protected reload(): void {
    void this.store.load();
  }

  protected selectApproval(approval: ApprovalItem): void {
    this.store.selectApproval(approval);
  }

  protected canApprove(approval: ApprovalItem): boolean {
    return (
      this.approvalAvailable() &&
      this.hasActiveSubscription() &&
      approval.capabilities?.canApprove === true &&
      this.permissions.canUseFeature('approvals')
    );
  }

  protected canReject(approval: ApprovalItem): boolean {
    return (
      this.approvalAvailable() &&
      this.hasActiveSubscription() &&
      approval.capabilities?.canReject === true &&
      this.permissions.canUseFeature('approvals')
    );
  }

  protected canShare(approval: ApprovalItem): boolean {
    return (
      this.shareAvailable() &&
      this.hasActiveSubscription() &&
      approval.capabilities?.canShare === true
    );
  }

  protected canDownload(approval: ApprovalItem): boolean {
    return approval.capabilities?.canDownload === true;
  }

  protected openComment(decision: ApprovalDecision, approval: ApprovalItem): void {
    this.store.selectApproval(approval);
    this.pendingDecisionSignal.set(decision);
    this.noteControl.setValue('');
    this.commentOpenSignal.set(true);
  }

  protected closeComment(): void {
    this.commentOpenSignal.set(false);
    this.pendingDecisionSignal.set(null);
    this.noteControl.setValue('');
  }

  protected async submitDecision(): Promise<void> {
    const approval = this.store.selectedApproval();
    const decision = this.pendingDecisionSignal();
    if (!approval || !decision) {
      return;
    }

    const result = await this.store.decide(approval.id, {
      decision,
      note: this.noteControl.value.trim() || null,
    });

    if (result.ok) {
      this.closeComment();
    }
  }

  protected async openShare(approval: ApprovalItem): Promise<void> {
    this.store.selectApproval(approval);
    this.shareLinkSignal.set(null);
    this.shareOpenSignal.set(true);

    if (!this.canShare(approval)) {
      return;
    }

    this.shareLoadingSignal.set(true);
    const link = await this.generatedVersions.getShareUrl(approval.generatedVersionId);
    this.shareLoadingSignal.set(false);
    this.shareLinkSignal.set(link);
  }

  protected closeShare(): void {
    this.shareOpenSignal.set(false);
    this.shareLinkSignal.set(null);
  }

  protected copyShareLink(): void {
    const link = this.shareLinkSignal();
    if (!link?.url) {
      return;
    }

    void navigator.clipboard.writeText(link.url);
  }

  protected async download(approval: ApprovalItem): Promise<void> {
    if (!this.canDownload(approval)) {
      return;
    }

    this.downloadingIdSignal.set(approval.id);
    const link = await this.generatedVersions.getDownloadUrl(approval.generatedVersionId);
    this.downloadingIdSignal.set(null);

    if (link?.url) {
      window.open(link.url, '_blank', 'noopener,noreferrer');
    }
  }
}
