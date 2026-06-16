import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import { CreditLedgerItemView, MasterCreditOverviewView, ProviderCreditAdjustmentRequest, ProviderCreditPoolView, WorkspaceCreditAccountView, WorkspaceCreditAdjustmentRequest } from '../models/provider-credit-exchange.models';

@Injectable({ providedIn: 'root' })
export class ProviderCreditApiService {
  private readonly api = inject(ApiService);

  async getProviderCreditPool(providerId: string): Promise<ProviderCreditPoolView> {
    return this.get<ProviderCreditPoolView>(`/api/v1/master/providers/${encodeURIComponent(providerId)}/credit-pool`);
  }

  async createProviderCreditPool(providerId: string, payload: Partial<ProviderCreditPoolView>): Promise<ProviderCreditPoolView> {
    return this.post<ProviderCreditPoolView, Partial<ProviderCreditPoolView>>(`/api/v1/master/providers/${encodeURIComponent(providerId)}/credit-pool`, payload);
  }

  async updateProviderCreditPool(providerId: string, payload: Partial<ProviderCreditPoolView>): Promise<ProviderCreditPoolView> {
    return this.put<ProviderCreditPoolView, Partial<ProviderCreditPoolView>>(`/api/v1/master/providers/${encodeURIComponent(providerId)}/credit-pool`, payload);
  }

  async adjustProviderCreditPool(providerId: string, payload: ProviderCreditAdjustmentRequest): Promise<ProviderCreditPoolView> {
    return this.post<ProviderCreditPoolView, ProviderCreditAdjustmentRequest>(`/api/v1/master/providers/${encodeURIComponent(providerId)}/credit-pool/adjust`, payload);
  }

  async getProviderCreditLedger(providerId: string): Promise<readonly CreditLedgerItemView[]> {
    return this.get<readonly CreditLedgerItemView[]>(`/api/v1/master/providers/${encodeURIComponent(providerId)}/credit-ledger`);
  }

  async getMasterCreditOverview(): Promise<MasterCreditOverviewView> {
    return this.get<MasterCreditOverviewView>('/api/v1/master/credits/overview');
  }

  async adjustWorkspaceCredits(workspaceId: string, payload: WorkspaceCreditAdjustmentRequest): Promise<unknown> {
    return this.post<unknown, WorkspaceCreditAdjustmentRequest>(`/api/v1/master/workspaces/${encodeURIComponent(workspaceId)}/credits/adjust`, payload);
  }

  async getMasterWorkspaceCredits(workspaceId: string): Promise<WorkspaceCreditAccountView> {
    const data = await this.get<WorkspaceCreditAccountView | { readonly creditAccount?: WorkspaceCreditAccountView }>(`/api/v1/master/workspaces/${encodeURIComponent(workspaceId)}/credits`);
    return 'creditAccount' in data && data.creditAccount ? data.creditAccount : data as WorkspaceCreditAccountView;
  }

  private async get<T>(path: string): Promise<T> {
    return unwrapApiResponse(await firstValueFrom(this.api.get<T>(path)));
  }

  private async post<T, B>(path: string, body: B): Promise<T> {
    return unwrapApiResponse(await firstValueFrom(this.api.post<T, B>(path, body)));
  }

  private async put<T, B>(path: string, body: B): Promise<T> {
    return unwrapApiResponse(await firstValueFrom(this.api.put<T, B>(path, body)));
  }
}
