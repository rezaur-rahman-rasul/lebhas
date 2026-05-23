import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { CreativeOutput } from '../../models/creative-generation.models';
import { CreativeOutputCard } from '../creative-output-card/creative-output-card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';

@Component({
  selector: 'app-creative-output-gallery',
  standalone: true,
  imports: [CreativeOutputCard, EmptyStateComponent],
  templateUrl: './creative-output-gallery.html',
  styleUrl: './creative-output-gallery.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CreativeOutputGallery {
  readonly outputs = input<readonly CreativeOutput[]>([]);
  readonly loading = input(false);
  readonly canDownload = input(false);

  readonly previewRequested = output<CreativeOutput>();
  readonly downloadRequested = output<CreativeOutput>();
  readonly detailRequested = output<CreativeOutput>();
}
