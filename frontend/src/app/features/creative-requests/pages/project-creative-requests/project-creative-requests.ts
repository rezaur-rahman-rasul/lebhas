import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { KeyValuePipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { AssetStore } from '@app/features/assets/state/asset.store';
import { BrandStore } from '@app/features/brands/brand.store';
import { ProductServiceStore } from '@app/features/product-services/product-service.store';
import { ProjectStore } from '@app/features/projects/project.store';
import { PromptHistoryStore } from '@app/features/prompts/state/prompt-history.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  CreativeRequest,
  creativeRequestStatusLabel,
  creativeRequestStatusTone,
} from '../../creative-request.models';
import { CreativeRequestStore } from '../../creative-request.store';
import {
  GeneratedVersion,
  generatedVersionStatusLabel,
  generatedVersionStatusTone,
} from '@app/features/generated-versions/generated-version.models';
import { GeneratedVersionStore } from '@app/features/generated-versions/generated-version.store';

const REQUEST_FORMATS = ['Square image', 'Story/Reel', 'Landscape banner', 'Product showcase'] as const;

@Component({
  selector: 'app-project-creative-requests-page',
  standalone: true,
  imports: [
    RouterLink,
    KeyValuePipe,
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    ModalShellComponent,
    PageHeaderComponent,
  ],
  templateUrl: './project-creative-requests.html',
  styleUrl: './project-creative-requests.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProjectCreativeRequestsPage {
  protected readonly requestStore = inject(CreativeRequestStore);
  protected readonly versionStore = inject(GeneratedVersionStore);
  protected readonly workspace = inject(WorkspaceStore);
  protected readonly assetStore = inject(AssetStore);
  protected readonly promptHistoryStore = inject(PromptHistoryStore);

  private readonly auth = inject(CurrentUserStore);
  private readonly brandStore = inject(BrandStore);
  private readonly productStore = inject(ProductServiceStore);
  private readonly projectStore = inject(ProjectStore);
  private readonly route = inject(ActivatedRoute);

  private readonly drawerOpenSignal = signal(false);
  private readonly selectedAssetIdsSignal = signal<readonly string[]>([]);
  private readonly fieldErrorsSignal = signal<Readonly<Record<string, string>>>({});
  private readonly selectedVersionSignal = signal<GeneratedVersion | null>(null);
  private readonly versionDetailOpenSignal = signal(false);
  private readonly shareDialogOpenSignal = signal(false);
  private readonly shareLinkSignal = signal<string | null>(null);
  private readonly shareExpiresAtSignal = signal<string | null>(null);
  private readonly shareLoadingSignal = signal(false);
  private readonly downloadingVersionIdSignal = signal<string | null>(null);
  private readonly initializingSignal = signal(true);

  protected readonly drawerOpen = this.drawerOpenSignal.asReadonly();
  protected readonly selectedAssetIds = this.selectedAssetIdsSignal.asReadonly();
  protected readonly fieldErrors = this.fieldErrorsSignal.asReadonly();
  protected readonly selectedVersion = this.selectedVersionSignal.asReadonly();
  protected readonly versionDetailOpen = this.versionDetailOpenSignal.asReadonly();
  protected readonly shareDialogOpen = this.shareDialogOpenSignal.asReadonly();
  protected readonly shareLink = this.shareLinkSignal.asReadonly();
  protected readonly shareExpiresAt = this.shareExpiresAtSignal.asReadonly();
  protected readonly shareLoading = this.shareLoadingSignal.asReadonly();
  protected readonly downloadingVersionId = this.downloadingVersionIdSignal.asReadonly();
  protected readonly initializing = this.initializingSignal.asReadonly();

  protected readonly requestFormats = REQUEST_FORMATS;
  protected readonly creativeRequestStatusLabel = creativeRequestStatusLabel;
  protected readonly creativeRequestStatusTone = creativeRequestStatusTone;
  protected readonly generatedVersionStatusLabel = generatedVersionStatusLabel;
  protected readonly generatedVersionStatusTone = generatedVersionStatusTone;

  protected readonly form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    brief: new FormControl('', { nonNullable: true }),
    promptId: new FormControl('', { nonNullable: true }),
    targetPlatform: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    requestedFormat: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    requestedVersions: new FormControl(1, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1)],
    }),
  });

  protected readonly projectId = computed(() => this.route.snapshot.paramMap.get('projectId') ?? '');
  protected readonly project = computed(
    () => this.projectStore.items().find((item) => item.id === this.projectId()) ?? null,
  );
  protected readonly product = computed(() => {
    const project = this.project();
    return this.productStore.items().find((item) => item.id === project?.productServiceId) ?? null;
  });
  protected readonly brand = computed(() => {
    const product = this.product();
    return this.brandStore.items().find((item) => item.id === product?.brandId) ?? null;
  });
  protected readonly hasWorkspaceContext = computed(() => Boolean(this.auth.activeWorkspaceId()));
  protected readonly requestCountLabel = computed(
    () => `${this.requestStore.requests().length} request${this.requestStore.requests().length === 1 ? '' : 's'}`,
  );
  protected readonly versionLimit = computed(() => {
    const limit =
      this.workspace.featureLimit('creative.request.versions') ??
      this.workspace.featureLimit('generatedVersionsPerRequest') ??
      this.workspace.featureLimit('generated_versions_per_request');
    return typeof limit?.limit === 'number' ? limit.limit : null;
  });
  protected readonly versionLimitMessage = computed(() => {
    const limit = this.versionLimit();
    const requested = this.form.controls.requestedVersions.value;
    return limit !== null && requested > limit
      ? `Your current package allows only ${limit} creative version(s) for this request.`
      : null;
  });
  protected readonly estimatedCreditsLabel = computed(() => {
    const credits = this.workspace.featureLimit('creative.request.estimatedCredits');
    return typeof credits?.limit === 'number' ? `${credits.limit} credits` : 'Provided after request review';
  });
  protected readonly shareAvailable = computed(() => {
    const policy = this.workspace.featurePolicy();

    if (policy?.shareAvailable === false) {
      return false;
    }

    return this.workspace.isFeatureEnabled('sharing') && this.workspace.isFeatureEnabled('generatedVersions.share');
  });
  protected readonly shareUnavailableMessage = computed(() => {
    const policy = this.workspace.featurePolicy();

    if (policy?.shareAvailable === false) {
      return 'Sharing is not available in your current package.';
    }

    const subscriptionStatus = this.workspace.subscription()?.status?.toLowerCase();
    if (subscriptionStatus && !['active', 'trialing'].includes(subscriptionStatus)) {
      return 'Sharing is paused because your workspace subscription is not active.';
    }

    return null;
  });

  constructor() {
    void this.initialize();
  }

  protected async initialize(): Promise<void> {
    this.initializingSignal.set(true);
    const workspaceId = this.auth.activeWorkspaceId();
    const projectId = this.projectId();
    if (!workspaceId || !projectId) {
      this.initializingSignal.set(false);
      return;
    }

    await Promise.all([
      this.brandStore.load(workspaceId),
      this.productStore.load(workspaceId),
      this.projectStore.load(workspaceId),
      this.assetStore.loadProjectAssets(projectId),
      this.promptHistoryStore.loadHistory(projectId),
      this.requestStore.loadByProject(projectId),
    ]);

    this.form.patchValue(
      {
        targetPlatform: this.project()?.targetPlatform ?? '',
        requestedFormat: REQUEST_FORMATS[0],
      },
      { emitEvent: false },
    );
    this.initializingSignal.set(false);
  }

  protected openDrawer(): void {
    this.fieldErrorsSignal.set({});
    this.drawerOpenSignal.set(true);
  }

  protected closeDrawer(): void {
    this.drawerOpenSignal.set(false);
  }

  protected toggleAsset(assetId: string): void {
    this.selectedAssetIdsSignal.update((ids) =>
      ids.includes(assetId) ? ids.filter((id) => id !== assetId) : [...ids, assetId],
    );
  }

  protected isAssetSelected(assetId: string): boolean {
    return this.selectedAssetIdsSignal().includes(assetId);
  }

  protected async submitRequest(): Promise<void> {
    this.fieldErrorsSignal.set({});
    if (this.form.invalid || this.versionLimitMessage()) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const result = await this.requestStore.create(this.projectId(), {
      title: value.title.trim(),
      brief: value.brief.trim() || null,
      promptId: value.promptId || null,
      assetIds: this.selectedAssetIdsSignal(),
      targetPlatform: value.targetPlatform,
      requestedFormat: value.requestedFormat,
      requestedVersions: value.requestedVersions,
    });

    if (result.ok) {
      this.closeDrawer();
      const request = this.requestStore.selectedRequest();
      if (request) {
        await this.loadVersions(request);
      }
    } else {
      this.fieldErrorsSignal.set(result.fieldErrors);
    }
  }

  protected async loadVersions(request: CreativeRequest): Promise<void> {
    this.requestStore.selectRequest(request);
    await this.versionStore.loadByCreativeRequest(request.id);
  }

  protected openVersionDetail(version: GeneratedVersion): void {
    this.selectedVersionSignal.set(version);
    this.versionDetailOpenSignal.set(true);
  }

  protected closeVersionDetail(): void {
    this.selectedVersionSignal.set(null);
    this.versionDetailOpenSignal.set(false);
  }

  protected async downloadVersion(version: GeneratedVersion): Promise<void> {
    if (version.capabilities?.canDownload !== true || this.downloadingVersionIdSignal()) {
      return;
    }

    this.downloadingVersionIdSignal.set(version.id);
    const link = await this.versionStore.getDownloadUrl(version.id);
    this.downloadingVersionIdSignal.set(null);

    if (link?.url) {
      window.open(link.url, '_blank', 'noopener');
    }
  }

  protected async openShareDialog(version: GeneratedVersion): Promise<void> {
    this.shareDialogOpenSignal.set(true);
    this.shareLinkSignal.set(null);
    this.shareExpiresAtSignal.set(null);

    if (version.capabilities?.canShare !== true || !this.shareAvailable()) {
      return;
    }

    this.shareLoadingSignal.set(true);
    const link = await this.versionStore.getShareUrl(version.id);
    this.shareLoadingSignal.set(false);
    this.shareLinkSignal.set(link?.url ?? null);
    this.shareExpiresAtSignal.set(link?.expiresAt ?? null);
  }

  protected closeShareDialog(): void {
    this.shareDialogOpenSignal.set(false);
    this.shareLoadingSignal.set(false);
    this.shareLinkSignal.set(null);
    this.shareExpiresAtSignal.set(null);
  }

  protected copyShareLink(): void {
    const link = this.shareLinkSignal();
    if (link && navigator.clipboard) {
      void navigator.clipboard.writeText(link);
    }
  }

  protected reloadRequests(): void {
    void this.requestStore.loadByProject(this.projectId());
  }
}
