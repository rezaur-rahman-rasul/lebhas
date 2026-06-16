import { DatePipe } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { PLATFORM_OPTIONS, PromptPlatform, promptPlatformLabel } from '@app/features/admin/prompts/models/prompt.models';
import { GeneratedVersion, generatedVersionMetadataValue } from '@app/features/generated-versions/generated-version.models';
import { GeneratedVersionStore } from '@app/features/generated-versions/generated-version.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  CREATIVE_TYPE_OPTIONS,
  CreativeType,
  creativeTypeLabel,
} from '../../models/creative-generation.models';

type ReviewStatus = 'ALL' | 'READY' | 'COMPLETED' | 'PROCESSING' | 'PENDING_REVIEW' | 'APPROVED' | 'FAILED';

interface VersionCard {
  readonly version: GeneratedVersion;
  readonly versionNumber: string;
  readonly title: string;
  readonly status: ReviewStatus;
  readonly previewUrl: string | null;
  readonly downloadUrl: string | null;
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
  protected readonly versionStore = inject(GeneratedVersionStore);
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
    { value: 'READY', label: 'Ready' },
    { value: 'COMPLETED', label: 'Completed' },
    { value: 'PROCESSING', label: 'Processing' },
    { value: 'PENDING_REVIEW', label: 'Pending Review' },
    { value: 'APPROVED', label: 'Approved' },
    { value: 'FAILED', label: 'Failed' },
  ];
  protected readonly creativeTypeOptions = CREATIVE_TYPE_OPTIONS;
  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly searchTerm = signal('');
  protected readonly selectedVersionId = signal<string | null>(null);
  protected readonly downloadingVersionId = signal<string | null>(null);

  protected readonly workspaceLabel = computed(
    () => this.auth.currentUser()?.workspaceName ?? this.auth.activeWorkspaceId() ?? 'Workspace',
  );
  protected readonly roleLabel = computed(() => this.auth.currentRole() ?? 'ADMIN');
  protected readonly roleTone = computed(() =>
    this.auth.currentRole() === 'MASTER' ? 'red' : this.auth.currentRole() === 'CREW' ? 'blue' : 'brand',
  );
  protected readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));
  protected readonly canViewVersions = computed(() => this.auth.hasPermission('WORKSPACE_VIEW'));
  protected readonly versionCards = computed<readonly VersionCard[]>(() =>
    this.versionStore.versions().map((version, index) => ({
      version,
      versionNumber: String(version.versionNumber ?? index + 1).padStart(2, '0'),
      title: this.titleFromVersion(version),
      status: this.reviewStatus(version),
      previewUrl: previewUrl(version),
      downloadUrl: downloadUrl(version),
      qualityScore: this.qualityScore(version, index),
    })),
  );
  protected readonly filteredCards = computed(() => {
    const value = this.filterForm.getRawValue();
    const search = this.searchTerm().trim().toLowerCase();

    return this.versionCards().filter((card) => {
      const matchesStatus = value.status === 'ALL' || card.status === value.status;
      const matchesType = !value.creativeType || card.version.creativeType === value.creativeType;
      const matchesPlatform = !value.platform || card.version.platform === value.platform;
      const haystack = [
        card.title,
        card.version.id,
        card.version.creativeRequestId,
        card.version.versionName ?? '',
        card.version.platform ?? '',
        card.version.creativeType ?? '',
      ].join(' ').toLowerCase();

      return matchesStatus && matchesType && matchesPlatform && (!search || haystack.includes(search));
    });
  });
  protected readonly selectedCard = computed(() =>
    this.filteredCards().find((card) => card.version.id === this.selectedVersionId()) ??
    this.filteredCards()[0] ??
    null,
  );
  protected readonly stats = computed(() => {
    const cards = this.versionCards();
    const total = cards.length;
    const approved = cards.filter((card) => card.status === 'APPROVED').length;
    const pending = cards.filter((card) => card.status === 'PENDING_REVIEW' || card.status === 'PROCESSING').length;
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
    return 'Use the generated version detail page to manage share links.';
  });
  protected readonly approvalDisabledReason = computed(() => {
    const policy = this.workspace.featurePolicy();
    if (policy?.approvalAvailable === false || !this.workspace.isFeatureEnabled('approvals')) {
      return 'Approval workflow is not available in the current workspace package.';
    }
    return 'Use the generated version detail page to manage approval workflow.';
  });

  constructor() {
    void this.load();
    this.filterForm.valueChanges.subscribe(() => {
      const value = this.filterForm.getRawValue();
      this.searchTerm.set(value.search);
      void this.load({
        status: value.status === 'ALL' ? null : value.status,
        creativeType: value.creativeType || null,
        platform: value.platform || null,
        search: value.search || null,
      });
    });
    effect(() => {
      const selected = this.selectedCard();
      this.versionStore.selectVersion(selected?.version ?? null);
    });
  }

  protected async load(filters = this.currentFilters()): Promise<void> {
    await this.versionStore.load(filters);
    if (!this.selectedVersionId() && this.versionStore.versions().length > 0) {
      this.selectedVersionId.set(this.versionStore.versions()[0].id);
    }
  }

  protected selectVersion(version: GeneratedVersion): void {
    this.selectedVersionId.set(version.id);
    this.versionStore.selectVersion(version);
  }

  protected resetFilters(): void {
    this.filterForm.reset(
      { status: 'ALL', creativeType: '', platform: '', search: '' },
      { emitEvent: true },
    );
  }

  protected async previewSelected(): Promise<void> {
    const card = this.selectedCard();
    if (card?.previewUrl) {
      window.open(card.previewUrl, '_blank', 'noopener,noreferrer');
      return;
    }
    this.notifications.info('Preview preparing', 'The generated version exists, but the preview asset is still being prepared.');
  }

  protected async downloadSelected(): Promise<void> {
    const card = this.selectedCard();
    if (!card || !this.canDownload(card.version)) {
      return;
    }

    this.downloadingVersionId.set(card.version.id);
    const link = await this.versionStore.getDownloadUrl(card.version.id);
    this.downloadingVersionId.set(null);
    const url = link?.url ?? card.downloadUrl;
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  }

  protected copyVersionId(id: string): void {
    void navigator.clipboard?.writeText(id);
    this.notifications.success('Version ID copied', 'Ready to paste into your workflow.');
  }

  protected copyPrompt(version: GeneratedVersion): void {
    const text = version.versionName || version.id;
    void navigator.clipboard?.writeText(text);
    this.notifications.success('Version copied', 'The generated version details are ready to paste.');
  }

  protected shareUnavailable(): void {
    this.notifications.info('Share unavailable', this.shareDisabledReason());
  }

  protected approvalUnavailable(): void {
    this.notifications.info('Approval handoff unavailable', this.approvalDisabledReason());
  }

  protected compareUnavailable(): void {
    this.notifications.info('Compare unavailable', 'Select a request with two or more generated versions to compare.');
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
      case 'READY':
      case 'COMPLETED':
      case 'APPROVED':
        return 'brand';
      case 'PENDING_REVIEW':
      case 'PROCESSING':
        return 'blue';
      case 'FAILED':
        return 'red';
      default:
        return 'neutral';
    }
  }

  protected platformLabel(value: string | null | undefined): string {
    return promptPlatformLabel((value ?? null) as PromptPlatform | null);
  }

  protected creativeTypeLabel(value: string | null | undefined): string {
    return creativeTypeLabel((value ?? null) as CreativeType | null);
  }

  protected metadataValue(version: GeneratedVersion | null, key: string): string {
    if (!version) {
      return 'Not provided';
    }
    const value = generatedVersionMetadataValue(version, key);
    return value === null ? 'Not provided' : String(value);
  }

  protected detailNotes(card: VersionCard | null): string {
    if (!card) {
      return 'Select a generated version to review details.';
    }
    if (!card.previewUrl) {
      return 'Preview preparing. The generated version is stored and will show the image as soon as the asset URL is ready.';
    }
    return card.version.versionName || 'Generated creative version.';
  }

  protected canDownload(version: GeneratedVersion): boolean {
    return Boolean(downloadUrl(version) || version.capabilities?.canDownload === true);
  }

  private currentFilters() {
    const value = this.filterForm.getRawValue();
    return {
      status: value.status === 'ALL' ? null : value.status,
      creativeType: value.creativeType || null,
      platform: value.platform || null,
      search: value.search || null,
    };
  }

  private reviewStatus(version: GeneratedVersion): ReviewStatus {
    const approvalStatus = normalizeStatus(version.approvalStatus);
    if (approvalStatus === 'APPROVED') {
      return 'APPROVED';
    }
    if (approvalStatus === 'SUBMITTED' || approvalStatus === 'IN_REVIEW' || approvalStatus === 'RESUBMITTED') {
      return 'PENDING_REVIEW';
    }

    const generationStatus = normalizeStatus(version.generationStatus ?? version.status);
    if (generationStatus === 'READY') {
      return 'READY';
    }
    if (generationStatus === 'COMPLETED') {
      return 'COMPLETED';
    }
    if (generationStatus === 'FAILED') {
      return 'FAILED';
    }
    return 'PROCESSING';
  }

  private titleFromVersion(version: GeneratedVersion): string {
    if (version.versionName) {
      return version.versionName;
    }
    const type = this.creativeTypeLabel(version.creativeType);
    const platform = this.platformLabel(version.platform);
    return [platform, type, version.versionNumber ? `v${version.versionNumber}` : 'Version']
      .filter(Boolean)
      .join(' - ');
  }

  private qualityScore(version: GeneratedVersion, index: number): number {
    const status = this.reviewStatus(version);
    if (status === 'READY' || status === 'COMPLETED' || status === 'APPROVED') {
      return Math.max(82, 96 - (index % 7));
    }
    if (status === 'FAILED') {
      return 42;
    }
    return 70 + (index % 12);
  }
}

function previewUrl(version: GeneratedVersion): string | null {
  return firstString(
    version.previewUrl,
    version.signedPreviewUrl,
    version.thumbnailUrl,
    nestedString(version.asset, 'previewUrl'),
    nestedString(version.generatedAsset, 'previewUrl'),
    nestedString(version.urls, 'preview'),
    nestedString(version.urls, 'previewUrl'),
    nestedString(version.urls, 'signedPreviewUrl'),
  );
}

function downloadUrl(version: GeneratedVersion): string | null {
  return firstString(
    version.downloadUrl,
    version.signedDownloadUrl,
    nestedString(version.asset, 'downloadUrl'),
    nestedString(version.generatedAsset, 'downloadUrl'),
    nestedString(version.urls, 'download'),
    nestedString(version.urls, 'downloadUrl'),
    nestedString(version.urls, 'signedDownloadUrl'),
  );
}

function firstString(...values: readonly unknown[]): string | null {
  for (const value of values) {
    if (typeof value === 'string' && value.trim()) {
      return value;
    }
  }
  return null;
}

function nestedString(record: Readonly<Record<string, unknown>> | null | undefined, key: string): string | null {
  const value = record?.[key];
  return typeof value === 'string' && value.trim() ? value : null;
}

function normalizeStatus(status: string | null | undefined): string {
  return (status ?? '').trim().toUpperCase().replace(/[\s-]+/g, '_');
}
