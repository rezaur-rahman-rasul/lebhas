import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiEndpoints } from '@app/core/api/api-endpoints';
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
      this.api.get<ProductServiceRecord[]>(ApiEndpoints.productServices.list(workspaceId)),
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
        ApiEndpoints.brands.productServices(workspaceId, brandId),
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
        ApiEndpoints.productServices.detail(workspaceId, productServiceId),
        payload,
      ),
    );

    return unwrapApiResponse(response);
  }

  async remove(workspaceId: string, productServiceId: string): Promise<void> {
    await firstValueFrom(
      this.api.delete<void>(ApiEndpoints.productServices.detail(workspaceId, productServiceId)),
    );
  }
}
