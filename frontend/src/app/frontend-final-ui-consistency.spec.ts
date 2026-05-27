import { readFileSync } from 'node:fs';
import { join } from 'node:path';

import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { NotificationStateService } from './core/state/notification-state.service';
import { DASHBOARD_NAVIGATION } from './shared/layouts/dashboard-layout/dashboard-navigation';

function read(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8');
}

describe('Final Day 1 to Day 10 UI consistency cleanup', () => {
  it('groups Master navigation by ordinary-user business areas without duplicate monitoring labels', () => {
    const masterItems = DASHBOARD_NAVIGATION.filter((item) => item.roles?.includes('MASTER'));
    const labels = masterItems.map((item) => item.label);
    const groups = new Set(masterItems.map((item) => item.group).filter(Boolean));

    expect(groups).toEqual(
      new Set(['Overview', 'AI Operations', 'System Monitoring', 'Usage & Billing', 'Payments']),
    );
    expect(labels).toContain('AI Dashboard');
    expect(labels).toContain('Monitoring Overview');
    expect(labels).toContain('Alerts');
    expect(labels).not.toContain('AI Monitoring');
    expect(labels).not.toContain('AI Provider Health');
    expect(labels).not.toContain('Monitoring Alerts');
    expect(labels).not.toContain('Payment Monitoring');
    expect(labels).not.toContain('Workspace Monitoring');
    expect(labels.filter((label) => label === 'Provider Health').length).toBe(1);
  });

  it('keeps active route aliases for AI, monitoring, usage, and payment Master screens', () => {
    const providerHealth = DASHBOARD_NAVIGATION.find((item) => item.label === 'Provider Health');
    const monitoring = DASHBOARD_NAVIGATION.find((item) => item.label === 'Monitoring Overview');
    const systemHealth = DASHBOARD_NAVIGATION.find((item) => item.label === 'System Health');
    const alerts = DASHBOARD_NAVIGATION.find((item) => item.label === 'Alerts');

    expect(providerHealth?.activePaths).toContain('/ai-monitoring/providers/metrics');
    expect(monitoring?.activePaths).toContain('/monitoring');
    expect(systemHealth?.activePaths).toContain('/monitoring/system-health');
    expect(alerts?.activePaths).toContain('/monitoring/alerts');
    expect(DASHBOARD_NAVIGATION.some((item) => item.route === '/master/usage/ai-costs')).toBe(true);
    expect(DASHBOARD_NAVIGATION.some((item) => item.route === '/master/payments/providers')).toBe(true);
  });

  it('renders one grouped sidebar with accessible active state and no legacy directives', () => {
    const sidebar = read('src/app/shared/layouts/dashboard-layout/components/app-sidebar/app-sidebar.html');
    const source = read('src/app/shared/layouts/dashboard-layout/components/app-sidebar/app-sidebar.ts');
    const styles = read('src/app/shared/layouts/dashboard-layout/components/app-sidebar/app-sidebar.scss');

    expect(sidebar).toContain('navigationGroups()');
    expect(sidebar).toContain('sidebar-group-label');
    expect(sidebar).toContain('[attr.aria-current]="isActive(item) ?');
    expect(sidebar).not.toMatch(/\*ngIf\b|\*ngFor\b/);
    expect(source).toContain('groupNavigationItems');
    expect(styles).toContain('scrollbar-gutter: stable');
    expect(styles).toContain('scrollbar-width: thin');
  });

  it('uses shared page shell, page header, error, empty, and loading patterns', () => {
    const layout = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.html');
    const layoutStyles = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.scss');
    const pageHeader = read('src/app/shared/components/app-page-header/app-page-header.html');
    const errorState = read('src/app/shared/components/app-error-state/app-error-state.html');
    const emptyState = read('src/app/shared/components/app-empty-state/app-empty-state.html');
    const loadingState = read('src/app/shared/components/app-loading-state/app-loading-state.html');

    expect(layout).toContain('dashboard-page-container');
    expect(layoutStyles).toContain('width: min(100%, 112rem)');
    expect(pageHeader).toContain('border-b border-border pb-5');
    expect(errorState).toContain('role="alert"');
    expect(emptyState).toContain('border-dashed');
    expect(loadingState).toContain('min-h-[16rem]');
  });

  it('deduplicates duplicate toasts and limits visible notifications to two', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([])],
    });

    const notifications = TestBed.inject(NotificationStateService);

    notifications.error('Usage data could not load', 'We could not load this data. Please try again.', 'usage|500');
    notifications.error('Usage data could not load', 'We could not load this data. Please try again.', 'usage|500');
    notifications.error('Payment data could not load', 'We could not load this data. Please try again.', 'payment|500');
    notifications.error('System health could not load', 'We could not load this data. Please try again.', 'health|500');
    notifications.error('AI monitoring data could not load', 'We could not load this data. Please try again.', 'ai|500');

    expect(notifications.notifications().length).toBe(2);
    expect(notifications.notifications().map((item) => item.key)).toEqual([
      'health|500',
      'ai|500',
    ]);
  });

  it('uses a dark custom workspace dropdown instead of a native select', () => {
    const layout = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.html');
    const source = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.ts');
    const styles = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.scss');

    expect(layout).toContain('role="listbox"');
    expect(layout).toContain('role="option"');
    expect(layout).toContain('Search workspaces');
    expect(layout).toContain('[title]="option.name"');
    expect(layout).not.toContain('<select');
    expect(source).toContain('workspaceDropdownOpen = signal(false)');
    expect(source).toContain("document:keydown.escape");
    expect(styles).toContain('.workspace-option');
  });

  it('keeps global error handling friendly and prevents raw repeated server-error copy', () => {
    const httpError = read('src/app/core/api/http-error.ts');
    const interceptor = read('src/app/core/interceptors/error.interceptor.ts');
    const toast = read('src/app/shared/components/toast/toast.html');

    expect(httpError).toContain('We could not load this data. Please try again.');
    expect(httpError).toContain('This data is not available yet. Please try again later.');
    expect(interceptor).toContain('requestFailureTitle');
    expect(interceptor).toContain('requestFailureMessage');
    expect(toast).toContain('top-20');
    expect(toast).toContain('Dismiss notification:');
  });
});
