import { Injectable, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  GeneratedVersion,
  GeneratedVersionActionResult,
  GeneratedVersionLink,
} from './generated-version.models';
import { GeneratedVersionApiService } from './generated-version-api.service';

@Injectable({ providedIn: 'root' })
export class GeneratedVersionStore {
  private readonly auth = inject(CurrentUserStore);
  private readonly notifications = inject(NotificationStateService);
  private readonly api = inject(GeneratedVersionApiService);

  private readonly versionsSignal = signal<readonly GeneratedVersion[]>([]);
  private readonly selectedVersionSignal = signal<GeneratedVersion | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly versions = this.versionsSignal.asReadonly();
  readonly selectedVersion = this.selectedVersionSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  selectVersion(version: GeneratedVersion | null): void {
    this.selectedVersionSignal.set(version);
  }

  clearError(): void {
    this.errorSignal.set(null);
  }

  async loadByCreativeRequest(creativeRequestId: string): Promise<GeneratedVersionActionResult> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return { ok: false, fieldErrors: { workspaceId: 'Select a workspace before loading versions.' } };
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const versions = await this.api.listByCreativeRequest(workspaceId, creativeRequestId);
      this.versionsSignal.set(versions);
      const selected = this.selectedVersionSignal();
      if (selected) {
        this.selectedVersionSignal.set(versions.find((version) => version.id === selected.id) ?? null);
      }
      return { ok: true, fieldErrors: {} };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.errorSignal.set(normalized.message);
      this.notifications.error('Generated versions', normalized.message);
      return { ok: false, message: normalized.message, fieldErrors: {} };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async loadOne(generatedVersionId: string): Promise<GeneratedVersionActionResult> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return { ok: false, fieldErrors: { workspaceId: 'Select a workspace before loading this version.' } };
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const version = await this.api.get(workspaceId, generatedVersionId);
      this.selectedVersionSignal.set(version);
      this.versionsSignal.update((versions) => upsertVersion(versions, version));
      return { ok: true, fieldErrors: {} };
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.errorSignal.set(normalized.message);
      this.notifications.error('Generated version', normalized.message);
      return { ok: false, message: normalized.message, fieldErrors: {} };
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async getDownloadUrl(generatedVersionId: string): Promise<GeneratedVersionLink | null> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return null;
    }

    try {
      return await this.api.getDownloadUrl(workspaceId, generatedVersionId);
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.notifications.error('Download unavailable', normalized.message || 'This download link is no longer available.');
      return null;
    }
  }

  async getShareUrl(generatedVersionId: string): Promise<GeneratedVersionLink | null> {
    const workspaceId = this.auth.activeWorkspaceId();
    if (!workspaceId) {
      return null;
    }

    try {
      return await this.api.getShareUrl(workspaceId, generatedVersionId);
    } catch (error) {
      const normalized = normalizeHttpError(error);
      this.notifications.error('Share link unavailable', normalized.message || 'This share link is no longer available.');
      return null;
    }
  }
}

function upsertVersion(
  versions: readonly GeneratedVersion[],
  version: GeneratedVersion,
): readonly GeneratedVersion[] {
  const index = versions.findIndex((item) => item.id === version.id);
  if (index === -1) {
    return [version, ...versions];
  }

  return versions.map((item, itemIndex) => (itemIndex === index ? version : item));
}
