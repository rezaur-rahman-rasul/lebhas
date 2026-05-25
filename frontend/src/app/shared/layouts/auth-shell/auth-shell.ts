import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ThemeToggleComponent } from '@app/shared/components/theme-toggle/theme-toggle';
import { AuthFooterComponent } from '@app/features/auth/components/auth-footer/auth-footer';
import { CreativePreviewHeroComponent } from '@app/features/auth/components/creative-preview-hero/creative-preview-hero';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [AuthFooterComponent, CreativePreviewHeroComponent, ThemeToggleComponent],
  templateUrl: './auth-shell.html',
  styleUrl: './auth-shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthLayoutComponent {}
