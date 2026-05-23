import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';

import { ThemeStore } from '@app/core/theme/theme.store';
import { IconComponent } from '../icon/icon';

type ThemeToggleTone = 'ghost' | 'surface';

@Component({
  selector: 'app-theme-toggle',
  standalone: true,
  imports: [IconComponent],
  templateUrl: './theme-toggle.html',
  styleUrl: './theme-toggle.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThemeToggleComponent {
  private readonly themeStore = inject(ThemeStore);

  readonly tone = input<ThemeToggleTone>('surface');

  protected readonly theme = this.themeStore.theme;
  protected readonly ariaLabel = computed(() =>
    this.theme() === 'dark' ? 'Switch to light mode' : 'Switch to dark mode',
  );

  protected toggleTheme(): void {
    this.themeStore.toggleTheme();
  }
}
