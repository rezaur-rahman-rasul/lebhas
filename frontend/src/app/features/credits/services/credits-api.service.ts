import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import { CreditLedgerFilters, CreditLedgerItemView, GenerationCreditPreviewView, WorkspaceCreditAccountView } from '../models/credits.models';

@Injectable({ providedIn: 'root' })
export class CreditsApiService {
  private readonly api = inject(ApiService);

  async getMasterCreditOverview() {
    const response = await firstValueFrom(this.api.get('/api/v1/master/credits/overview'));
    return unwrapApiResponse(response);
  }

  async getWorkspaceCredits(workspaceId: string): Promise<WorkspaceCreditAccountView> {
    const response = await firstValueFrom(this.api.get<WorkspaceCreditAccountView>(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/credits`));
    return unwrapApiResponse(response);
  }

  async getWorkspaceCreditLedger(workspaceId: string, filters?: CreditLedgerFilters): Promise<readonly CreditLedgerItemView[]> {
    const response = await firstValueFrom(this.api.get<readonly CreditLedgerItemView[]>(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/credit-ledger`, clean(filters)));
    return unwrapApiResponse(response);
  }

  async getMasterWorkspaceCredits(workspaceId: string): Promise<WorkspaceCreditAccountView> {
    const response = await firstValueFrom(this.api.get<WorkspaceCreditAccountView>(`/api/v1/master/workspaces/${encodeURIComponent(workspaceId)}/credits`));
    return unwrapApiResponse(response);
  }

  async previewGeneration(workspaceId: string, creativeRequestId: string, payload: unknown): Promise<GenerationCreditPreviewView> {
    const response = await firstValueFrom(this.api.post<GenerationCreditPreviewView, unknown>(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/creative-requests/${encodeURIComponent(creativeRequestId)}/generation/preview`, payload));
    return unwrapApiResponse(response);
  }
}

function clean(filters?: CreditLedgerFilters): Record<string, string> {
  const params: Record<string, string> = {};
  for (const [key, value] of Object.entries(filters ?? {})) {
    if (value) {
      params[key] = value;
    }
  }
  return params;
}
