import { Injectable, computed, inject, signal } from '@angular/core';

import { Brand, CreateBrandPayload, UpdateBrandPayload } from './brand.models';
import { BrandService } from './brand.service';

@Injectable({ providedIn: 'root' })
export class BrandStore {
  private readonly service = inject(BrandService);

  private readonly itemsSignal = signal<readonly Brand[]>([]);
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
      const brands = await this.service.list(workspaceId);
      this.itemsSignal.set(brands);
      this.loadedWorkspaceIdSignal.set(workspaceId);
    } catch {
      this.itemsSignal.set([]);
      this.errorSignal.set('Brands could not be loaded.');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  async create(workspaceId: string, payload: CreateBrandPayload): Promise<Brand> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const brand = await this.service.create(workspaceId, payload);
      this.itemsSignal.update((items) => [brand, ...items]);
      this.loadedWorkspaceIdSignal.set(workspaceId);
      return brand;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async update(workspaceId: string, brandId: string, payload: UpdateBrandPayload): Promise<Brand> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const brand = await this.service.update(workspaceId, brandId, payload);
      this.itemsSignal.update((items) => items.map((item) => (item.id === brand.id ? brand : item)));
      return brand;
    } finally {
      this.savingSignal.set(false);
    }
  }

  async remove(workspaceId: string, brandId: string): Promise<void> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      await this.service.remove(workspaceId, brandId);
      this.itemsSignal.update((items) => items.filter((item) => item.id !== brandId));
    } finally {
      this.savingSignal.set(false);
    }
  }
}
