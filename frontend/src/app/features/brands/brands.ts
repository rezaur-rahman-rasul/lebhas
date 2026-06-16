import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { Asset, DEFAULT_ASSET_FILTERS } from '@app/features/admin/assets/models/asset.models';
import { AssetService } from '@app/features/admin/assets/services/asset.service';
import { AssetStore } from '@app/features/admin/assets/state/asset.store';
import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { InputComponent } from '@app/shared/components/input/input';
import { ModalComponent } from '@app/shared/components/modal/modal';
import { SectionHeaderComponent } from '@app/shared/components/section-header/section-header';
import {
  Brand,
  BrandLanguagePreference,
  BrandStatus,
  CreateBrandPayload,
  UpdateBrandPayload,
} from './brand.models';
import { BrandStore } from './brand.store';

type BrandDialogMode = 'create' | 'edit' | null;
const LANGUAGE_PREFERENCES: readonly {
  readonly value: BrandLanguagePreference;
  readonly label: string;
}[] = [
  { value: 'BOTH', label: 'Bangla and English' },
  { value: 'BANGLA', label: 'Bangla only' },
  { value: 'ENGLISH', label: 'English only' },
];

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
  private readonly assetStore = inject(AssetStore);
  private readonly assetService = inject(AssetService);
  protected readonly store = inject(BrandStore);

  protected readonly dialogMode = signal<BrandDialogMode>(null);
  protected readonly attemptedSubmit = signal(false);
  protected readonly selectedLogoFile = signal<File | null>(null);
  protected readonly currentLogoAsset = signal<Asset | null>(null);
  protected readonly logoError = signal('');
  protected readonly skeletonRows = [0, 1, 2, 3] as const;

  protected readonly canView = this.permissions.canViewBrands;
  protected readonly canManage = this.permissions.canManageBrands;
  protected readonly workspaceId = this.workspace.activeWorkspaceId;
  protected readonly brands = this.store.items;
  protected readonly selectedBrandId = this.store.selectedBrandId;
  protected readonly selectedBrand = this.store.selectedBrand;
  protected readonly languagePreferences = LANGUAGE_PREFERENCES;

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
    languagePreference: ['BOTH' as BrandLanguagePreference, [Validators.required]],
    status: ['ACTIVE' as BrandStatus],
  });

  constructor() {
    effect(() => {
      const workspaceId = this.workspaceId();
      if (workspaceId && this.canView()) {
        void this.store.load(workspaceId);
        return;
      }
      this.store.reset();
    });
  }

  protected selectBrand(brandId: string): void {
    this.store.selectBrand(brandId);
  }

  protected openCreateDialog(): void {
    this.attemptedSubmit.set(false);
    this.selectedLogoFile.set(null);
    this.currentLogoAsset.set(null);
    this.logoError.set('');
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
      languagePreference: 'BOTH',
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
    this.selectedLogoFile.set(null);
    this.currentLogoAsset.set(null);
    this.logoError.set('');
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
      languagePreference: brand.languagePreference ?? 'BOTH',
      status: brand.status,
    });
    this.dialogMode.set('edit');
    void this.loadCurrentLogoAsset(brand.id);
  }

  protected closeDialog(): void {
    this.selectedLogoFile.set(null);
    this.currentLogoAsset.set(null);
    this.logoError.set('');
    this.dialogMode.set(null);
  }

  protected onLogoSelected(event: Event): void {
    const input = event.target as HTMLInputElement | null;
    const file = input?.files?.item(0) ?? null;

    this.logoError.set('');
    if (!file) {
      this.selectedLogoFile.set(null);
      return;
    }

    const allowedTypes = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/svg+xml', 'image/webp']);
    const extension = file.name.split('.').pop()?.toLowerCase() ?? '';
    const allowedExtensions = new Set(['jpg', 'jpeg', 'png', 'svg', 'webp']);
    if (!allowedTypes.has(file.type) && !allowedExtensions.has(extension)) {
      this.selectedLogoFile.set(null);
      this.logoError.set('Upload a JPG, PNG, SVG, or WebP logo.');
      if (input) {
        input.value = '';
      }
      return;
    }

    if (file.size > 5 * 1024 * 1024) {
      this.selectedLogoFile.set(null);
      this.logoError.set('Logo file must be 5 MB or smaller.');
      if (input) {
        input.value = '';
      }
      return;
    }

    this.selectedLogoFile.set(file);
  }

  protected removeLogo(): void {
    this.selectedLogoFile.set(null);
    this.logoError.set('');
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

    try {
      let savedBrand: Brand;
      if (this.dialogMode() === 'create') {
        savedBrand = await this.store.create(workspaceId, payload);
      } else {
        const brand = this.selectedBrand();
        if (!brand) {
          return;
        }

        savedBrand = await this.store.update(workspaceId, brand.id, {
          ...payload,
          status: this.form.getRawValue().status,
        });
        this.store.selectBrand(savedBrand.id);
      }

      const logoUploaded = await this.uploadSelectedLogo(savedBrand.id);
      if (!logoUploaded) {
        return;
      }
    } catch {
      return;
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

  protected fieldError(fieldName: 'name' | 'languagePreference'): string {
    const control = this.form.controls[fieldName];

    if (!this.attemptedSubmit() && !control.touched) {
      return '';
    }

    if (control.hasError('required')) {
      return fieldName === 'name' ? 'Enter a brand name.' : 'Choose a creative language preference.';
    }

    return '';
  }

  protected colorPreview(color: string | null): string {
    return color && color.trim() ? color : 'transparent';
  }

  protected languagePreferenceLabel(value: BrandLanguagePreference | null | undefined): string {
    return LANGUAGE_PREFERENCES.find((option) => option.value === value)?.label ?? 'Bangla and English';
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
      languagePreference: value.languagePreference,
    };
  }

  private normalize(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  private async uploadSelectedLogo(brandId: string): Promise<boolean> {
    const file = this.selectedLogoFile();
    if (!file) {
      return true;
    }

    const result = await this.assetStore.uploadAsset({
      file,
      assetCategory: 'BRAND_LOGO',
      folderId: null,
      tags: ['brand-logo'],
      metadata: { brandId },
    });

    if (!result.ok) {
      this.logoError.set(result.message ?? 'Brand saved, but logo upload failed.');
      return false;
    }

    this.currentLogoAsset.set(this.assetStore.selectedAsset());
    return true;
  }

  private async loadCurrentLogoAsset(brandId: string): Promise<void> {
    const workspaceId = this.workspaceId();
    if (!workspaceId) {
      this.currentLogoAsset.set(null);
      return;
    }

    try {
      const page = await firstValueFrom(
        this.assetService.listAssets(
          workspaceId,
          {
            ...DEFAULT_ASSET_FILTERS,
            assetCategory: 'BRAND_LOGO',
            status: null,
            search: '',
          },
          0,
          50,
        ),
      );
      this.currentLogoAsset.set(
        page.items.find((asset) =>
          String(asset.metadata?.['brandId'] ?? '') === brandId &&
          (asset.status === 'READY' || asset.status === 'AVAILABLE')
        ) ?? null,
      );
    } catch {
      this.currentLogoAsset.set(null);
    }
  }
}
