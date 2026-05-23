import { Injectable, computed, inject, signal } from '@angular/core';

import {
  CreateProductServicePayload,
  ProductServiceRecord,
  UpdateProductServicePayload,
} from './product-service.models';
import { ProductServiceCatalogService } from './product-service.service';

@Injectable({ providedIn: 'root' })
export class ProductServiceStore {
  private readonly service = inject(ProductServiceCatalogService);

  private readonly itemsSignal = signal<readonly ProductServiceRecord[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly savingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly loadedWorkspaceIdSignal = signal<string | null>(null);

  readonly items = this.itemsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly saving = this.savingSignal.asReadonly();
  readonly error = this.errorSignal.asReadonly();
  readonly total = computed(() => this.itemsSignal().length);

  async load(workspaceId: string, options?: { readonly force?: boolean }): Promise<void> {
    if (
      !options?.force &&
      this.loadedWorkspaceIdSignal() === workspaceId &&
      (this.itemsSignal().length > 0 || this.errorSignal() === null)
    ) {
      return;
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const products = await this.service.list(workspaceId);
      this.itemsSignal.set(products);
      this.loadedWorkspaceIdSignal.set(workspaceId);
    } catch {
      this.itemsSignal.set([]);
      this.errorSignal.set('Product services could not be loaded.');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async create(
    workspaceId: string,
    brandId: string,
    payload: CreateProductServicePayload,
  ): Promise<ProductServiceRecord> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const productService = await this.service.create(workspaceId, brandId, payload);
      this.itemsSignal.update((items) => [productService, ...items]);
      this.loadedWorkspaceIdSignal.set(workspaceId);
      return productService;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async update(
    workspaceId: string,
    productServiceId: string,
    payload: UpdateProductServicePayload,
  ): Promise<ProductServiceRecord> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const productService = await this.service.update(workspaceId, productServiceId, payload);
      this.itemsSignal.update((items) =>
        items.map((item) => (item.id === productService.id ? productService : item)),
      );
      return productService;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async remove(workspaceId: string, productServiceId: string): Promise<void> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await this.service.remove(workspaceId, productServiceId);
      this.itemsSignal.update((items) => items.filter((item) => item.id !== productServiceId));
    } finally {
      this.savingSignal.set(false);
    }
  }
}
