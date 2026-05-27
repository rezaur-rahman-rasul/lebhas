import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  CreateProductServicePayload,
  ProductServiceRecord,
  UpdateProductServicePayload,
} from './product-service.models';
import { ProductServiceApiService } from './product-service-api.service';

@Injectable({ providedIn: 'root' })
export class ProductServiceStore {
  private readonly service = inject(ProductServiceApiService);
  private readonly notifications = inject(NotificationStateService);

  private readonly itemsSignal = signal<readonly ProductServiceRecord[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly savingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly loadedWorkspaceIdSignal = signal<string | null>(null);
  private readonly selectedProductServiceIdSignal = signal<string | null>(null);

  readonly items = this.itemsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly saving = this.savingSignal.asReadonly();
  readonly creating = this.saving;
  readonly updating = this.saving;
  readonly deleting = this.saving;
  readonly error = this.errorSignal.asReadonly();
  readonly workspaceId = this.loadedWorkspaceIdSignal.asReadonly();
  readonly selectedProductServiceId = this.selectedProductServiceIdSignal.asReadonly();
  readonly total = computed(() => this.itemsSignal().length);
  readonly productServiceCount = this.total;
  readonly hasProductServices = computed(() => this.itemsSignal().length > 0);
  readonly selectedProductService = computed(
    () => this.itemsSignal().find((item) => item.id === this.selectedProductServiceIdSignal()) ?? null,
  );
  readonly catalogRosterSubtitle = computed(
    () => `${this.itemsSignal().length} linked products and services`,
  );

  async load(workspaceId: string, options?: { readonly force?: boolean }): Promise<void> {
    if (
      !options?.force &&
      this.loadedWorkspaceIdSignal() === workspaceId &&
      (this.itemsSignal().length > 0 || this.errorSignal() === null)
    ) {
      return;
    }

    if (this.loadedWorkspaceIdSignal() !== workspaceId) {
      this.itemsSignal.set([]);
      this.selectedProductServiceIdSignal.set(null);
      this.loadedWorkspaceIdSignal.set(workspaceId);
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const products = await this.service.list(workspaceId);
      this.itemsSignal.set(products);
      this.loadedWorkspaceIdSignal.set(workspaceId);
      this.selectFirstAvailableProductService();
    } catch (error) {
      this.itemsSignal.set([]);
      this.selectedProductServiceIdSignal.set(null);
      this.errorSignal.set(normalizeHttpError(error).message || 'Product services could not be loaded.');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  selectProductService(productServiceId: string | null): void {
    if (!productServiceId || this.itemsSignal().some((item) => item.id === productServiceId)) {
      this.selectedProductServiceIdSignal.set(productServiceId);
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
      this.selectedProductServiceIdSignal.set(productService.id);
      this.notifications.success('Product/service created', `${productService.name} is linked to its brand.`);
      return productService;
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message);
      throw error;
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
      this.selectedProductServiceIdSignal.set(productService.id);
      this.notifications.success('Product/service updated', `${productService.name} changes were saved.`);
      return productService;
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message);
      throw error;
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
      this.selectFirstAvailableProductService();
      this.notifications.success('Product/service deleted', 'The catalog item was removed.');
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message);
      throw error;
    } finally {
      this.savingSignal.set(false);
    }
  }

  reset(): void {
    this.itemsSignal.set([]);
    this.selectedProductServiceIdSignal.set(null);
    this.loadedWorkspaceIdSignal.set(null);
    this.errorSignal.set(null);
    this.loadingSignal.set(false);
  }

  private selectFirstAvailableProductService(): void {
    const productServices = this.itemsSignal();
    const currentSelection = this.selectedProductServiceIdSignal();

    if (productServices.length === 0) {
      this.selectedProductServiceIdSignal.set(null);
      return;
    }

    if (!currentSelection || !productServices.some((item) => item.id === currentSelection)) {
      this.selectedProductServiceIdSignal.set(productServices[0].id);
    }
  }
}
