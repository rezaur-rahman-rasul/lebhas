import { ChangeDetectionStrategy, Component } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';

interface Highlight {
  readonly title: string;
  readonly description: string;
  readonly icon: string;
}

@Component({
  selector: 'app-feature-highlights',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './feature-highlights.html',
  styleUrl: './feature-highlights.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeatureHighlightsComponent {
  protected readonly highlights: readonly Highlight[] = [
    {
      title: 'Workspace-aware operations',
      description: 'Keep every brand, product, and project inside the correct tenant and access scope.',
      icon: 'badge-check',
    },
    {
      title: 'Brand and product context',
      description: 'Prepare creative generation around actual catalog items and service offers instead of generic prompts.',
      icon: 'folder-kanban',
    },
    {
      title: 'Campaign hierarchy',
      description: 'Map projects, creative requests, generated versions, and approvals in a clean SaaS hierarchy.',
      icon: 'image',
    },
    {
      title: 'Social export readiness',
      description: 'Frame assets for Facebook, Instagram, TikTok, and LinkedIn from the first product story.',
      icon: 'pencil-line',
    },
    {
      title: 'Dark-first design system',
      description: 'Start in a premium dark theme with persistent light mode for teams that need a brighter workspace.',
      icon: 'wand-sparkles',
    },
    {
      title: 'Role-aware dashboard shell',
      description: 'Give MASTER, ADMIN, and CREW roles a shared shell with the right navigation foundation.',
      icon: 'users',
    },
  ];
}
