import { inject, Injectable } from '@angular/core';
import { firstValueFrom } from 'rxjs';

import { ApiService } from '@app/core/api/api.service';
import { ApiResponse } from '@app/shared/models/api-response.model';
import { Brand, CreateBrandPayload, UpdateBrandPayload } from './brand.models';

@Injectable({ providedIn: 'root' })
export class BrandService {
  private readonly api = inject(ApiService);

  async list(workspaceId: string): Promise<readonly Brand[]> {
    const response = await firstValueFrom(
      this.api.get<Brand[]>(`/api/v1/workspaces/${workspaceId}/brands`),
    );

    return this.unwrap(response);
  }

  async get(workspaceId: string, brandId: string): Promise<Brand> {
    const response = await firstValueFrom(
      this.api.get<Brand>(`/api/v1/workspaces/${workspaceId}/brands/${brandId}`),
    );

    return this.unwrap(response);
  }

  async create(workspaceId: string, payload: CreateBrandPayload): Promise<Brand> {
    const response = await firstValueFrom(
      this.api.post<Brand, CreateBrandPayload>(`/api/v1/workspaces/${workspaceId}/brands`, payload),
    );

    return this.unwrap(response);
  }

  async update(workspaceId: string, brandId: string, payload: UpdateBrandPayload): Promise<Brand> {
    const response = await firstValueFrom(
      this.api.put<Brand, UpdateBrandPayload>(
        `/api/v1/workspaces/${workspaceId}/brands/${brandId}`,
        payload,
      ),
    );

    return this.unwrap(response);
  }

  async remove(workspaceId: string, brandId: string): Promise<void> {
    await firstValueFrom(this.api.delete<void>(`/api/v1/workspaces/${workspaceId}/brands/${brandId}`));
  }

  private unwrap<T>(response: ApiResponse<T>): T {
    return response.data;
  }
}
