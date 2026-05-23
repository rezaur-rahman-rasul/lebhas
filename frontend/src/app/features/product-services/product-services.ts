import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { InputComponent } from '@app/shared/components/input/input';
import { LoadingComponent } from '@app/shared/components/loading/loading';
import { ModalComponent } from '@app/shared/components/modal/modal';
import { SectionHeaderComponent } from '@app/shared/components/section-header/section-header';
import { BrandStore } from '../brands/brand.store';
import {
  CreateProductServicePayload,
  ProductServiceRecord,
  ProductServiceStatus,
  UpdateProductServicePayload,
} from './product-service.models';
import { ProductServiceStore } from './product-service.store';

type ProductDialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-product-services',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    InputComponent,
    LoadingComponent,
    ModalComponent,
    SectionHeaderComponent,
  ],
  templateUrl: './product-services.html',
  styleUrl: './product-services.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductServicesComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;
  private readonly workspace = inject(WorkspaceStore);
  private readonly permissions = inject(PermissionStore);
  protected readonly brandStore = inject(BrandStore);
  protected readonly store = inject(ProductServiceStore);

  protected readonly selectedProductId = signal<string | null>(null);
  protected readonly dialogMode = signal<ProductDialogMode>(null);
  protected readonly attemptedSubmit = signal(false);

  protected readonly canView = this.permissions.canViewProducts;
  protected readonly canManage = this.permissions.canManageProducts;
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly brands = this.brandStore.items;
  protected readonly products = this.store.items;
  protected readonly selectedProduct = computed(
    () => this.products().find((product) => product.id === this.selectedProductId()) ?? null,
  );
  protected readonly selectedBrand = computed(() => {
    const product = this.selectedProduct();
    return this.brands().find((brand) => brand.id === product?.brandId) ?? null;
  });

  protected readonly form = this.formBuilder.group({
    brandId: ['', [Validators.required]],
    name: ['', [Validators.required]],
    description: [''],
    category: [''],
    targetAudience: [''],
    sellingPoints: [''],
    status: ['ACTIVE' as ProductServiceStatus],
  });

  constructor() {
    effect(() => {
      const workspaceId = this.workspaceId();
      if (!workspaceId) {
        return;
      }

      if (this.permissions.canViewBrands()) {
        void this.brandStore.load(workspaceId);
      }

      if (this.canView()) {
        void this.store.load(workspaceId);
      }
    });

    effect(() => {
      const products = this.products();
      const currentSelection = this.selectedProductId();

      if (products.length === 0) {
        this.selectedProductId.set(null);
        return;
      }

      if (!currentSelection || !products.some((product) => product.id === currentSelection)) {
        this.selectedProductId.set(products[0].id);
      }
    });
  }

  protected selectProduct(productId: string): void {
    this.selectedProductId.set(productId);
  }

  protected openCreateDialog(): void {
    this.attemptedSubmit.set(false);
    this.form.reset({
      brandId: '',
      name: '',
      description: '',
      category: '',
      targetAudience: '',
      sellingPoints: '',
      status: 'ACTIVE',
    });
    this.dialogMode.set('create');
  }

  protected openEditDialog(): void {
    const product = this.selectedProduct();
    if (!product) {
      return;
    }

    this.attemptedSubmit.set(false);
    this.form.reset({
      brandId: product.brandId,
      name: product.name,
      description: product.description ?? '',
      category: product.category ?? '',
      targetAudience: product.targetAudience ?? '',
      sellingPoints: product.sellingPoints ?? '',
      status: product.status,
    });
    this.dialogMode.set('edit');
  }

  protected closeDialog(): void {
    this.dialogMode.set(null);
  }

  protected async submit(): Promise<void> {
    const workspaceId = this.workspaceId();
    if (!workspaceId) {
      return;
    }

    this.attemptedSubmit.set(true);
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    const value = this.form.getRawValue();
    const payload = this.toPayload();

    if (this.dialogMode() === 'create') {
      const record = await this.store.create(workspaceId, value.brandId, payload);
      this.selectedProductId.set(record.id);
    } else {
      const product = this.selectedProduct();
      if (!product) {
        return;
      }

      const updatedProduct = await this.store.update(workspaceId, product.id, {
        ...payload,
        status: value.status,
      });
      this.selectedProductId.set(updatedProduct.id);
    }

    this.closeDialog();
  }

  protected async deleteSelected(): Promise<void> {
    const workspaceId = this.workspaceId();
    const product = this.selectedProduct();
    if (!workspaceId || !product) {
      return;
    }

    const confirmed = globalThis.confirm(`Delete ${product.name}?`);
    if (!confirmed) {
      return;
    }

    await this.store.remove(workspaceId, product.id);
  }

  protected brandName(brandId: string): string {
    return this.brands().find((brand) => brand.id === brandId)?.name ?? 'Unknown brand';
  }

  protected fieldError(fieldName: 'brandId' | 'name'): string {
    const control = this.form.controls[fieldName];

    if (!this.attemptedSubmit() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return fieldName === 'brandId' ? 'Select a brand.' : 'Enter a product or service name.';
    }

    return '';
  }

  private toPayload(): CreateProductServicePayload {
    const value = this.form.getRawValue();

    return {
      name: value.name.trim(),
      description: this.normalize(value.description),
      category: this.normalize(value.category),
      targetAudience: this.normalize(value.targetAudience),
      sellingPoints: this.normalize(value.sellingPoints),
    };
  }

  private normalize(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }
}
