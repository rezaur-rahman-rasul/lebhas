import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { CardComponent } from '@app/shared/components/card/card';
import { EmptyStateComponent } from '@app/shared/components/empty-state/empty-state';
import { PageHeaderComponent } from '@app/shared/components/page-header/page-header';

interface RoleSectionHighlight {
  readonly title: string;
  readonly description: string;
}

interface RoleSectionData {
  readonly eyebrow: string;
  readonly title: string;
  readonly description: string;
  readonly badgeLabel: string;
  readonly badgeTone: 'brand' | 'blue' | 'red' | 'neutral';
  readonly emptyIcon: string;
  readonly emptyTitle: string;
  readonly emptyDescription: string;
  readonly highlights: readonly RoleSectionHighlight[];
}

@Component({
  selector: 'app-role-section-page',
  standalone: true,
  imports: [BadgeComponent, CardComponent, EmptyStateComponent, PageHeaderComponent],
  templateUrl: './role-section-page.html',
  styleUrl: './role-section-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RoleSectionPageComponent {
  private readonly route = inject(ActivatedRoute);

  protected readonly section = this.route.snapshot.data['section'] as RoleSectionData;
}
