import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

import { IconComponent } from '../icon/icon';

export interface AppBreadcrumbItem {
  readonly label: string;
  readonly route?: string | unknown[];
}

@Component({
  selector: 'app-context-breadcrumb',
  standalone: true,
  imports: [RouterLink, IconComponent],
  templateUrl: './app-context-breadcrumb.html',
  styleUrl: './app-context-breadcrumb.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppContextBreadcrumbComponent {
  readonly items = input<readonly AppBreadcrumbItem[]>([]);
}
