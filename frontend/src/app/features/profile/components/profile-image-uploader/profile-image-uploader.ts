import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  computed,
  input,
  output,
  signal,
} from '@angular/core';

import { ButtonComponent } from '@app/shared/components/app-button/app-button';
import { CardComponent } from '@app/shared/components/app-card/app-card';
import { IconComponent } from '@app/shared/components/icon/icon';
import { UserProfileView } from '../../models/profile.models';
import {
  isAllowedProfileImageSize,
  isAllowedProfileImageType,
} from '../../services/profile-security';
import { ProfileAvatarComponent } from '../profile-avatar/profile-avatar';

@Component({
  selector: 'app-profile-image-uploader',
  standalone: true,
  imports: [ButtonComponent, CardComponent, IconComponent, ProfileAvatarComponent],
  templateUrl: './profile-image-uploader.html',
  styleUrl: './profile-image-uploader.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileImageUploaderComponent implements OnDestroy {
  readonly profile = input<UserProfileView | null>(null);
  readonly uploading = input(false);
  readonly progress = input(0);
  readonly error = input<string | null>(null);
  readonly canRemove = input(true);

  readonly imageSelected = output<File>();
  readonly removeImage = output<void>();
  readonly retryUpload = output<void>();

  private readonly previewUrlSignal = signal<string | null>(null);
  private readonly selectedFileSignal = signal<File | null>(null);
  protected readonly validationMessage = signal<string | null>(null);

  protected readonly previewUrl = this.previewUrlSignal.asReadonly();
  protected readonly displayImageUrl = computed(() => this.previewUrlSignal() ?? this.profile()?.profileImageUrl ?? null);
  protected readonly hasImage = computed(() => Boolean(this.displayImageUrl()));
  protected readonly progressValue = computed(() => Math.max(0, Math.min(100, this.progress())));

  ngOnDestroy(): void {
    this.revokePreview();
  }

  protected selectImage(event: Event): void {
    const inputElement = event.target as HTMLInputElement;
    const file = inputElement.files?.[0] ?? null;
    inputElement.value = '';

    if (!file) {
      return;
    }

    if (!isAllowedProfileImageType(file)) {
      this.validationMessage.set('Please upload JPG, PNG, or WEBP image.');
      return;
    }

    if (!isAllowedProfileImageSize(file)) {
      this.validationMessage.set('Image is too large. Please choose a smaller file.');
      return;
    }

    this.validationMessage.set(null);
    this.selectedFileSignal.set(file);
    this.revokePreview();
    this.previewUrlSignal.set(URL.createObjectURL(file));
    this.imageSelected.emit(file);
  }

  protected remove(): void {
    this.revokePreview();
    this.selectedFileSignal.set(null);
    this.validationMessage.set(null);
    this.removeImage.emit();
  }

  protected retry(): void {
    const file = this.selectedFileSignal();
    if (!file) {
      this.retryUpload.emit();
      return;
    }

    this.validationMessage.set(null);
    this.imageSelected.emit(file);
  }

  private revokePreview(): void {
    const previewUrl = this.previewUrlSignal();
    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
      this.previewUrlSignal.set(null);
    }
  }
}
