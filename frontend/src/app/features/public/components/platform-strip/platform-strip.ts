import { ChangeDetectionStrategy, Component } from '@angular/core';

import { IconComponent } from '@app/shared/components/icon/icon';

interface SupportedPlatform {
  readonly name: string;
  readonly icon: string;
  readonly description: string;
}

@Component({
  selector: 'app-platform-strip',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './platform-strip.html',
  styleUrl: './platform-strip.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlatformStripComponent {
  protected readonly platforms: readonly SupportedPlatform[] = [
    {
      name: 'Facebook',
      icon: 'megaphone',
      description: 'Feed, carousel, and marketplace visuals tuned for high-frequency campaigns.',
    },
    {
      name: 'Instagram',
      icon: 'image',
      description: 'Square posts, stories, and reel covers with polished creative framing.',
    },
    {
      name: 'TikTok',
      icon: 'video',
      description: 'Vertical-first concepts ready for short-form launches and creator workflows.',
    },
    {
      name: 'LinkedIn',
      icon: 'globe',
      description: 'Clean ad creative for B2B offers, hiring campaigns, and professional services.',
    },
  ];
}
