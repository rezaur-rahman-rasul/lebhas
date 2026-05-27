import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

function read(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8');
}

describe('Light mode theme consistency', () => {
  it('defines production light-mode tokens for app surfaces, text, borders, forms, states, and skeletons', () => {
    const styles = read('src/styles.scss');

    expect(styles).toContain('html.light');
    expect(styles).toContain('--color-canvas: 241 245 249');
    expect(styles).toContain('--color-surface: 255 255 255');
    expect(styles).toContain('--color-border: 203 213 225');
    expect(styles).toContain('--color-muted: 71 85 105');
    expect(styles).toContain('--color-input: 255 255 255');
    expect(styles).toContain('--color-dropdown: 255 255 255');
    expect(styles).toContain('--color-danger-bg: 254 242 242');
    expect(styles).toContain('--color-skeleton: 226 232 240');
  });

  it('keeps dark mode as the default theme with its existing dark canvas token', () => {
    const themeStore = read('src/app/core/theme/theme.types.ts');
    const styles = read('src/styles.scss');

    expect(themeStore).toContain("DEFAULT_THEME: Theme = 'dark'");
    expect(styles).toContain('html.dark');
    expect(styles).toContain('--color-canvas: 6 11 23');
  });

  it('makes the light sidebar readable with strong active state and access-scope contrast', () => {
    const sidebar = read('src/app/shared/layouts/dashboard-layout/components/app-sidebar/app-sidebar.scss');

    expect(sidebar).toContain(":host-context(.light) .sidebar-link");
    expect(sidebar).toContain('color: rgb(51 65 85)');
    expect(sidebar).toContain(":host-context(.light) .sidebar-link--active");
    expect(sidebar).toContain('background: rgb(220 252 231 / 0.88)');
    expect(sidebar).toContain('color: rgb(6 95 70)');
    expect(sidebar).toContain('border-color: rgb(203 213 225)');
  });

  it('keeps topbar and workspace dropdown light surfaces readable', () => {
    const layoutStyles = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.scss');
    const layoutTemplate = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.html');

    expect(layoutStyles).toContain(":host-context(.light) .dashboard-topbar");
    expect(layoutStyles).toContain('background: rgb(248 250 252 / 0.94)');
    expect(layoutStyles).toContain(":host-context(.light) .workspace-option");
    expect(layoutStyles).toContain('background: rgb(220 252 231)');
    expect(layoutTemplate).toContain('role="listbox"');
    expect(layoutTemplate).toContain('role="option"');
  });

  it('maps Tailwind theme colors to centralized tokens and keeps refactored templates modern', () => {
    const tailwind = read('tailwind.config.js');
    const home = read('src/app/features/public/home/home.html');
    const layout = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.html');

    expect(tailwind).toContain("'border-strong': 'rgb(var(--color-border-strong) / <alpha-value>)'");
    expect(tailwind).toContain("input: 'rgb(var(--color-input) / <alpha-value>)'");
    expect(tailwind).toContain("dropdown: 'rgb(var(--color-dropdown) / <alpha-value>)'");
    expect(home).not.toMatch(/\*ngIf\b|\*ngFor\b/);
    expect(layout).not.toMatch(/\*ngIf\b|\*ngFor\b/);
  });
});
