import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { unwrapApiResponse } from '@app/shared/utils/api-response';
import {
  CreateProductServicePayload,
  ProductServiceRecord,
  UpdateProductServicePayload,
} from './product-service.models';

@Injectable({ providedIn: 'root' })
export class ProductServiceApiService {
  private readonly api = inject(ApiService);

  async list(workspaceId: string): Promise<readonly ProductServiceRecord[]> {
    const response = await firstValueFrom(
      this.api.get<ProductServiceRecord[]>(`/api/v1/workspaces/${workspaceId}/product-services`),
    );

    return unwrapApiResponse(response);
  }

  async create(
    workspaceId: string,
    brandId: string,
    payload: CreateProductServicePayload,
  ): Promise<ProductServiceRecord> {
    const response = await firstValueFrom(
      this.api.post<ProductServiceRecord, CreateProductServicePayload>(
        `/api/v1/workspaces/${workspaceId}/brands/${brandId}/product-services`,
        payload,
      ),
    );

    return unwrapApiResponse(response);
  }

  async update(
    workspaceId: string,
    productServiceId: string,
    payload: UpdateProductServicePayload,
  ): Promise<ProductServiceRecord> {
    const response = await firstValueFrom(
      this.api.put<ProductServiceRecord, UpdateProductServicePayload>(
        `/api/v1/workspaces/${workspaceId}/product-services/${productServiceId}`,
        payload,
      ),
    );

    return unwrapApiResponse(response);
  }

  async remove(workspaceId: string, productServiceId: string): Promise<void> {
    await firstValueFrom(
      this.api.delete<void>(`/api/v1/workspaces/${workspaceId}/product-services/${productServiceId}`),
    );
  }
}
