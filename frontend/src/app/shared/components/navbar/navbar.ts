import {
  afterNextRender,
  ChangeDetectionStrategy,
  Component,
  HostListener,
  effect,
  input,
  output,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { BrandLogoComponent } from '../brand-logo/brand-logo';
import { ButtonComponent } from '../button/button';
import { IconComponent } from '../icon/icon';
import { ThemeToggleComponent } from '../theme-toggle/theme-toggle';

interface NavbarItem {
  readonly label: string;
  readonly href: string;
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [RouterLink, BrandLogoComponent, ButtonComponent, IconComponent, ThemeToggleComponent],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NavbarComponent {
  readonly items = input<readonly NavbarItem[]>([]);
  readonly authenticated = input(false);
  readonly loginRequested = output<void>();

  protected readonly menuOpen = signal(false);
  protected readonly isDesktop = signal(false);

  constructor() {
    this.updateViewportMode();
    afterNextRender(() => this.updateViewportMode());

    effect(() => {
      if (this.isDesktop()) {
        this.menuOpen.set(false);
      }
    });
  }

  @HostListener('window:resize')
  protected onWindowResize(): void {
    this.updateViewportMode();
  }

  protected toggleMenu(): void {
    this.menuOpen.update((value) => !value);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected requestLogin(): void {
    this.closeMenu();
    this.loginRequested.emit();
  }

  private updateViewportMode(): void {
    this.isDesktop.set(globalThis.matchMedia('(min-width: 1024px)').matches);
  }
}
