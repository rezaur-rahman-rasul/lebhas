import { DOCUMENT } from '@angular/common';
import { Injectable, computed, effect, inject, signal } from '@angular/core';

import { DEFAULT_THEME, THEME_STORAGE_KEY, Theme } from './theme.types';

@Injectable({ providedIn: 'root' })
export class ThemeStore {
  private readonly document = inject(DOCUMENT);
  private readonly themeSignal = signal<Theme>(this.readStoredTheme());

  readonly theme = this.themeSignal.asReadonly();
  readonly isDark = computed(() => this.themeSignal() === 'dark');

  constructor() {
    effect(() => {
      const theme = this.themeSignal();
      this.persist(theme);
      this.applyTheme(theme);
    });
  }

  initialize(): void {
    this.applyTheme(this.themeSignal());
  }

  setTheme(theme: Theme): void {
    this.themeSignal.set(theme);
  }

  toggleTheme(): void {
    this.themeSignal.update((theme) => (theme === 'dark' ? 'light' : 'dark'));
  }

  private readStoredTheme(): Theme {
    try {
      const storedTheme = globalThis.localStorage?.getItem(THEME_STORAGE_KEY);
      return storedTheme === 'light' ? 'light' : DEFAULT_THEME;
    } catch {
      return DEFAULT_THEME;
    }
  }

  private persist(theme: Theme): void {
    try {
      globalThis.localStorage?.setItem(THEME_STORAGE_KEY, theme);
    } catch {
      // Ignore storage access issues and keep the in-memory theme.
    }
  }

  private applyTheme(theme: Theme): void {
    const root = this.document.documentElement;
    root.classList.remove('dark', 'light');
    root.classList.add(theme);
    root.dataset['theme'] = theme;
  }
}
