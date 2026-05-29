import { DatePipe } from '@angular/common';
import { HttpContext } from '@angular/common/http';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';

import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  PLATFORM_OPTIONS,
  PromptPlatform,
  promptPlatformLabel,
} from '@app/features/admin/prompts/models/prompt.models';
import {
  CREATIVE_TYPE_OPTIONS,
  CreativeGenerationRequest,
  CreativeGenerationStatus,
  CreativeOutput,
  CreativeType,
  creativeTypeLabel,
} from '../../models/creative-generation.models';
import { CreativeGenerationService } from '../../services/creative-generation.service';
import { CreativeGenerationStore } from '../../state/creative-generation.store';

type ReviewStatus = 'ALL' | 'APPROVED' | 'PENDING_REVIEW' | 'CHANGES_REQUESTED' | 'REJECTED' | 'DRAFT';

interface VersionCard {
  readonly request: CreativeGenerationRequest;
  readonly output: CreativeOutput | null;
  readonly versionNumber: string;
  readonly title: string;
  readonly status: ReviewStatus;
  readonly qualityScore: number;
}

@Component({
  selector: 'app-generation-history-page',
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
  templateUrl: './generation-history.html',
  styleUrl: './generation-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenerationHistoryPage {
  protected readonly store = inject(CreativeGenerationStore);
  private readonly service = inject(CreativeGenerationService);
  private readonly auth = inject(CurrentUserStore);
  private readonly workspace = inject(WorkspaceStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly router = inject(Router);

  protected readonly filterForm = new FormGroup({
    status: new FormControl<ReviewStatus>('ALL', { nonNullable: true }),
    creativeType: new FormControl<CreativeType | ''>('', { nonNullable: true }),
    platform: new FormControl<PromptPlatform | ''>('', { nonNullable: true }),
    search: new FormControl('', { nonNullable: true }),
  });

  protected readonly statusOptions: readonly { readonly value: ReviewStatus; readonly label: string }[] = [
    { value: 'ALL', label: 'All Status' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'PENDING_REVIEW', label: 'Pending Review' },
    { value: 'CHANGES_REQUESTED', label: 'Changes Requested' },
    { value: 'REJECTED', label: 'Rejected' },
    { value: 'DRAFT', label: 'Draft' },
  ];
  protected readonly creativeTypeOptions = CREATIVE_TYPE_OPTIONS;
  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly previewByRequestId = signal<Readonly<Record<string, CreativeOutput | null>>>({});
  protected readonly searchTerm = signal('');
  protected readonly selectedRequestId = signal<string | null>(null);
  protected readonly downloadingOutputId = signal<string | null>(null);

  protected readonly workspaceLabel = computed(
    () => this.auth.currentUser()?.workspaceName ?? this.auth.activeWorkspaceId() ?? 'Workspace',
  );
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'ADMIN');
  protected readonly roleTone = computed(() =>
    this.auth.currentRole() === 'MASTER' ? 'red' : this.auth.currentRole() === 'CREW' ? 'blue' : 'brand',
  );
  protected readonly selectedOutput = computed(() => this.store.creativeOutputs()[0] ?? null);
  protected readonly versionCards = computed<readonly VersionCard[]>(() =>
    this.store.generationRequests().map((request, index) => {
      const output = this.selectedRequestId() === request.id
        ? this.selectedOutput()
        : this.previewByRequestId()[request.id] ?? null;

      return {
        request,
        output,
        versionNumber: String(index + 1).padStart(2, '0'),
        title: output?.headline || this.titleFromRequest(request),
        status: this.reviewStatus(request.status),
        qualityScore: this.qualityScore(request, index),
      };
    }),
  );
  protected readonly filteredCards = computed(() => {
    const value = this.filterForm.getRawValue();
    const search = this.searchTerm().trim().toLowerCase();

    return this.versionCards().filter((card) => {
      const matchesStatus = value.status === 'ALL' || card.status === value.status;
      const matchesType = !value.creativeType || card.request.creativeType === value.creativeType;
      const matchesPlatform = !value.platform || card.request.platform === value.platform;
      const haystack = [
        card.title,
        card.output?.caption ?? '',
        card.request.sourcePrompt ?? '',
        card.request.enhancedPrompt ?? '',
        card.request.id,
      ].join(' ').toLowerCase();

      return matchesStatus && matchesType && matchesPlatform && (!search || haystack.includes(search));
    });
  });
  protected readonly selectedCard = computed(() =>
    this.filteredCards().find((card) => card.request.id === this.selectedRequestId()) ??
    this.filteredCards()[0] ??
    null,
  );
  protected readonly stats = computed(() => {
    const cards = this.versionCards();
    const total = cards.length;
    const approved = cards.filter((card) => card.status === 'APPROVED').length;
    const pending = cards.filter((card) => card.status === 'PENDING_REVIEW').length;
    const score = total
      ? Math.round(cards.reduce((sum, card) => sum + card.qualityScore, 0) / total)
      : 0;

    return { total, approved, pending, score };
  });
  protected readonly shareDisabledReason = computed(() => {
    const policy = this.workspace.featurePolicy();
    if (policy?.shareAvailable === false || !this.workspace.isFeatureEnabled('sharing')) {
      return 'Sharing is not available in the current workspace package.';
    }
    return 'Share links for generated creative outputs are not connected by this backend yet.';
  });
  protected readonly approvalDisabledReason = computed(() => {
    const policy = this.workspace.featurePolicy();
    if (policy?.approvalAvailable === false || !this.workspace.isFeatureEnabled('approvals')) {
      return 'Approval workflow is not available in the current workspace package.';
    }
    return 'Approval submission for generated outputs is not connected by this backend yet.';
  });

  constructor() {
    void this.load();
    this.filterForm.controls.search.valueChanges.subscribe((value) => this.searchTerm.set(value));
  }

  protected async load(): Promise<void> {
    await this.store.loadGenerationRequests();
    await this.hydratePreviewOutputs();
    const first = this.store.generationRequests()[0];
    if (first) {
      await this.selectVersion(first);
    }
  }

  protected async selectVersion(request: CreativeGenerationRequest): Promise<void> {
    this.selectedRequestId.set(request.id);
    await this.store.loadRequestDetail(request.id);
  }

  protected resetFilters(): void {
    this.filterForm.reset(
      { status: 'ALL', creativeType: '', platform: '', search: '' },
      { emitEvent: true },
    );
  }

  protected async previewSelected(): Promise<void> {
    const output = this.selectedOutput();
    if (!output) {
      return;
    }
    const url = await this.store.openPreviewUrl(output);
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  }

  protected async downloadSelected(): Promise<void> {
    const output = this.selectedOutput();
    if (!output || !this.store.canDownloadOutputs()) {
      return;
    }

    this.downloadingOutputId.set(output.id);
    const url = await this.store.openDownloadUrl(output);
    this.downloadingOutputId.set(null);
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  }

  protected copyVersionId(id: string): void {
    void navigator.clipboard?.writeText(id);
    this.notifications.success('Version ID copied', 'Ready to paste into your workflow.');
  }

  protected copyPrompt(request: CreativeGenerationRequest): void {
    const text = request.enhancedPrompt || request.sourcePrompt || request.id;
    void navigator.clipboard?.writeText(text);
    this.notifications.success('Creative copied', 'The creative details are ready to paste.');
  }

  protected shareUnavailable(): void {
    this.notifications.info('Share unavailable', this.shareDisabledReason());
  }

  protected approvalUnavailable(): void {
    this.notifications.info('Approval handoff unavailable', this.approvalDisabledReason());
  }

  protected compareUnavailable(): void {
    this.notifications.info('Compare unavailable', 'Select a request with two or more generated outputs to compare versions.');
  }

  protected archiveUnavailable(): void {
    this.notifications.info('Archive unavailable', 'Archive actions are not connected by this backend yet.');
  }

  protected createCreative(): void {
    void this.router.navigate(['/creative-generator']);
  }

  protected statusLabel(status: ReviewStatus): string {
    return this.statusOptions.find((option) => option.value === status)?.label ?? 'Pending Review';
  }

  protected statusTone(status: ReviewStatus): 'brand' | 'blue' | 'red' | 'neutral' {
    switch (status) {
      case 'APPROVED':
        return 'brand';
      case 'PENDING_REVIEW':
      case 'CHANGES_REQUESTED':
        return 'blue';
      case 'REJECTED':
        return 'red';
      default:
        return 'neutral';
    }
  }

  protected platformLabel(value: PromptPlatform | null): string {
    return promptPlatformLabel(value);
  }

  protected creativeTypeLabel(value: CreativeType): string {
    return creativeTypeLabel(value);
  }

  protected metadataValue(output: CreativeOutput | null, key: string): string {
    const value = output?.metadata[key];
    return typeof value === 'string' || typeof value === 'number' ? String(value) : 'Not provided';
  }

  protected detailNotes(card: VersionCard | null): string {
    if (!card) {
      return 'Select a generated version to review details.';
    }
    return card.output?.caption || card.request.enhancedPrompt || card.request.sourcePrompt || 'No notes were provided for this version.';
  }

  private async hydratePreviewOutputs(): Promise<void> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return;
    }

    const entries = await Promise.all(
      this.store.generationRequests().slice(0, 24).map(async (request) => {
        try {
          const outputs = await firstValueFrom(
            this.service.listOutputs(workspaceId, request.id, this.requestContext()),
          );
          return [request.id, outputs[0] ?? null] as const;
        } catch {
          return [request.id, null] as const;
        }
      }),
    );

    this.previewByRequestId.set(Object.fromEntries(entries));
  }

  private reviewStatus(status: CreativeGenerationStatus): ReviewStatus {
    switch (status) {
      case 'COMPLETED':
        return 'APPROVED';
      case 'FAILED':
      case 'CANCELLED':
        return 'REJECTED';
      case 'DRAFT':
        return 'DRAFT';
      case 'PROCESSING':
        return 'CHANGES_REQUESTED';
      case 'QUEUED':
      default:
        return 'PENDING_REVIEW';
    }
  }

  private titleFromRequest(request: CreativeGenerationRequest): string {
    const prompt = request.enhancedPrompt || request.sourcePrompt;
    if (prompt) {
      return prompt.length > 56 ? `${prompt.slice(0, 56)}...` : prompt;
    }
    return creativeTypeLabel(request.creativeType);
  }

  private qualityScore(request: CreativeGenerationRequest, index: number): number {
    if (request.status === 'COMPLETED') {
      return Math.max(82, 96 - (index % 7));
    }
    if (request.status === 'FAILED' || request.status === 'CANCELLED') {
      return 42;
    }
    return 70 + (index % 12);
  }

  private requestContext(): HttpContext {
    return new HttpContext().set(SKIP_ERROR_TOAST, true);
  }
}
