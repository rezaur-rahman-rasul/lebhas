import { Injectable, computed, inject, signal } from '@angular/core';
import { normalizeHttpError } from '@app/core/api/http-error';
import { CreditLedgerFilters, CreditLedgerItemView, GenerationCreditPreviewView, WorkspaceCreditAccountView } from '../models/credits.models';
import { CreditsApiService } from '../services/credits-api.service';

@Injectable({ providedIn: 'root' })
export class WorkspaceCreditsStore {
  private readonly api = inject(CreditsApiService);

  private readonly creditAccountSignal = signal<WorkspaceCreditAccountView | null>(null);
  private readonly creditLedgerSignal = signal<readonly CreditLedgerItemView[]>([]);
  private readonly generationPreviewSignal = signal<GenerationCreditPreviewView | null>(null);
  private readonly loadingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);

  readonly creditAccount = this.creditAccountSignal.asReadonly();
  readonly creditLedger = this.creditLedgerSignal.asReadonly();
  readonly generationPreview = this.generationPreviewSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();

  readonly availableCredits = computed(() => this.creditAccountSignal()?.availableCredits ?? this.generationPreviewSignal()?.availableCredits ?? null);
  readonly hasCredits = computed(() => (this.availableCredits() ?? 0) > 0);
  readonly isLowCredit = computed(() => (this.availableCredits() ?? 0) > 0 && (this.availableCredits() ?? 0) < 100);
  readonly freeCreditGrantedLabel = computed(() => this.creditAccountSignal()?.freeCreditsGranted ? 'Free credits granted once on registration' : 'Free signup credits unavailable');
  readonly canGenerateFromPreview = computed(() => this.generationPreviewSignal()?.canQueueGeneration === true);

  async loadAccount(workspaceId: string): Promise<void> {
    await this.run(async () => this.creditAccountSignal.set(await this.api.getWorkspaceCredits(workspaceId)));
  }

  async loadLedger(workspaceId: string, filters?: CreditLedgerFilters): Promise<void> {
    await this.run(async () => this.creditLedgerSignal.set(await this.api.getWorkspaceCreditLedger(workspaceId, filters)));
  }

  async previewGeneration(workspaceId: string, creativeRequestId: string, payload: unknown): Promise<GenerationCreditPreviewView | null> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      const preview = await this.api.previewGeneration(workspaceId, creativeRequestId, payload);
      this.generationPreviewSignal.set(preview);
      return preview;
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message || 'Credit preview could not be loaded.');
      return null;
    } finally {
      this.loadingSignal.set(false);
    }
  }

  setLocalGenerationPreview(preview: GenerationCreditPreviewView | null): void {
    this.generationPreviewSignal.set(preview);
  }

  async refreshAfterGeneration(workspaceId: string): Promise<void> {
    await this.loadAccount(workspaceId);
    await this.loadLedger(workspaceId);
  }

  private async run(action: () => Promise<void>): Promise<void> {
    this.loadingSignal.set(true);
    this.errorSignal.set(null);
    try {
      await action();
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message || 'Credit balance could not be loaded.');
    } finally {
      this.loadingSignal.set(false);
    }
  }
}
