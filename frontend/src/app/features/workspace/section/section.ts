import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { SectionHeaderComponent } from '@app/shared/components/section-header/section-header';

interface WorkspaceSectionCard {
  readonly title: string;
  readonly description: string;
}

interface WorkspaceSectionData {
  readonly eyebrow: string;
  readonly title: string;
  readonly description: string;
  readonly badge: string;
  readonly badgeTone: 'neutral' | 'brand' | 'blue' | 'red';
  readonly emptyIcon: string;
  readonly emptyTitle: string;
  readonly emptyDescription: string;
  readonly cards: readonly WorkspaceSectionCard[];
}

@Component({
  selector: 'app-workspace-section-page',
  standalone: true,
  imports: [BadgeComponent, CardComponent, EmptyStateComponent, SectionHeaderComponent],
  templateUrl: './section.html',
  styleUrl: './section.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WorkspaceSectionPageComponent {
  private readonly route = inject(ActivatedRoute);

  protected readonly section = this.route.snapshot.data['section'] as WorkspaceSectionData;
}

