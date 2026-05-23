import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

import { IconComponent } from '../icon/icon';

@Component({
  selector: 'app-sidebar-item',
  standalone: true,
  imports: [RouterLink, RouterLinkActive, IconComponent],
  templateUrl: './sidebar-item.html',
  styleUrl: './sidebar-item.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SidebarItemComponent {
  readonly label = input.required<string>();
  readonly icon = input.required<string>();
  readonly route = input.required<string>();
  readonly compact = input(false);
}
