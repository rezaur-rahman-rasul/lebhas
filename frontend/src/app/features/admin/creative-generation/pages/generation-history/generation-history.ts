import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { ButtonComponent } from '@app/shared/components/button/button';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { ModalShellComponent } from '@app/shared/components/modal-shell/modal-shell';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';
import {
  PLATFORM_OPTIONS,
  PromptPlatform,
  campaignObjectiveLabel,
  promptPlatformLabel,
} from '@app/features/admin/prompts/models/prompt.models';
import { CreativeOutputGallery } from '../../components/creative-output-gallery/creative-output-gallery';
import { GenerationJobCard } from '../../components/generation-job-card/generation-job-card';
import { GenerationProgress } from '../../components/generation-progress/generation-progress';
import {
  CREATIVE_GENERATION_STATUS_OPTIONS,
  CREATIVE_TYPE_OPTIONS,
  CreativeGenerationFilter,
  CreativeGenerationRequest,
  CreativeGenerationStatus,
  CreativeOutput,
  CreativeType,
  DEFAULT_CREATIVE_GENERATION_FILTERS,
  creativeGenerationStatusLabel,
  creativeGenerationStatusTone,
  creativeTypeLabel,
} from '../../models/creative-generation.models';
import { CreativeGenerationStore } from '../../state/creative-generation.store';

@Component({
  selector: 'app-generation-history-page',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    BadgeComponent,
    ButtonComponent,
    CardComponent,
    EmptyStateComponent,
    ModalShellComponent,
    PageHeaderComponent,
    GenerationProgress,
    GenerationJobCard,
    CreativeOutputGallery,
  ],
  templateUrl: './generation-history.html',
  styleUrl: './generation-history.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GenerationHistoryPage {
  protected readonly store = inject(CreativeGenerationStore);
  private readonly router = inject(Router);

  protected readonly filterForm = new FormGroup({
    status: new FormControl<CreativeGenerationStatus | ''>('', { nonNullable: true }),
    creativeType: new FormControl<CreativeType | ''>('', { nonNullable: true }),
    platform: new FormControl<PromptPlatform | ''>('', { nonNullable: true }),
  });

  protected readonly statusOptions = CREATIVE_GENERATION_STATUS_OPTIONS;
  protected readonly creativeTypeOptions = CREATIVE_TYPE_OPTIONS;
  protected readonly platformOptions = PLATFORM_OPTIONS;
  protected readonly statusLabel = creativeGenerationStatusLabel;
  protected readonly statusTone = creativeGenerationStatusTone;
  protected readonly creativeTypeLabel = creativeTypeLabel;
  protected readonly promptPlatformLabel = promptPlatformLabel;
  protected readonly campaignObjectiveLabel = campaignObjectiveLabel;

  constructor() {
    void this.store.loadGenerationRequests();
  }

  protected async applyFilters(): Promise<void> {
    await this.store.loadGenerationRequests(this.buildFilters(), 0);
  }

  protected async resetFilters(): Promise<void> {
    this.filterForm.reset(
      {
        status: '',
        creativeType: '',
        platform: '',
      },
      { emitEvent: false },
    );
    await this.store.loadGenerationRequests(DEFAULT_CREATIVE_GENERATION_FILTERS, 0);
  }

  protected openDetail(request: CreativeGenerationRequest): void {
    void this.store.loadRequestDetail(request.id);
  }

  protected closeDetail(): void {
    this.store.clearSelectedRequest();
  }

  protected async retryRequest(request: CreativeGenerationRequest): Promise<void> {
    await this.store.loadRequestDetail(request.id);
    await this.store.retrySelectedGeneration();
  }

  protected async cancelRequest(request: CreativeGenerationRequest): Promise<void> {
    await this.store.loadRequestDetail(request.id);
    await this.store.cancelSelectedGeneration();
  }

  protected previousPage(): void {
    void this.store.loadGenerationRequests(undefined, this.store.generationPagination().page - 1);
  }

  protected nextPage(): void {
    void this.store.loadGenerationRequests(undefined, this.store.generationPagination().page + 1);
  }

  protected refreshSelected(): void {
    void this.store.refreshSelectedRequest();
  }

  protected previewOutput(output: CreativeOutput): void {
    void this.store.openPreviewUrl(output).then((url) => {
      if (url) {
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    });
  }

  protected downloadOutput(output: CreativeOutput): void {
    void this.store.openDownloadUrl(output).then((url) => {
      if (url) {
        window.open(url, '_blank', 'noopener,noreferrer');
      }
    });
  }

  protected openOutputDetail(output: CreativeOutput): void {
    void this.router.navigate(['/admin/creative-generation/outputs', output.id]);
  }

  private buildFilters(): CreativeGenerationFilter {
    const value = this.filterForm.getRawValue();
    return {
      userId: null,
      status: value.status || null,
      creativeType: value.creativeType || null,
      platform: value.platform || null,
    };
  }
}
