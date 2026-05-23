import { ChangeDetectionStrategy, Component, inject, output, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { ButtonComponent } from '@app/shared/components/button/button';
import { IconComponent } from '@app/shared/components/icon/icon';
import { ThemeToggleComponent } from '@app/shared/components/theme-toggle/theme-toggle';

interface PublicNavItem {
  readonly label: string;
  readonly href: string;
}

@Component({
  selector: 'app-public-navbar',
  standalone: true,
  imports: [RouterLink, ButtonComponent, IconComponent, ThemeToggleComponent],
  templateUrl: './public-navbar.html',
  styleUrl: './public-navbar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicNavbarComponent {
  private readonly auth = inject(CurrentUserStore);

  readonly loginRequested = output<void>();

  protected readonly isAuthenticated = this.auth.isAuthenticated;
  protected readonly menuOpen = signal(false);
  protected readonly navItems: readonly PublicNavItem[] = [
    { label: 'Platforms', href: '#platforms' },
    { label: 'Workflow', href: '#how-it-works' },
    { label: 'Bangladesh', href: '#bangladesh' },
    { label: 'Preview', href: '#preview' },
  ];

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected requestLogin(): void {
    this.closeMenu();
    this.loginRequested.emit();
  }
}
