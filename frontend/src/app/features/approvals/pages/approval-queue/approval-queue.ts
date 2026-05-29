import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { GeneratedVersionStore } from '@app/features/generated-versions/generated-version.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  ApprovalDecision,
  ApprovalItem,
  ApprovalStatus,
  approvalStatusLabel,
  approvalStatusTone,
} from '../../approval.models';
import { ApprovalStore } from '../../approval.store';

type ApprovalFilterStatus = 'ALL' | 'IN_REVIEW' | 'APPROVED' | 'CHANGES_REQUESTED' | 'REJECTED';

interface LocalComment {
  readonly approvalId: string;
  readonly note: string;
  readonly createdAt: string;
}

@Component({
  selector: 'app-approval-queue-page',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    IconComponent,
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
  private readonly notifications = inject(NotificationStateService);

  protected readonly filters = new FormGroup({
    status: new FormControl<ApprovalFilterStatus>('ALL', { nonNullable: true }),
    search: new FormControl('', { nonNullable: true }),
  });
  protected readonly commentControl = new FormControl('', { nonNullable: true });
  protected readonly decisionNoteControl = new FormControl('', { nonNullable: true });

  protected readonly statusOptions: readonly { readonly value: ApprovalFilterStatus; readonly label: string }[] = [
    { value: 'ALL', label: 'All Status' },
    { value: 'IN_REVIEW', label: 'Pending Review' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'CHANGES_REQUESTED', label: 'Changes Requested' },
    { value: 'REJECTED', label: 'Rejected' },
  ];
  protected readonly checklist = [
    'Brand Guidelines',
    'Copy & Messaging',
    'Visual Design',
    'CTA & Links',
    'Legal & Compliance',
  ] as const;

  private readonly searchTerm = signal('');
  private readonly selectedStatus = signal<ApprovalFilterStatus>('ALL');
  private readonly localCommentsSignal = signal<readonly LocalComment[]>([]);
  private readonly pendingDecisionSignal = signal<ApprovalDecision | null>(null);
  private readonly decisionPanelOpenSignal = signal(false);
  private readonly shareLinkSignal = signal<{ readonly url: string; readonly expiresAt?: string | null } | null>(null);
  private readonly shareLoadingSignal = signal(false);
  private readonly downloadingIdSignal = signal<string | null>(null);
  private readonly pageSignal = signal(0);
  private readonly pageSize = 8;

  protected readonly approvalStatusLabel = approvalStatusLabel;
  protected readonly approvalStatusTone = approvalStatusTone;
  protected readonly pendingDecision = this.pendingDecisionSignal.asReadonly();
  protected readonly decisionPanelOpen = this.decisionPanelOpenSignal.asReadonly();
  protected readonly shareLink = this.shareLinkSignal.asReadonly();
  protected readonly shareLoading = this.shareLoadingSignal.asReadonly();
  protected readonly downloadingId = this.downloadingIdSignal.asReadonly();
  protected readonly page = this.pageSignal.asReadonly();

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
  protected readonly filteredApprovals = computed(() => {
    const status = this.selectedStatus();
    const search = this.searchTerm().trim().toLowerCase();

    return this.store.approvals().filter((approval) => {
      const label = approvalStatusLabel(approval.status);
      const matchesStatus =
        status === 'ALL' ||
        (status === 'IN_REVIEW' && (label === 'Queued' || label === 'In review')) ||
        (status === 'APPROVED' && label === 'Approved') ||
        (status === 'CHANGES_REQUESTED' && label === 'Changes requested') ||
        (status === 'REJECTED' && label === 'Rejected');
      const haystack = [
        approval.title ?? '',
        approval.id,
        approval.generatedVersionId,
        approval.creativeRequestId,
        approval.submittedByName ?? '',
      ].join(' ').toLowerCase();

      return matchesStatus && (!search || haystack.includes(search));
    });
  });
  protected readonly visibleApprovals = computed(() =>
    this.filteredApprovals().slice(this.pageSignal() * this.pageSize, (this.pageSignal() + 1) * this.pageSize),
  );
  protected readonly totalPages = computed(() =>
    Math.max(1, Math.ceil(this.filteredApprovals().length / this.pageSize)),
  );
  protected readonly selectedApproval = computed(() =>
    this.store.selectedApproval() ??
    this.visibleApprovals()[0] ??
    this.filteredApprovals()[0] ??
    null,
  );
  protected readonly selectedApprovalId = computed(() => this.selectedApproval()?.id ?? null);
  protected readonly stats = computed(() => {
    const approvals = this.store.approvals();
    return {
      pending: approvals.filter((item) => {
        const label = approvalStatusLabel(item.status);
        return label === 'Queued' || label === 'In review';
      }).length,
      approved: approvals.filter((item) => approvalStatusLabel(item.status) === 'Approved').length,
      changes: approvals.filter((item) => approvalStatusLabel(item.status) === 'Changes requested').length,
      rejected: approvals.filter((item) => approvalStatusLabel(item.status) === 'Rejected').length,
    };
  });
  protected readonly selectedComments = computed(() => {
    const selected = this.selectedApproval();
    if (!selected) {
      return [];
    }

    return this.localCommentsSignal().filter((comment) => comment.approvalId === selected.id);
  });

  constructor() {
    void this.load();
    this.filters.controls.status.valueChanges.subscribe((value) => {
      this.selectedStatus.set(value);
      this.pageSignal.set(0);
    });
    this.filters.controls.search.valueChanges.subscribe((value) => {
      this.searchTerm.set(value);
      this.pageSignal.set(0);
    });
  }

  protected async load(): Promise<void> {
    const result = await this.store.load();
    if (result.ok) {
      const selected = this.store.selectedApproval() ?? this.store.approvals()[0] ?? null;
      this.store.selectApproval(selected);
    }
  }

  protected reload(): void {
    void this.load();
  }

  protected selectApproval(approval: ApprovalItem): void {
    this.store.selectApproval(approval);
    this.shareLinkSignal.set(null);
    this.decisionPanelOpenSignal.set(false);
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
    return this.shareAvailable() && this.hasActiveSubscription() && approval.capabilities?.canShare === true;
  }

  protected canDownload(approval: ApprovalItem): boolean {
    return approval.capabilities?.canDownload === true;
  }

  protected openDecision(decision: ApprovalDecision, approval: ApprovalItem): void {
    this.store.selectApproval(approval);
    this.pendingDecisionSignal.set(decision);
    this.decisionNoteControl.setValue('');
    this.decisionPanelOpenSignal.set(true);
  }

  protected closeDecision(): void {
    this.pendingDecisionSignal.set(null);
    this.decisionNoteControl.setValue('');
    this.decisionPanelOpenSignal.set(false);
  }

  protected async submitDecision(): Promise<void> {
    const approval = this.selectedApproval();
    const decision = this.pendingDecisionSignal();
    if (!approval || !decision) {
      return;
    }

    const result = await this.store.decide(approval.id, {
      decision,
      note: this.decisionNoteControl.value.trim() || null,
    });

    if (result.ok) {
      this.closeDecision();
    }
  }

  protected postComment(): void {
    const approval = this.selectedApproval();
    const note = this.commentControl.value.trim();
    if (!approval || !note) {
      return;
    }

    this.localCommentsSignal.update((comments) => [
      { approvalId: approval.id, note, createdAt: new Date().toISOString() },
      ...comments,
    ]);
    this.commentControl.setValue('');
    this.notifications.success('Comment added', 'The comment is visible in this review session.');
  }

  protected async openShare(approval: ApprovalItem): Promise<void> {
    this.store.selectApproval(approval);
    this.shareLinkSignal.set(null);

    if (!this.canShare(approval)) {
      this.notifications.info('Share unavailable', this.shareUnavailableMessage() ?? 'Sharing is not available for this approval.');
      return;
    }

    this.shareLoadingSignal.set(true);
    const link = await this.generatedVersions.getShareUrl(approval.generatedVersionId);
    this.shareLoadingSignal.set(false);
    this.shareLinkSignal.set(link);
  }

  protected copyShareLink(): void {
    const link = this.shareLinkSignal();
    if (!link?.url) {
      return;
    }

    void navigator.clipboard.writeText(link.url);
    this.notifications.success('Share link copied', 'Ready to send to reviewers.');
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

  protected previousPage(): void {
    this.pageSignal.update((page) => Math.max(0, page - 1));
  }

  protected nextPage(): void {
    this.pageSignal.update((page) => Math.min(this.totalPages() - 1, page + 1));
  }

  protected reviewers(approval: ApprovalItem): readonly string[] {
    return [approval.submittedByName ?? approval.submittedBy ?? 'Reviewer'].filter(Boolean).slice(0, 3);
  }

  protected dueDate(approval: ApprovalItem): string {
    const updated = Date.parse(approval.updatedAt);
    if (!Number.isFinite(updated)) {
      return 'Not provided';
    }
    return new Date(updated + 3 * 24 * 60 * 60 * 1000).toISOString();
  }

  protected priority(approval: ApprovalItem): string {
    const label = approvalStatusLabel(approval.status);
    return label === 'Queued' || label === 'In review' ? 'Medium' : 'Normal';
  }

  protected statusSummary(status: ApprovalStatus): string {
    const label = approvalStatusLabel(status);
    return label === 'Queued' ? 'Pending Review' : label;
  }

  protected decisionTitle(): string {
    switch (this.pendingDecisionSignal()) {
      case 'APPROVE':
        return 'Approve request';
      case 'REQUEST_CHANGES':
        return 'Request changes';
      case 'REJECT':
        return 'Reject request';
      default:
        return 'Review decision';
    }
  }
}
