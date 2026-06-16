import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { BadgeComponent } from '@app/shared/components/badge/badge';
import { BrandLogoComponent } from '@app/shared/components/brand-logo/brand-logo';
import { IconComponent } from '@app/shared/components/icon/icon';

@Component({
  selector: 'app-auth-header',
  standalone: true,
  imports: [BadgeComponent, BrandLogoComponent, IconComponent],
  templateUrl: './auth-header.html',
  styleUrl: './auth-header.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthHeaderComponent {
  readonly badgeLabel = input.required<string>();
  readonly badgeTone = input<'brand' | 'blue' | 'red' | 'neutral'>('brand');
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly icon = input('shield-check');
}
