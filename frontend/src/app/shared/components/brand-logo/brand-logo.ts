import { NgOptimizedImage } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ThemeStore } from '@app/core/theme/theme.store';

type BrandLogoSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'app-brand-logo',
  standalone: true,
  templateUrl: './brand-logo.html',
  styleUrl: './brand-logo.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    NgOptimizedImage
  ]
})
export class BrandLogoComponent {
  private readonly themeStore = inject(ThemeStore);

  readonly size = input<BrandLogoSize>('md');
  readonly elevated = input(true);

  protected readonly logoSrc = computed(() =>
    this.themeStore.theme() === 'light' ? '/assets/lebhas-logo-header-light.png' : '/assets/lebhas-logo-header.png',
  );
}
