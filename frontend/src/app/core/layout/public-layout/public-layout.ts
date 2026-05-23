import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { AuthLayoutComponent } from '@app/features/auth/components/auth-layout/auth-layout';

@Component({
  selector: 'app-public-layout',
  standalone: true,
  imports: [RouterOutlet, AuthLayoutComponent],
  templateUrl: './public-layout.html',
  styleUrl: './public-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicLayoutComponent {}
