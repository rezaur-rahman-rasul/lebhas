import { KeyValuePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import {
  GeneratedVersion,
  generatedVersionStatusLabel,
  generatedVersionStatusTone,
} from '../../generated-version.models';
import { GeneratedVersionStore } from '../../generated-version.store';

@Component({
  selector: 'app-generated-version-detail-page',
  standalone: true,
  imports: [
    RouterLink,
    KeyValuePipe,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    ModalShellComponent,
    PageHeaderComponent,
  ],
  templateUrl: './generated-version-detail.html',
  styleUrl: './generated-version-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GeneratedVersionDetailPage {
  protected readonly store = inject(GeneratedVersionStore);
  private readonly workspace = inject(WorkspaceStore);
  private readonly route = inject(ActivatedRoute);

  protected readonly shareDialogOpen = signal(false);
  protected readonly shareLink = signal<string | null>(null);
  protected readonly shareExpiresAt = signal<string | null>(null);
  protected readonly shareLoading = signal(false);
  protected readonly downloading = signal(false);

  protected readonly generatedVersionId = computed(
    () => this.route.snapshot.paramMap.get('generatedVersionId') ?? '',
  );
  protected readonly generatedVersionStatusLabel = generatedVersionStatusLabel;
  protected readonly generatedVersionStatusTone = generatedVersionStatusTone;
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
    void this.store.loadOne(this.generatedVersionId());
  }

  protected reload(): void {
    void this.store.loadOne(this.generatedVersionId());
  }

  protected canDownload(version: GeneratedVersion): boolean {
    return version.capabilities?.canDownload === true && !this.downloading();
  }

  protected canShare(version: GeneratedVersion): boolean {
    return version.capabilities?.canShare === true && this.shareAvailable();
  }

  protected async download(version: GeneratedVersion): Promise<void> {
    if (!this.canDownload(version)) {
      return;
    }

    this.downloading.set(true);
    const link = await this.store.getDownloadUrl(version.id);
    this.downloading.set(false);

    if (link?.url) {
      window.open(link.url, '_blank', 'noopener');
    }
  }

  protected async openShareDialog(version: GeneratedVersion): Promise<void> {
    this.shareDialogOpen.set(true);
    this.shareLink.set(null);
    this.shareExpiresAt.set(null);

    if (!this.canShare(version)) {
      return;
    }

    this.shareLoading.set(true);
    const link = await this.store.getShareUrl(version.id);
    this.shareLoading.set(false);
    this.shareLink.set(link?.url ?? null);
    this.shareExpiresAt.set(link?.expiresAt ?? null);
  }

  protected closeShareDialog(): void {
    this.shareDialogOpen.set(false);
    this.shareLoading.set(false);
    this.shareLink.set(null);
    this.shareExpiresAt.set(null);
  }

  protected copyShareLink(): void {
    const link = this.shareLink();

    if (link && navigator.clipboard) {
      void navigator.clipboard.writeText(link);
    }
  }
}
