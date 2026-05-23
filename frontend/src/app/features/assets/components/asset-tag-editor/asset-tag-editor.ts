import { ChangeDetectionStrategy, Component, effect, input, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';
import { MAX_ASSET_TAG_LENGTH, normalizeAssetTag } from '../../models/asset.models';

@Component({
  selector: 'app-asset-tag-editor',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, IconComponent],
  templateUrl: './asset-tag-editor.html',
  styleUrl: './asset-tag-editor.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetTagEditor {
  readonly tags = input<readonly string[]>([]);
  readonly readonly = input(false);
  readonly saving = input(false);

  readonly saved = output<readonly string[]>();

  protected readonly tagControl = new FormControl('', { nonNullable: true });
  protected readonly localTags = signal<readonly string[]>([]);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    effect(() => {
      this.localTags.set([...this.tags()]);
      this.tagControl.setValue('', { emitEvent: false });
      this.errorMessage.set(null);
    });
  }

  protected addTag(): void {
    const nextTag = normalizeAssetTag(this.tagControl.value);
    if (!nextTag) {
      return;
    }

    if (nextTag.length > MAX_ASSET_TAG_LENGTH) {
      this.errorMessage.set(`Tags must be ${MAX_ASSET_TAG_LENGTH} characters or fewer.`);
      return;
    }

    if (this.localTags().includes(nextTag)) {
      this.errorMessage.set('This tag is already applied.');
      return;
    }

    this.localTags.update((tags) => [...tags, nextTag]);
    this.tagControl.setValue('', { emitEvent: false });
    this.errorMessage.set(null);
  }

  protected removeTag(tag: string): void {
    this.localTags.update((tags) => tags.filter((item) => item !== tag));
  }

  protected saveTags(): void {
    this.saved.emit(this.localTags());
  }
}
