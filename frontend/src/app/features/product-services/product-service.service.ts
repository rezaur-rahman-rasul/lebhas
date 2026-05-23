import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiResponse } from '@app/shared/models/api-response.model';
import {
  CreateProductServicePayload,
  ProductServiceRecord,
  UpdateProductServicePayload,
} from './product-service.models';

@Injectable({ providedIn: 'root' })
export class ProductServiceCatalogService {
  private readonly api = inject(ApiService);

  async list(workspaceId: string): Promise<readonly ProductServiceRecord[]> {
    const response = await firstValueFrom(
      this.api.get<ProductServiceRecord[]>(`/api/v1/workspaces/${workspaceId}/product-services`),
    );

    return this.unwrap(response);
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

    return this.unwrap(response);
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

    return this.unwrap(response);
  }

  async remove(workspaceId: string, productServiceId: string): Promise<void> {
    await firstValueFrom(
      this.api.delete<void>(`/api/v1/workspaces/${workspaceId}/product-services/${productServiceId}`),
    );
  }

  private unwrap<T>(response: ApiResponse<T>): T {
    return response.data;
  }
}
