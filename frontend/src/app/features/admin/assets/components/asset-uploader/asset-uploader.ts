import { ChangeDetectionStrategy, Component, computed, effect, inject, input, OnDestroy, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  AssetCategory,
  AssetFolder,
  ASSET_UPLOAD_CATEGORY_OPTIONS,
  normalizeAssetTag,
  UploadAssetPayload,
} from '../../models/asset.models';

interface UploadFormValue {
  readonly assetCategory: AssetCategory;
  readonly folderId: string;
  readonly tags: string;
}

interface AssetUploadValidationRule {
  readonly allowedExtensions: readonly string[];
  readonly allowedMimeTypes: readonly string[];
  readonly maxSizeBytes: number;
  readonly hint: string;
  readonly sizeErrorMessage: string;
}

const MEGABYTE = 1024 * 1024;
const BLOCKED_FILENAME_SEGMENTS = new Set([
  'exe',
  'bat',
  'cmd',
  'com',
  'msi',
  'ps1',
  'php',
  'jsp',
  'sh',
  'jar',
  'js',
  'html',
  'svgz',
  'hta',
  'scr',
]);
const IMAGE_RULE: AssetUploadValidationRule = {
  allowedExtensions: ['jpg', 'jpeg', 'png', 'webp'],
  allowedMimeTypes: ['image/jpeg', 'image/png', 'image/webp'],
  maxSizeBytes: 10 * MEGABYTE,
  hint: 'JPG, JPEG, PNG, or WebP, up to 10 MB',
  sizeErrorMessage: 'Image files must be 10 MB or smaller.',
};
const VIDEO_RULE: AssetUploadValidationRule = {
  allowedExtensions: ['mp4', 'mov'],
  allowedMimeTypes: ['video/mp4', 'video/quicktime'],
  maxSizeBytes: 200 * MEGABYTE,
  hint: 'MP4 or MOV, up to 200 MB',
  sizeErrorMessage: 'Video files must be 200 MB or smaller.',
};
const LOGO_RULE: AssetUploadValidationRule = {
  allowedExtensions: ['png', 'svg', 'webp'],
  allowedMimeTypes: ['image/png', 'image/svg+xml', 'image/webp'],
  maxSizeBytes: 5 * MEGABYTE,
  hint: 'PNG, SVG, or WebP, up to 5 MB',
  sizeErrorMessage: 'Logo files must be 5 MB or smaller.',
};
const OTHER_RULE: AssetUploadValidationRule = {
  allowedExtensions: ['jpg', 'jpeg', 'png', 'webp', 'mp4', 'mov'],
  allowedMimeTypes: ['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'video/quicktime'],
  maxSizeBytes: 200 * MEGABYTE,
  hint: 'JPG, JPEG, PNG, WebP, MP4, or MOV, up to 200 MB',
  sizeErrorMessage: 'Other assets must be 200 MB or smaller.',
};
const VALIDATION_RULES: Record<AssetCategory, AssetUploadValidationRule> = {
  PRODUCT_IMAGE: IMAGE_RULE,
  RAW_IMAGE: IMAGE_RULE,
  GENERATED_IMAGE: IMAGE_RULE,
  THUMBNAIL: IMAGE_RULE,
  PRODUCT_VIDEO: VIDEO_RULE,
  RAW_VIDEO: VIDEO_RULE,
  GENERATED_VIDEO: VIDEO_RULE,
  BRAND_LOGO: LOGO_RULE,
  OTHER: OTHER_RULE,
};

@Component({
  selector: 'app-asset-uploader',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, IconComponent],
  templateUrl: './asset-uploader.html',
  styleUrl: './asset-uploader.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetUploader implements OnDestroy {
  readonly open = input(false);
  readonly folders = input<readonly AssetFolder[]>([]);
  readonly uploading = input(false);
  readonly uploadProgress = input<number | null>(null);
  readonly fieldErrors = input<Readonly<Record<string, string>>>({});

  readonly submitted = output<UploadAssetPayload>();
  readonly closed = output<void>();
  readonly uploadCancelled = output<void>();

  protected readonly categoryOptions = ASSET_UPLOAD_CATEGORY_OPTIONS;
  protected readonly uploadForm = new FormGroup({
    assetCategory: new FormControl<AssetCategory>('PRODUCT_IMAGE', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    folderId: new FormControl('', { nonNullable: true }),
    tags: new FormControl('', { nonNullable: true }),
  });

  private readonly selectedFileSignal = signal<File | null>(null);
  private readonly previewUrlSignal = signal<string | null>(null);
  private readonly validationMessageSignal = signal<string | null>(null);
  private readonly draggingSignal = signal(false);

  protected readonly selectedFile = this.selectedFileSignal.asReadonly();
  protected readonly previewUrl = this.previewUrlSignal.asReadonly();
  protected readonly validationMessage = this.validationMessageSignal.asReadonly();
  protected readonly dragging = this.draggingSignal.asReadonly();

  protected readonly selectedCategoryMeta = computed(() =>
    this.categoryOptions.find((option) => option.value === this.uploadForm.controls.assetCategory.value),
  );
  protected readonly fileHint = computed(() =>
    this.validationRule(this.uploadForm.controls.assetCategory.value).hint,
  );
  protected readonly acceptedFileTypes = computed(() =>
    this.validationRule(this.uploadForm.controls.assetCategory.value).allowedExtensions
      .map((extension) => `.${extension}`)
      .join(','),
  );
  protected readonly canSubmit = computed(
    () =>
      Boolean(this.selectedFileSignal()) &&
      !this.uploading() &&
      !this.validationMessageSignal() &&
      this.uploadForm.valid,
  );

  constructor() {
    effect(() => {
      if (!this.open()) {
        this.reset();
      }
    });

    effect(() => {
      const file = this.selectedFileSignal();
      if (!file) {
        return;
      }

      this.validateFile(file, this.uploadForm.controls.assetCategory.value);
    });
  }

  ngOnDestroy(): void {
    this.releasePreviewUrl();
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.draggingSignal.set(true);
  }

  protected onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.draggingSignal.set(false);
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.draggingSignal.set(false);
    const file = event.dataTransfer?.files?.item(0);
    if (file) {
      this.acceptFile(file);
    }
  }

  protected onFileChange(event: Event): void {
    const file = (event.target as HTMLInputElement | null)?.files?.item(0);
    if (file) {
      this.acceptFile(file);
    }
  }

  protected reset(): void {
    this.uploadForm.reset({
      assetCategory: 'PRODUCT_IMAGE',
      folderId: '',
      tags: '',
    });
    this.selectedFileSignal.set(null);
    this.validationMessageSignal.set(null);
    this.draggingSignal.set(false);
    this.releasePreviewUrl();
  }

  protected requestClose(): void {
    if (this.uploading()) {
      this.uploadCancelled.emit();
      return;
    }

    this.closed.emit();
  }

  protected submit(): void {
    const file = this.selectedFileSignal();
    if (!file) {
      this.validationMessageSignal.set('Choose a file before uploading.');
      return;
    }

    if (!this.validateFile(file, this.uploadForm.controls.assetCategory.value)) {
      return;
    }

    const tags = this.uploadForm.controls.tags.value
      .split(',')
      .map(normalizeAssetTag)
      .filter(Boolean);

    this.submitted.emit({
      file,
      assetCategory: this.uploadForm.controls.assetCategory.value,
      folderId: this.uploadForm.controls.folderId.value || null,
      tags,
      metadata: {},
    });
  }

  private acceptFile(file: File): void {
    this.selectedFileSignal.set(file);
    this.releasePreviewUrl();

    if (file.type.startsWith('image/')) {
      this.previewUrlSignal.set(URL.createObjectURL(file));
    } else if (file.type.startsWith('video/')) {
      this.previewUrlSignal.set(URL.createObjectURL(file));
    }
  }

  private validateFile(file: File, category: AssetCategory): boolean {
    const fileName = file.name.trim();
    const segments = fileName.toLowerCase().split('.').filter(Boolean);
    const extension = segments.length > 1 ? segments[segments.length - 1] : '';
    const rule = this.validationRule(category);

    if (!fileName || file.size <= 0) {
      this.validationMessageSignal.set('Choose a non-empty asset file.');
      return false;
    }

    if (segments.length < 2 || !extension) {
      this.validationMessageSignal.set('Include a supported file extension in the upload name.');
      return false;
    }

    if (segments.slice(0, -1).some((segment) => BLOCKED_FILENAME_SEGMENTS.has(segment))) {
      this.validationMessageSignal.set('Rename the file before uploading. This filename contains a blocked extension segment.');
      return false;
    }

    if (!rule.allowedExtensions.includes(extension)) {
      this.validationMessageSignal.set(
        `Unsupported file extension for ${this.selectedCategoryMeta()?.label.toLowerCase()}.`,
      );
      return false;
    }

    if (file.size > rule.maxSizeBytes) {
      this.validationMessageSignal.set(rule.sizeErrorMessage);
      return false;
    }

    if (
      file.type &&
      file.type !== 'application/octet-stream' &&
      !rule.allowedMimeTypes.includes(file.type)
    ) {
      this.validationMessageSignal.set(
        `Unsupported file type for ${this.selectedCategoryMeta()?.label.toLowerCase()}.`,
      );
      return false;
    }

    this.validationMessageSignal.set(null);
    return true;
  }

  private validationRule(category: AssetCategory): AssetUploadValidationRule {
    return VALIDATION_RULES[category];
  }

  private releasePreviewUrl(): void {
    const previewUrl = this.previewUrlSignal();
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }
    this.previewUrlSignal.set(null);
  }
}
