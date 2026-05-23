import { ChangeDetectionStrategy, Component, output } from '@angular/core';

import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';

interface HeroSignal {
  readonly label: string;
  readonly value: string;
}

@Component({
  selector: 'app-hero-section',
  standalone: true,
  imports: [ButtonComponent, IconComponent],
  templateUrl: './hero-section.html',
  styleUrl: './hero-section.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HeroSectionComponent {
  readonly primaryActionRequested = output<void>();
  readonly secondaryActionRequested = output<void>();

  protected readonly rawImageUrl = '/assets/product-flatlay.png';
  protected readonly creativeImageUrl = '/assets/campaign-poster.png';
  protected readonly storyboardImageUrl = '/assets/packaging-bundle.png';
  protected readonly signals: readonly HeroSignal[] = [
    { label: 'Languages', value: 'Bangla + English' },
    { label: 'Platforms', value: 'Facebook, Instagram, TikTok, LinkedIn' },
    { label: 'Hierarchy', value: 'Workspace, brand, product, project, request' },
  ];
}
