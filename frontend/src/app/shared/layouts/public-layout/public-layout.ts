import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AuthLayoutComponent } from '@app/shared/layouts/auth-shell/auth-shell';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [RouterOutlet, AuthLayoutComponent],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicLayoutComponent {}
