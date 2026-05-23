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
import { Brand, BrandStatus, CreateBrandPayload, UpdateBrandPayload } from './brand.models';
import { BrandStore } from './brand.store';

type BrandDialogMode = 'create' | 'edit' | null;

@Component({
  selector: 'app-brands',
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
  templateUrl: './brands.html',
  styleUrl: './brands.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BrandsComponent {
  private readonly formBuilder = inject(FormBuilder).nonNullable;
  private readonly workspace = inject(WorkspaceStore);
  private readonly permissions = inject(PermissionStore);
  protected readonly store = inject(BrandStore);

  protected readonly selectedBrandId = signal<string | null>(null);
  protected readonly dialogMode = signal<BrandDialogMode>(null);
  protected readonly attemptedSubmit = signal(false);

  protected readonly canView = this.permissions.canViewBrands;
  protected readonly canManage = this.permissions.canManageBrands;
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly brands = this.store.items;
  protected readonly selectedBrand = computed(
    () => this.brands().find((brand) => brand.id === this.selectedBrandId()) ?? null,
  );

  protected readonly form = this.formBuilder.group({
    name: ['', [Validators.required]],
    businessType: [''],
    industry: [''],
    targetAudience: [''],
    brandVoice: [''],
    preferredCta: [''],
    primaryColor: [''],
    secondaryColor: [''],
    website: [''],
    facebookUrl: [''],
    instagramUrl: [''],
    linkedinUrl: [''],
    tiktokUrl: [''],
    status: ['ACTIVE' as BrandStatus],
  });

  constructor() {
    effect(() => {
      const workspaceId = this.workspaceId();
      if (workspaceId && this.canView()) {
        void this.store.load(workspaceId);
      }
    });

    effect(() => {
      const brands = this.brands();
      const currentSelection = this.selectedBrandId();

      if (brands.length === 0) {
        this.selectedBrandId.set(null);
        return;
      }

      if (!currentSelection || !brands.some((brand) => brand.id === currentSelection)) {
        this.selectedBrandId.set(brands[0].id);
      }
    });
  }

  protected selectBrand(brandId: string): void {
    this.selectedBrandId.set(brandId);
  }

  protected openCreateDialog(): void {
    this.attemptedSubmit.set(false);
    this.form.reset({
      name: '',
      businessType: '',
      industry: '',
      targetAudience: '',
      brandVoice: '',
      preferredCta: '',
      primaryColor: '',
      secondaryColor: '',
      website: '',
      facebookUrl: '',
      instagramUrl: '',
      linkedinUrl: '',
      tiktokUrl: '',
      status: 'ACTIVE',
    });
    this.dialogMode.set('create');
  }

  protected openEditDialog(): void {
    const brand = this.selectedBrand();
    if (!brand) {
      return;
    }

    this.attemptedSubmit.set(false);
    this.form.reset({
      name: brand.name,
      businessType: brand.businessType ?? '',
      industry: brand.industry ?? '',
      targetAudience: brand.targetAudience ?? '',
      brandVoice: brand.brandVoice ?? '',
      preferredCta: brand.preferredCta ?? '',
      primaryColor: brand.primaryColor ?? '',
      secondaryColor: brand.secondaryColor ?? '',
      website: brand.website ?? '',
      facebookUrl: brand.facebookUrl ?? '',
      instagramUrl: brand.instagramUrl ?? '',
      linkedinUrl: brand.linkedinUrl ?? '',
      tiktokUrl: brand.tiktokUrl ?? '',
      status: brand.status,
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

    const payload = this.toPayload();

    if (this.dialogMode() === 'create') {
      const brand = await this.store.create(workspaceId, payload);
      this.selectedBrandId.set(brand.id);
    } else {
      const brand = this.selectedBrand();
      if (!brand) {
        return;
      }

      const updatedBrand = await this.store.update(workspaceId, brand.id, {
        ...payload,
        status: this.form.getRawValue().status,
      });
      this.selectedBrandId.set(updatedBrand.id);
    }

    this.closeDialog();
  }

  protected async deleteSelected(): Promise<void> {
    const workspaceId = this.workspaceId();
    const brand = this.selectedBrand();
    if (!workspaceId || !brand) {
      return;
    }

    const confirmed = globalThis.confirm(`Delete ${brand.name}?`);
    if (!confirmed) {
      return;
    }

    await this.store.remove(workspaceId, brand.id);
  }

  protected fieldError(fieldName: 'name'): string {
    const control = this.form.controls[fieldName];

    if (!this.attemptedSubmit() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return 'Enter a brand name.';
    }

    return '';
  }

  protected colorPreview(color: string | null): string {
    return color && color.trim() ? color : 'transparent';
  }

  private toPayload(): CreateBrandPayload {
    const value = this.form.getRawValue();

    return {
      name: value.name.trim(),
      businessType: this.normalize(value.businessType),
      industry: this.normalize(value.industry),
      targetAudience: this.normalize(value.targetAudience),
      brandVoice: this.normalize(value.brandVoice),
      preferredCta: this.normalize(value.preferredCta),
      primaryColor: this.normalize(value.primaryColor),
      secondaryColor: this.normalize(value.secondaryColor),
      website: this.normalize(value.website),
      facebookUrl: this.normalize(value.facebookUrl),
      instagramUrl: this.normalize(value.instagramUrl),
      linkedinUrl: this.normalize(value.linkedinUrl),
      tiktokUrl: this.normalize(value.tiktokUrl),
    };
  }

  private normalize(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }
}
