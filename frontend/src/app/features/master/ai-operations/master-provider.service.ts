import { HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiEndpoints } from '@app/core/api/api-endpoints';
import { ApiService } from '@app/core/api/api.service';
import { SKIP_ERROR_TOAST } from '@app/core/auth/auth-request-context';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  CreateMasterProviderRequest,
  MasterProviderEnvironment,
  MasterProviderStatus,
  MasterProviderType,
  MasterProviderView,
  ProviderConnectionTestResult,
  ProviderCredentialSavedView,
  SaveProviderCredentialRequest,
  TestProviderConnectionRequest,
} from './master-provider.models';

@Injectable({ providedIn: 'root' })
export class MasterProviderService {
  private readonly api = inject(ApiService);
  private readonly quietContext = new HttpContext().set(SKIP_ERROR_TOAST, true);

  async listProviders(filters?: {
    readonly type?: MasterProviderType | '';
    readonly status?: MasterProviderStatus | '';
    readonly environment?: MasterProviderEnvironment | '';
  }): Promise<readonly MasterProviderView[]> {
    const response = await firstValueFrom(
      this.api.get<readonly MasterProviderView[]>(ApiEndpoints.master.providers, {
        params: this.cleanParams(filters),
        context: this.quietContext,
      }),
    );
    return unwrapApiResponse(response);
  }

  async createProvider(payload: CreateMasterProviderRequest): Promise<MasterProviderView> {
    const response = await firstValueFrom(
      this.api.post<MasterProviderView, CreateMasterProviderRequest>(ApiEndpoints.master.providers, payload, {
        context: this.quietContext,
      }),
    );
    return unwrapApiResponse(response);
  }

  async getProvider(providerId: string): Promise<MasterProviderView> {
    const response = await firstValueFrom(
      this.api.get<MasterProviderView>(ApiEndpoints.master.provider(providerId), {
        context: this.quietContext,
      }),
    );
    return unwrapApiResponse(response);
  }

  async saveCredential(
    providerId: string,
    payload: SaveProviderCredentialRequest,
  ): Promise<ProviderCredentialSavedView> {
    const response = await firstValueFrom(
      this.api.put<ProviderCredentialSavedView, SaveProviderCredentialRequest>(
        ApiEndpoints.master.providerCredentials(providerId),
        payload,
        { context: this.quietContext },
      ),
    );
    return unwrapApiResponse(response);
  }

  async testConnection(
    providerId: string,
    payload: TestProviderConnectionRequest,
  ): Promise<ProviderConnectionTestResult> {
    const response = await firstValueFrom(
      this.api.post<ProviderConnectionTestResult, TestProviderConnectionRequest>(
        ApiEndpoints.master.providerTestConnection(providerId),
        payload,
        { context: this.quietContext },
      ),
    );
    return unwrapApiResponse(response);
  }

  async revokeCredential(
    providerId: string,
    environment: MasterProviderEnvironment,
  ): Promise<ProviderCredentialSavedView> {
    const response = await firstValueFrom(
      this.api.delete<ProviderCredentialSavedView>(ApiEndpoints.master.providerCredentials(providerId), {
        params: { environment },
        context: this.quietContext,
      }),
    );
    return unwrapApiResponse(response);
  }

  async updateProviderStatus(
    providerId: string,
    status: MasterProviderStatus,
  ): Promise<MasterProviderView> {
    const response = await firstValueFrom(
      this.api.patch<MasterProviderView, { readonly status: MasterProviderStatus }>(
        ApiEndpoints.master.providerStatus(providerId),
        { status },
        { context: this.quietContext },
      ),
    );
    return unwrapApiResponse(response);
  }

  private cleanParams(
    params?: Record<string, string | number | boolean | null | undefined>,
  ): Record<string, string | number | boolean> {
    const clean: Record<string, string | number | boolean> = {};
    for (const [key, value] of Object.entries(params ?? {})) {
      if (value === null || value === undefined || value === '') {
        continue;
      }
      clean[key] = value;
    }
    return clean;
  }
}
