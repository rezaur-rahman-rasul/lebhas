import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { NavigationItem } from '@app/shared/layouts/navigation.models';
import { BadgeComponent } from '../badge/badge';
import { BrandLogoComponent } from '../brand-logo/brand-logo';
import { IconComponent } from '../icon/icon';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, BadgeComponent, BrandLogoComponent, IconComponent],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarComponent {
  readonly brandName = input('Lebhas - Brand Attire');
  readonly workspaceLabel = input('Select workspace');
  readonly role = input('ADMIN');
  readonly items = input<readonly NavigationItem[]>([]);
  readonly compact = input(false);
  readonly itemSelected = output<void>();

  protected roleBadgeTone(): 'brand' | 'blue' | 'red' | 'neutral' {
    const role = this.role();
    return role === 'MASTER' ? 'red' : role === 'CREW' ? 'blue' : 'brand';
  }
}
