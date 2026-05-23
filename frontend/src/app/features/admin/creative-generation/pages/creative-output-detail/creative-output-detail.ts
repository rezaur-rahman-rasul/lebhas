import { KeyValuePipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { IconComponent } from '@app/shared/components/icon/icon';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import { promptPlatformLabel } from '@app/features/admin/prompts/models/prompt.models';
import {
  creativeGenerationStatusLabel,
  creativeGenerationStatusTone,
  creativeOutputFormatLabel,
  creativeTypeLabel,
  formatOutputDimensions,
  isImageFormat,
} from '../../models/creative-generation.models';
import { CreativeGenerationStore } from '../../state/creative-generation.store';

@Component({
  selector: 'app-creative-output-detail-page',
  standalone: true,
  imports: [
    DatePipe,
    KeyValuePipe,
    BadgeComponent,
    ButtonComponent,
    EmptyStateComponent,
    IconComponent,
    PageHeaderComponent,
  ],
  templateUrl: './creative-output-detail.html',
  styleUrl: './creative-output-detail.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreativeOutputDetailPage {
  protected readonly store = inject(CreativeGenerationStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly statusLabel = creativeGenerationStatusLabel;
  protected readonly statusTone = creativeGenerationStatusTone;
  protected readonly creativeTypeLabel = creativeTypeLabel;
  protected readonly outputFormatLabel = creativeOutputFormatLabel;
  protected readonly promptPlatformLabel = promptPlatformLabel;
  protected readonly formatOutputDimensions = formatOutputDimensions;
  protected readonly isImageFormat = isImageFormat;

  constructor() {
    const outputId = this.route.snapshot.paramMap.get('outputId');
    if (outputId) {
      void this.store.loadOutputDetail(outputId);
    }
  }

  protected backToHistory(): void {
    void this.router.navigate(['/admin/creative-generation/history']);
  }

  protected async preview(): Promise<void> {
    const output = this.store.selectedOutput();
    if (!output) {
      return;
    }
    const url = await this.store.openPreviewUrl(output);
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  }

  protected async download(): Promise<void> {
    const output = this.store.selectedOutput();
    if (!output) {
      return;
    }
    const url = await this.store.openDownloadUrl(output);
    if (url) {
      window.open(url, '_blank', 'noopener,noreferrer');
    }
  }
}
