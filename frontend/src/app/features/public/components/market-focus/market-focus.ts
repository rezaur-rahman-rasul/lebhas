import { ChangeDetectionStrategy, Component } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';

interface MarketPersona {
  readonly title: string;
  readonly description: string;
  readonly icon: string;
}

@Component({
  selector: 'app-market-focus',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './market-focus.html',
  styleUrl: './market-focus.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarketFocusComponent {
  protected readonly personas: readonly MarketPersona[] = [
    {
      title: 'SMEs',
      description: 'Move faster on product promotions without building a heavyweight design process first.',
      icon: 'building-2',
    },
    {
      title: 'Agencies',
      description: 'Keep multiple client projects organized while staying close to platform-specific output.',
      icon: 'users',
    },
    {
      title: 'Freelancers',
      description: 'Reduce the time between receiving raw assets and handing back usable campaign creative.',
      icon: 'sparkles',
    },
    {
      title: 'E-commerce sellers',
      description: 'Turn catalog images into stronger creative for seasonal launches and daily promotions.',
      icon: 'megaphone',
    },
  ];
}
