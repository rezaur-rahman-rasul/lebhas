import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { describe, expect, it } from 'vitest';

function read(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8');
}

describe('Home header actions', () => {
  it('renders one theme toggle icon at a time with accessible switch labels', () => {
    const source = read('src/app/features/public/home/home.ts');
    const template = read('src/app/features/public/home/home.html');

    expect(source).toContain("currentTheme() === 'dark' ? 'sun' : 'moon'");
    expect(source).toContain('Switch to light mode');
    expect(source).toContain('Switch to dark mode');
    expect(template).toContain('[name]="themeToggleIcon()"');
    expect(template).toContain('[attr.aria-label]="themeToggleLabel()"');
    expect(template).not.toContain('theme-pill-option');
    expect(template).not.toContain('<app-theme-toggle');
  });

  it('keeps header action order and opens existing auth dialog in the right mode', () => {
    const source = read('src/app/features/public/home/home.ts');
    const template = read('src/app/features/public/home/home.html');

    expect(template.indexOf('class="theme-pill"')).toBeLessThan(template.indexOf('class="language-toggle"'));
    expect(template.indexOf('data-testid="navbar-login-button"')).toBeLessThan(
      template.indexOf('data-testid="navbar-get-started-button"'),
    );
    expect(source).toContain("this.authInitialTab.set('login')");
    expect(source).toContain("this.authInitialTab.set('register')");
    expect(template).toContain('[initialTab]="authInitialTab()"');
  });

  it('keeps Get Started responsive and avoids legacy structural directives', () => {
    const template = read('src/app/features/public/home/home.html');
    const styles = read('src/app/features/public/home/home.scss');

    expect(template).toContain('navbar-get-started-button');
    expect(template).toContain('header-primary-button mobile-full');
    expect(template).not.toMatch(/\*ngIf\b|\*ngFor\b/);
    expect(styles).toContain('.header-primary-button');
    expect(styles).toContain('.theme-pill:focus-visible');
  });
});
