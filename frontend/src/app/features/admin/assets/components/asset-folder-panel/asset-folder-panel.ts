import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';
import {
  AssetFolder,
  AssetFolderSelection,
  CreateAssetFolderPayload,
  UpdateAssetFolderPayload,
} from '../../models/asset.models';

@Component({
  selector: 'app-asset-folder-panel',
  standalone: true,
  imports: [ReactiveFormsModule, ButtonComponent, IconComponent],
  templateUrl: './asset-folder-panel.html',
  styleUrl: './asset-folder-panel.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AssetFolderPanel {
  readonly folders = input<readonly AssetFolder[]>([]);
  readonly selectedFolder = input<AssetFolderSelection>('all');
  readonly canManageFolders = input(false);
  readonly loading = input(false);

  readonly folderSelected = output<AssetFolderSelection>();
  readonly folderCreated = output<CreateAssetFolderPayload>();
  readonly folderRenamed = output<{ readonly folderId: string; readonly payload: UpdateAssetFolderPayload }>();
  readonly folderDeleted = output<string>();

  protected readonly showCreateForm = signal(false);
  protected readonly editingFolderId = signal<string | null>(null);

  protected readonly createForm = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(120)],
    }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(500)],
    }),
  });

  protected readonly editForm = new FormGroup({
    name: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(120)],
    }),
    description: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(500)],
    }),
  });

  protected readonly hasFolders = computed(() => this.folders().length > 0);

  protected toggleCreateForm(): void {
    this.showCreateForm.update((current) => !current);
    this.createForm.reset({ name: '', description: '' });
  }

  protected beginRename(folder: AssetFolder): void {
    this.editingFolderId.set(folder.id);
    this.editForm.reset({
      name: folder.name,
      description: folder.description ?? '',
    });
  }

  protected cancelRename(): void {
    this.editingFolderId.set(null);
  }

  protected createFolder(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.folderCreated.emit({
      name: this.createForm.controls.name.value.trim(),
      parentFolderId: null,
      description: this.createForm.controls.description.value.trim(),
    });
    this.showCreateForm.set(false);
    this.createForm.reset({ name: '', description: '' });
  }

  protected saveRename(folderId: string): void {
    if (this.editForm.invalid) {
      this.editForm.markAllAsTouched();
      return;
    }

    this.folderRenamed.emit({
      folderId,
      payload: {
        name: this.editForm.controls.name.value.trim(),
        parentFolderId: null,
        description: this.editForm.controls.description.value.trim(),
      },
    });
    this.editingFolderId.set(null);
  }
}
