import { expect, Page, test } from '@playwright/test';

import { registerMockApi, seedStoredSession } from './support/mock-api';

test('login dialog opens correctly', async ({ page }) => {
  await page.goto('/?auth=login');

  await expect(page.getByTestId('login-modal')).toBeVisible();
  await expect(
    page.getByRole('heading', { name: /access your creative workspace/i }),
  ).toBeVisible();
});

test('split auth layout renders correctly', async ({ page }) => {
  await page.goto('/?auth=login');

  const authDialog = page.getByTestId('auth-dialog-split');
  await expect(authDialog).toBeVisible();
  await expect(page.getByTestId('auth-tab-login')).toBeVisible();
  await expect(page.getByTestId('auth-tab-register')).toBeVisible();
  await expect(authDialog.getByAltText(/lebhas - brand attire logo/i)).toBeVisible();
});

test('JWT auth flow works', async ({ page }) => {
  const api = await registerMockApi(page, { role: 'ADMIN' });
  await page.goto('/?auth=login');
  await page.getByTestId('login-email').fill('admin@example.com');
  await page.getByTestId('login-password').fill('password123');
  await page.getByTestId('login-submit').click();

  await expect(page).toHaveURL(/\/dashboard$/);
  await expect(page.getByTestId('dashboard-sidebar')).toBeVisible();
  expect(api.counters.login).toBe(1);
});

test('homepage transformation workflow renders', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByTestId('hero-tagline')).toHaveText(
    /transform raw product images into high-converting campaign creatives/i,
  );
  await expect(page.getByTestId('hero-transform-grid')).toBeVisible();
  await expect(page.getByAltText(/raw product flat lay image/i)).toBeVisible();
  await expect(page.getByTestId('hero-processing-panel')).toBeVisible();
  await expect(page.getByAltText(/campaign creative output image/i)).toBeVisible();
});

test('feature cards display icons and labels', async ({ page }) => {
  await page.goto('/');

  const featureCards = page.getByTestId('feature-card');
  await expect(featureCards).toHaveCount(4);

  for (const title of ['Fast & Efficient', 'Cost Effective', 'Quality Optimised', 'Scalable']) {
    await expect(page.getByText(title, { exact: true })).toBeVisible();
  }

  const featureCardCount = await featureCards.count();
  for (let index = 0; index < featureCardCount; index += 1) {
    await expect(featureCards.nth(index).locator('svg')).toBeVisible();
  }
});

test('workflow panel is shown and scrollable on mobile', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');

  const stepper = page.getByTestId('workflow-stepper');
  await expect(stepper).toBeVisible();
  await expect(page.getByTestId('workflow-step-card')).toHaveCount(5);

  const isScrollable = await stepper.evaluate(
    (element) => element.scrollWidth > element.clientWidth + 1,
  );
  expect(isScrollable).toBeTruthy();
});

test('workspace overview renders metrics and activity', async ({ page }) => {
  await page.goto('/');

  const workspace = page.getByTestId('workspace-overview');
  await expect(workspace).toBeVisible();
  await expect(workspace.getByText('Brands', { exact: true })).toBeVisible();
  await expect(workspace.getByText('Creative Requests', { exact: true })).toBeVisible();
  await expect(workspace.getByText('Recent Activity', { exact: true })).toBeVisible();
  await expect(workspace.getByText('Top Platforms', { exact: true })).toBeVisible();
});

test('header login button opens the login modal', async ({ page }) => {
  await page.goto('/');

  await page.getByTestId('navbar-login-button').click();
  await expect(page.getByTestId('login-modal')).toBeVisible();
});

test('workspace context loads correctly', async ({ page }) => {
  await registerMockApi(page, { role: 'ADMIN' });
  await seedStoredSession(page, {
    activeWorkspaceId: '11111111-1111-1111-1111-111111111111',
  });

  await page.goto('/dashboard');

  await expect(page.getByTestId('workspace-switcher')).toHaveValue(
    '11111111-1111-1111-1111-111111111111',
  );
  await expect(page.getByTestId('dashboard-sidebar').getByText('Lebhas HQ')).toBeVisible();
});

test('role-aware navigation works', async ({ page }) => {
  await expectRoleSidebar(page, 'MASTER', [
    'System Dashboard',
    'Workspaces',
    'Users',
    'Support Access',
  ]);
  await expectRoleSidebar(page, 'ADMIN', [
    'Dashboard',
    'Brands',
    'Products/Services',
    'Projects/Campaigns',
  ]);
  await expectRoleSidebar(page, 'CREW', ['Dashboard', 'Assigned Projects']);
});

test('brand forms validate correctly', async ({ page }) => {
  await registerMockApi(page, { role: 'ADMIN' });
  await seedStoredSession(page, {
    activeWorkspaceId: '11111111-1111-1111-1111-111111111111',
  });

  await page.goto('/brands');
  await page.getByTestId('brand-create-button').click();
  await page.getByTestId('brand-submit').click();

  await expect(page.getByText('Enter a brand name.')).toBeVisible();
});

test('product service requires brand', async ({ page }) => {
  await registerMockApi(page, { role: 'ADMIN' });
  await seedStoredSession(page, {
    activeWorkspaceId: '11111111-1111-1111-1111-111111111111',
  });

  await page.goto('/product-services');
  await page.getByTestId('product-create-button').click();
  await page.getByTestId('product-submit').click();

  await expect(page.getByText('Select a brand.')).toBeVisible();
});

test('project campaign requires product service', async ({ page }) => {
  await registerMockApi(page, { role: 'ADMIN' });
  await seedStoredSession(page, {
    activeWorkspaceId: '11111111-1111-1111-1111-111111111111',
  });

  await page.goto('/projects');
  await page.getByTestId('project-create-button').click();
  await page.getByTestId('project-submit').click();

  await expect(page.getByText('Select a product or service.')).toBeVisible();
});

test('sidebar responsive behavior works', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await registerMockApi(page, { role: 'ADMIN' });
  await seedStoredSession(page, {
    activeWorkspaceId: '11111111-1111-1111-1111-111111111111',
  });

  await page.goto('/dashboard');
  await page.getByLabel('Open sidebar').click();
  await expect(page.getByTestId('dashboard-sidebar')).toBeVisible();
  await page.getByLabel('Close sidebar').click();
  await expect(page.getByLabel('Open sidebar')).toBeVisible();
});

test('mobile responsiveness works', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');

  await expect(page.getByLabel('Toggle navigation')).toBeVisible();
  const hasHorizontalOverflow = await page.evaluate(
    () => document.documentElement.scrollWidth > window.innerWidth + 1,
  );
  expect(hasHorizontalOverflow).toBeFalsy();
});

test('dark mode works', async ({ page }) => {
  await page.goto('/');

  await expect
    .poll(() => page.evaluate(() => document.documentElement.classList.contains('dark')))
    .toBeTruthy();
});

test('light mode works', async ({ page }) => {
  await page.goto('/');
  await page.evaluate(() => {
    window.localStorage.setItem('creative_saas.theme', 'light');
  });
  await page.reload();

  await expect
    .poll(() => page.evaluate(() => document.documentElement.classList.contains('light')))
    .toBeTruthy();
});

test('no human-image sections exist', async ({ page }) => {
  await page.goto('/');

  const text = (await page.locator('main').textContent())?.toLowerCase() ?? '';
  expect(text).not.toContain('model');
  expect(text).not.toContain('portrait');
  expect(text).not.toContain('influencer');

  const imageSources = await page
    .locator('main img')
    .evaluateAll((images) => images.map((image) => image.getAttribute('src') ?? ''));
  expect(imageSources.every((source) => source.startsWith('/assets/'))).toBeTruthy();
});

test('loading states render correctly', async ({ page }) => {
  await registerMockApi(page, { role: 'ADMIN', responseDelayMs: 500 });
  await seedStoredSession(page, {
    activeWorkspaceId: '11111111-1111-1111-1111-111111111111',
  });

  await page.goto('/brands');

  await expect(page.getByText('Loading brands')).toBeVisible();
});

async function expectRoleSidebar(
  page: Page,
  role: 'MASTER' | 'ADMIN' | 'CREW',
  labels: readonly string[],
): Promise<void> {
  await registerMockApi(page, {
    role,
    workspaceId: role === 'MASTER' ? null : '11111111-1111-1111-1111-111111111111',
    activeWorkspaceId: '11111111-1111-1111-1111-111111111111',
  });
  await seedStoredSession(page, {
    activeWorkspaceId: '11111111-1111-1111-1111-111111111111',
  });

  await page.goto('/dashboard');
  const sidebar = page.getByTestId('dashboard-sidebar');
  await expect(sidebar).toBeVisible();
  for (const label of labels) {
    await expect(sidebar.getByText(label, { exact: true })).toBeVisible();
  }
}
