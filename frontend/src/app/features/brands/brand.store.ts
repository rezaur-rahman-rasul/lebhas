import { Injectable, computed, inject, signal } from '@angular/core';

import { normalizeHttpError } from '@app/core/api/http-error';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { Brand, CreateBrandPayload, UpdateBrandPayload } from './brand.models';
import { BrandApiService } from './brand-api.service';

@Injectable({ providedIn: 'root' })
export class BrandStore {
  private readonly service = inject(BrandApiService);
  private readonly notifications = inject(NotificationStateService);

  private readonly itemsSignal = signal<readonly Brand[]>([]);
  private readonly loadingSignal = signal(false);
  private readonly savingSignal = signal(false);
  private readonly errorSignal = signal<string | null>(null);
  private readonly loadedWorkspaceIdSignal = signal<string | null>(null);
  private readonly selectedBrandIdSignal = signal<string | null>(null);

  readonly items = this.itemsSignal.asReadonly();
  readonly loading = this.loadingSignal.asReadonly();
  readonly saving = this.savingSignal.asReadonly();
  readonly creating = this.saving;
  readonly updating = this.saving;
  readonly deleting = this.saving;
  readonly error = this.errorSignal.asReadonly();
  readonly workspaceId = this.loadedWorkspaceIdSignal.asReadonly();
  readonly selectedBrandId = this.selectedBrandIdSignal.asReadonly();
  readonly total = computed(() => this.itemsSignal().length);
  readonly brandCount = this.total;
  readonly hasBrands = computed(() => this.itemsSignal().length > 0);
  readonly activeBrands = computed(() =>
    this.itemsSignal().filter((brand) => brand.status === 'ACTIVE'),
  );
  readonly selectedBrand = computed(
    () => this.itemsSignal().find((brand) => brand.id === this.selectedBrandIdSignal()) ?? null,
  );
  readonly brandDetailReady = computed(() => !this.loadingSignal() && Boolean(this.selectedBrand()));
  readonly brandRosterSubtitle = computed(
    () => `${this.itemsSignal().length} brands in the active workspace`,
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
      this.selectedBrandIdSignal.set(null);
      this.loadedWorkspaceIdSignal.set(workspaceId);
    }

    this.loadingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const brands = await this.service.list(workspaceId);
      this.itemsSignal.set(brands);
      this.loadedWorkspaceIdSignal.set(workspaceId);
      this.selectFirstAvailableBrand();
    } catch (error) {
      this.itemsSignal.set([]);
      this.selectedBrandIdSignal.set(null);
      this.errorSignal.set(normalizeHttpError(error).message || 'Brands could not be loaded.');
    } finally {
      this.loadingSignal.set(false);
    }
  }

  selectBrand(brandId: string | null): void {
    if (!brandId || this.itemsSignal().some((brand) => brand.id === brandId)) {
      this.selectedBrandIdSignal.set(brandId);
    }
  }

  async create(workspaceId: string, payload: CreateBrandPayload): Promise<Brand> {
    this.savingSignal.set(true);
    this.errorSignal.set(null);

    try {
      const brand = await this.service.create(workspaceId, payload);
      this.itemsSignal.update((items) => [brand, ...items]);
      this.loadedWorkspaceIdSignal.set(workspaceId);
      this.selectedBrandIdSignal.set(brand.id);
      this.notifications.success('Brand created', `${brand.name} is ready for products and campaigns.`);
      return brand;
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message);
      throw error;
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
      this.selectedBrandIdSignal.set(brand.id);
      this.notifications.success('Brand updated', `${brand.name} changes were saved.`);
      return brand;
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message);
      throw error;
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
      this.selectFirstAvailableBrand();
      this.notifications.success('Brand deleted', 'The brand was removed from this workspace.');
    } catch (error) {
      this.errorSignal.set(normalizeHttpError(error).message);
      throw error;
    } finally {
      this.savingSignal.set(false);
    }
  }

  reset(): void {
    this.itemsSignal.set([]);
    this.selectedBrandIdSignal.set(null);
    this.loadedWorkspaceIdSignal.set(null);
    this.errorSignal.set(null);
    this.loadingSignal.set(false);
  }

  private selectFirstAvailableBrand(): void {
    const brands = this.itemsSignal();
    const currentSelection = this.selectedBrandIdSignal();

    if (brands.length === 0) {
      this.selectedBrandIdSignal.set(null);
      return;
    }

    if (!currentSelection || !brands.some((brand) => brand.id === currentSelection)) {
      this.selectedBrandIdSignal.set(brands[0].id);
    }
  }
}
