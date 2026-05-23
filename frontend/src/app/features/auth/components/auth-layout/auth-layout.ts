import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ThemeToggleComponent } from '@app/shared/components/theme-toggle/theme-toggle';
import { AuthFooterComponent } from '../auth-footer/auth-footer';
import { CreativePreviewHeroComponent } from '../creative-preview-hero/creative-preview-hero';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [AuthFooterComponent, CreativePreviewHeroComponent, ThemeToggleComponent],
  templateUrl: './auth-layout.html',
  styleUrl: './auth-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthLayoutComponent {}
