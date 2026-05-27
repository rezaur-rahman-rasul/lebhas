import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { CurrentUserStore } from '@app/core/auth/current-user.store';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import { WorkspaceApiService } from '@app/core/workspace/workspace-api.service';
import { WorkspaceStore } from '@app/core/workspace/workspace.store';
import { routes } from './app.routes';
import { GeneratedVersionApiService } from './features/generated-versions/generated-version-api.service';
import { GeneratedVersionStore } from './features/generated-versions/generated-version.store';

const appRoot = join(process.cwd(), 'src/app');

function collectFiles(
  directory: string,
  predicate: (filePath: string) => boolean,
  files: string[] = [],
): string[] {
  for (const entry of readdirSync(directory)) {
    const fullPath = join(directory, entry);
    const stats = statSync(fullPath);

    if (stats.isDirectory()) {
      collectFiles(fullPath, predicate, files);
      continue;
    }

    if (predicate(fullPath)) {
      files.push(fullPath);
    }
  }

  return files;
}

function read(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8');
}

function relativeAppPath(filePath: string): string {
  return relative(appRoot, filePath).replaceAll('\\', '/');
}

function activeDayFiles(extension: string): string[] {
  return collectFiles(appRoot, (filePath) => {
    const appPath = relativeAppPath(filePath);
    return (
      filePath.endsWith(extension) &&
      !appPath.startsWith('features/admin/') &&
      !appPath.endsWith('.spec.ts')
    );
  });
}

describe('Batch 12 global refactor guardrails', () => {
  it('keeps one active auth, workspace, and permission foundation', () => {
    const activeTypescript = activeDayFiles('.ts').map(relativeAppPath);

    expect(activeTypescript.filter((file) => file.endsWith('core/auth/auth-api.service.ts')).length).toBe(1);
    expect(activeTypescript.filter((file) => file.endsWith('core/workspace/workspace-api.service.ts')).length).toBe(1);
    expect(activeTypescript.filter((file) => file.endsWith('core/workspace/workspace.store.ts')).length).toBe(1);
    expect(activeTypescript.filter((file) => file.endsWith('core/permissions/permission.store.ts')).length).toBe(1);
  });

  it('does not use legacy component naming or CSS files in active Day 1 to Day 6 code', () => {
    const oldComponentFiles = collectFiles(appRoot, (filePath) =>
      /\.(component\.ts|component\.html|component\.scss|component\.css|css)$/.test(filePath),
    )
      .map(relativeAppPath)
      .filter((file) => !file.startsWith('features/admin/'));

    expect(oldComponentFiles).toEqual([]);
  });

  it('does not use legacy structural directives in active Day 1 to Day 6 templates', () => {
    const offenders = activeDayFiles('.html')
      .filter((file) => /\*ngIf\b|\*ngFor\b/.test(readFileSync(file, 'utf8')))
      .map(relativeAppPath);

    expect(offenders).toEqual([]);
  });

  it('loads the public homepage as the default route and keeps login inside the split dialog', () => {
    const routeSource = read('src/app/app.routes.ts');

    expect(routes[0]?.path).toBe('');
    expect(routeSource).toContain("import('./features/public/home/home')");

    const home = read('src/app/features/public/home/home.html');
    const authDialog = read('src/app/features/auth/components/auth-dialog/auth-dialog.html');

    expect(home).toContain('data-testid="navbar-login-button"');
    expect(home).toContain('<app-auth-dialog');
    expect(authDialog).toContain('data-testid="auth-dialog-split"');
    expect(authDialog).toContain('lg:grid-cols');
  });

  it('keeps one fixed-height split auth dialog for login and register', () => {
    const activeTypescript = activeDayFiles('.ts').map(relativeAppPath);
    const authDialog = read('src/app/features/auth/components/auth-dialog/auth-dialog.html');
    const authStyles = read('src/app/features/auth/components/auth-dialog/auth-dialog.scss');

    expect(
      activeTypescript.filter((file) => file.endsWith('features/auth/components/auth-dialog/auth-dialog.ts')).length,
    ).toBe(1);
    expect(authDialog).toContain('!h-[min(51.25rem,calc(100dvh-1rem))]');
    expect(authDialog).toContain('sm:!h-[min(51.25rem,calc(100dvh-2rem))]');
    expect(authDialog).toContain('contentClass="h-full min-h-0 !overflow-hidden !p-0"');
    expect(authDialog).toContain('auth-dialog-form-scroll');
    expect(authDialog).toContain('auth-tab-switcher');
    expect(authDialog).toContain('h-11');
    expect(authDialog).toContain('w-fit shrink-0 self-start');
    expect(authDialog).toContain('h-9 min-w-24');
    expect(authDialog).toContain('data-testid="auth-login-panel"');
    expect(authDialog).toContain('data-testid="auth-register-panel"');
    expect(authDialog).toContain('role="tablist"');
    expect(authDialog).toContain('lg:grid-cols-[minmax(26rem,0.92fr)_minmax(30rem,1.08fr)]');
    expect(authDialog).toContain('overflow-y-auto');
    expect(authStyles).toContain('.auth-dialog-form-scroll');
    expect(authStyles).toContain('min-height: 5.5rem');
    expect(authStyles).toContain('max-height: 2.75rem');
    expect(authStyles).toContain('scrollbar-width: none');
  });

  it('keeps auth dialog free of layout hacks and legacy structural directives', () => {
    const authSources = [
      read('src/app/features/auth/components/auth-dialog/auth-dialog.ts'),
      read('src/app/features/auth/components/auth-dialog/auth-dialog.html'),
      read('src/app/features/auth/components/auth-dialog/auth-dialog.scss'),
    ].join('\n');

    expect(authSources).not.toMatch(/setTimeout|dispatchEvent|location\.reload|detectChanges|querySelector|getBoundingClientRect/);
    expect(authSources).not.toMatch(/\*ngIf\b|\*ngFor\b/);
  });
});

describe('Batch 12 UX and responsive coverage', () => {
  it('keeps dashboard quick actions visible above the fold', () => {
    const dashboard = read('src/app/features/dashboard/overview/overview.html');
    const source = read('src/app/features/dashboard/overview/overview.ts');

    expect(dashboard).toContain('quickActions()');
    expect(dashboard).toContain('Hierarchy overview');
    expect(source).toContain('Create Brand');
    expect(source).toContain('Create Product/Service');
    expect(source).toContain('Create Project');
    expect(source).toContain('Upload Asset');
    expect(source).toContain('Create Prompt');
  });

  it('keeps the brand page header and final two-column frame mounted on initial route render', () => {
    const template = read('src/app/features/brands/brands.html');
    const source = read('src/app/features/brands/brands.ts');
    const styles = read('src/app/features/brands/brands.scss');
    const store = read('src/app/features/brands/brand.store.ts');

    expect(template).toContain('eyebrow="Workspace brands"');
    expect(template).toContain('title="Brands"');
    expect(template).toContain(
      'Manage the brand layer that anchors products, campaigns, and later creative requests.',
    );
    expect(template).toContain('brand-create-button');
    expect(template).toContain('brands-page-grid');
    expect(template).toContain('Brand roster');
    expect(template).toContain('Brand detail');
    expect(template).toContain('brand-skeleton-row');
    expect(template).toContain('Select a brand to see details.');
    expect(source).toContain('Bangla and English');
    expect(source).toContain('void this.store.load(workspaceId)');
    expect(source).toContain('selectedBrand = this.store.selectedBrand');
    expect(styles).toContain('grid-template-columns: minmax(0, 1fr) 23.75rem');
    expect(store).toContain('selectedBrandIdSignal');
    expect(store).toContain('selectFirstAvailableBrand');
    expect(store).toContain('brandRosterSubtitle');
  });

  it('keeps brand page free of click refresh hacks and duplicate active brand foundations', () => {
    const activeTypescript = activeDayFiles('.ts').map(relativeAppPath);
    const brandFiles = activeTypescript.filter((file) => file.startsWith('features/brands/'));
    const brandSources = [
      read('src/app/features/brands/brands.ts'),
      read('src/app/features/brands/brands.html'),
      read('src/app/features/brands/brand.store.ts'),
    ].join('\n');

    expect(brandFiles.filter((file) => file.endsWith('brand.store.ts')).length).toBe(1);
    expect(brandFiles.filter((file) => file.endsWith('brand-api.service.ts')).length).toBe(1);
    expect(brandSources).not.toMatch(/setTimeout|dispatchEvent|location\.reload|detectChanges|querySelector/);
    expect(brandSources).not.toMatch(/\*ngIf\b|\*ngFor\b/);
  });

  it('keeps the product services page header and final relationship frame mounted on initial route render', () => {
    const template = read('src/app/features/product-services/product-services.html');
    const source = read('src/app/features/product-services/product-services.ts');
    const styles = read('src/app/features/product-services/product-services.scss');
    const store = read('src/app/features/product-services/product-service.store.ts');
    const readme = read('src/app/features/product-services/README.md');

    expect(template).toContain('eyebrow="Product and service catalog"');
    expect(template).toContain('title="Products / Services"');
    expect(template).toContain(
      'Connect each catalog item to a brand so project campaigns inherit the right commercial context.',
    );
    expect(template).toContain('product-create-button');
    expect(template).toContain('product-services-page-grid');
    expect(template).toContain('Catalog roster');
    expect(template).toContain('Catalog detail');
    expect(template).toContain('Relationship rule');
    expect(template).toContain('product-service-skeleton-row');
    expect(template).toContain('Create a brand first before adding products or services.');
    expect(template).toContain('Select a catalog item to see details.');
    expect(source).toContain('void this.store.load(workspaceId)');
    expect(source).toContain('selectedProduct = this.store.selectedProductService');
    expect(styles).toContain('grid-template-columns: minmax(0, 1fr) 23.75rem');
    expect(store).toContain('selectedProductServiceIdSignal');
    expect(store).toContain('selectFirstAvailableProductService');
    expect(store).toContain('catalogRosterSubtitle');
    expect(readme).toContain('roster, detail, and relationship rule cards share one stable grid');
  });

  it('keeps product services free of click refresh hacks and duplicate active product foundations', () => {
    const activeTypescript = activeDayFiles('.ts').map(relativeAppPath);
    const productFiles = activeTypescript.filter((file) => file.startsWith('features/product-services/'));
    const productSources = [
      read('src/app/features/product-services/product-services.ts'),
      read('src/app/features/product-services/product-services.html'),
      read('src/app/features/product-services/product-service.store.ts'),
    ].join('\n');

    expect(productFiles.filter((file) => file.endsWith('product-service.store.ts')).length).toBe(1);
    expect(productFiles.filter((file) => file.endsWith('product-service-api.service.ts')).length).toBe(1);
    expect(productSources).not.toMatch(/setTimeout|dispatchEvent|location\.reload|detectChanges|querySelector/);
    expect(productSources).not.toMatch(/\*ngIf\b|\*ngFor\b/);
  });

  it('keeps dashboard rendering eager and store-driven instead of click-driven', () => {
    const layout = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.html');
    const layoutSource = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.ts');
    const layoutStyles = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.scss');
    const dashboard = read('src/app/features/dashboard/overview/overview.html');
    const source = read('src/app/features/dashboard/overview/overview.ts');
    const store = read('src/app/features/dashboard/dashboard.store.ts');
    const workspaceStore = read('src/app/core/workspace/workspace.store.ts');
    const readme = read('src/app/shared/layouts/dashboard-layout/README.md');

    expect(layout).toContain('<router-outlet />');
    expect(layout).not.toContain('@defer');
    expect(layout).toContain('dashboard-topbar');
    expect(layout).toContain('dashboard-workspace-switcher');
    expect(layout).toContain('data-testid="workspace-switcher"');
    expect(layout).toContain('Loading workspaces');
    expect(layout).toContain('Select workspace');
    expect(layout).toContain('dashboard-profile-slot');
    expect(layoutSource).toContain('effect(() =>');
    expect(layoutSource).toContain('void this.workspace.initialize()');
    expect(layoutStyles).toContain('width: clamp(14rem, 20vw, 20rem)');
    expect(layoutStyles).toContain('width: max-content');
    expect(source).toContain('!this.dashboard.ready()');
    expect(source).toContain('void this.dashboard.load(workspaceId)');
    expect(source).toContain('Create Prompt');
    expect(dashboard).toContain('Hierarchy overview');
    expect(workspaceStore).toContain('Package details unavailable');
    expect(workspaceStore).toContain('Usage details unavailable');
    expect(store).toContain('finally');
    expect(readme).toContain('MASTER users can land on the dashboard before choosing a workspace');
  });

  it('keeps Master sidebar and topbar profile reserved on first shell render', () => {
    const sidebar = read('src/app/shared/layouts/dashboard-layout/components/app-sidebar/app-sidebar.html');
    const sidebarStyles = read('src/app/shared/layouts/dashboard-layout/components/app-sidebar/app-sidebar.scss');
    const navigation = read('src/app/shared/layouts/dashboard-layout/dashboard-navigation.ts');
    const profile = read('src/app/core/layout/user-profile-dropdown/user-profile-dropdown.html');
    const profileSource = read('src/app/core/layout/user-profile-dropdown/user-profile-dropdown.ts');

    expect(navigation).toContain("label: 'System Dashboard'");
    expect(navigation).toContain("roles: ['MASTER']");
    expect(sidebar).toContain('aria-label="Sidebar navigation"');
    expect(sidebar).toContain('aria-label="Access scope"');
    expect(sidebar).toContain('[attr.aria-current]="isActive(item) ?');
    expect(sidebarStyles).toContain('scrollbar-gutter: stable');
    expect(sidebarStyles).toContain('flex: 0 0 auto');
    expect(profile).toContain('data-testid="user-menu-trigger"');
    expect(profile).toContain('aria-label="Open user menu"');
    expect(profile).toContain('<app-avatar [name]="displayName()" [avatarUrl]="avatarUrl()" size="md" />');
    expect(profile).not.toContain('class="hidden min-w-0 text-left sm:block"');
    expect(profile).not.toContain('name="chevron-down"');
    expect(profile).toContain('{{ displayName() }}');
    expect(profile).toContain('{{ displayEmail() }}');
    expect(profileSource).toContain("'Email unavailable'");
  });

  it('does not keep a duplicate dashboard page implementation or click refresh hack', () => {
    const activeTypescript = activeDayFiles('.ts').map(relativeAppPath);
    const activeTemplates = activeDayFiles('.html').map(relativeAppPath);
    const dashboardSources = [
      read('src/app/features/dashboard/overview/overview.ts'),
      read('src/app/features/dashboard/overview/overview.html'),
      read('src/app/shared/layouts/dashboard-layout/dashboard-layout.html'),
    ].join('\n');

    expect(activeTypescript).not.toContain('features/dashboard/dashboard-home/dashboard-home.ts');
    expect(activeTemplates).not.toContain('features/dashboard/dashboard-home/dashboard-home.html');
    expect(dashboardSources).not.toMatch(/setTimeout|dispatchEvent|location\.reload|detectChanges/);
  });

  it('keeps required hierarchy selectors in brand, product, and project forms', () => {
    const brands = read('src/app/features/brands/brands.html');
    const products = read('src/app/features/product-services/product-services.html');
    const projects = read('src/app/features/projects/projects.html');

    expect(brands).toContain('Creative language preference');
    expect(brands).toContain('formControlName="languagePreference"');
    expect(products).toContain('formControlName="brandId"');
    expect(products).toContain('Selected brand context');
    expect(projects).toContain('formControlName="productServiceId"');
    expect(projects).toContain('Derived brand context');
  });

  it('renders friendly loading, empty, error, retry, and validation states on major screens', () => {
    const files = [
      'src/app/features/brands/brands.html',
      'src/app/features/product-services/product-services.html',
      'src/app/features/projects/projects.html',
      'src/app/features/assets/pages/project-assets/project-assets.html',
      'src/app/features/prompts/builder/prompt-builder.html',
      'src/app/features/creative-requests/pages/project-creative-requests/project-creative-requests.html',
      'src/app/features/approvals/pages/approval-queue/approval-queue.html',
      'src/app/features/generated-versions/pages/generated-version-detail/generated-version-detail.html',
    ];

    const combined = files.map(read).join('\n');

    expect(combined).toContain('app-empty-state');
    expect(combined).toMatch(/Loading|loading|animate-pulse|app-loading|app-ai-loading-state/);
    expect(combined).toContain('Retry');
    expect(combined).toMatch(/fieldError|fieldErrors|validationMessage|versionLimitMessage/);
    expect(read('src/app/core/api/http-error.ts')).toContain(
      'You do not have permission to perform this action.',
    );
  });

  it('keeps asset upload, prompt builder, and creative request builders mobile responsive', () => {
    const uploader = read('src/app/features/assets/components/asset-uploader/asset-uploader.html');
    const promptBuilder = read('src/app/features/prompts/builder/prompt-builder.html');
    const creativeRequests = read('src/app/features/creative-requests/pages/project-creative-requests/project-creative-requests.html');

    expect(uploader).toContain('inset-x-2');
    expect(uploader).toContain('overflow-y-auto');
    expect(uploader).toContain('aria-labelledby');
    expect(promptBuilder).toContain('xl:grid-cols');
    expect(promptBuilder).toContain('min-w-0');
    expect(creativeRequests).toContain('xl:grid-cols');
    expect(creativeRequests).toContain('min-w-0');
  });

  it('keeps homepage, dashboard, dialogs, and mobile rosters overflow-safe', () => {
    const home = read('src/app/features/public/home/home.html');
    const dashboardLayout = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.html');
    const dialog = read('src/app/shared/components/app-dialog/app-dialog.ts');
    const drawer = read('src/app/shared/components/app-drawer/app-drawer.ts');
    const brands = read('src/app/features/brands/brands.html');
    const products = read('src/app/features/product-services/product-services.html');
    const projects = read('src/app/features/projects/projects.html');

    expect(home).toContain('overflow-x-hidden');
    expect(dashboardLayout).toContain('overflow-x-hidden');
    expect(dialog).toContain('100dvh');
    expect(drawer).toContain('document:keydown');
    expect(brands).toContain('md:hidden');
    expect(products).toContain('md:hidden');
    expect(projects).toContain('md:hidden');
  });

  it('keeps technical backend errors mapped to friendly copy', () => {
    const httpErrors = read('src/app/core/api/http-error.ts');
    const creativeRequest = read('src/app/features/creative-requests/pages/project-creative-requests/project-creative-requests.ts');

    expect(httpErrors).toContain('You do not have permission to perform this action.');
    expect(httpErrors).toContain('Your workspace storage is full. Please remove old files or upgrade your package.');
    expect(httpErrors).toContain('planfeaturepolicy.maxgeneratedversionsperrequest');
    expect(creativeRequest).toContain('Your current package allows only');
    expect(creativeRequest).toContain('creative version(s) for this request');
  });
});

describe('Batch 12 dynamic policy state', () => {
  function configureWorkspacePolicy(policy: Record<string, unknown>): WorkspaceStore {
    const activeWorkspaceId = signal('workspace-1');
    const currentUser = signal({
      workspace: { id: 'workspace-1' },
      workspaceName: 'Lebhas Workspace',
    });

    TestBed.configureTestingModule({
      providers: [
        WorkspaceStore,
        {
          provide: CurrentUserStore,
          useValue: {
            activeWorkspaceId: activeWorkspaceId.asReadonly(),
            currentUser: currentUser.asReadonly(),
            currentRole: signal('ADMIN').asReadonly(),
            isAuthenticated: signal(true).asReadonly(),
            setActiveWorkspaceId: (workspaceId: string | null) => activeWorkspaceId.set(workspaceId ?? ''),
          },
        },
        {
          provide: WorkspaceApiService,
          useValue: {
            getAccessibleWorkspaces: vi.fn().mockResolvedValue([
              {
                id: 'workspace-1',
                name: 'Lebhas Workspace',
                currentUserRole: 'ADMIN',
                subscription: { planName: 'Dynamic Package', status: 'active' },
                featurePolicy: policy,
                featureToggles: {},
              },
            ]),
            getWorkspaceContext: vi.fn().mockResolvedValue({
              id: 'workspace-1',
              name: 'Lebhas Workspace',
              subscription: { planName: 'Dynamic Package', status: 'active' },
              featurePolicy: policy,
              featureToggles: {},
              usage: {
                creditsRemaining: 12,
                limits: {
                  generatedVersionsPerRequest: {
                    limit: 3,
                    remaining: 3,
                    message: 'Backend-provided version limit',
                  },
                },
              },
            }),
          },
        },
      ],
    });

    return TestBed.inject(WorkspaceStore);
  }

  it('loads workspace subscription and feature policy from backend context', async () => {
    const store = configureWorkspacePolicy({
      approvalAvailable: false,
      shareAvailable: true,
      features: {
        approvals: { enabled: false, reason: 'Backend disabled approvals' },
        sharing: { enabled: true },
      },
      limits: {
        generatedVersionsPerRequest: { limit: 3, remaining: 3 },
      },
    });

    await store.initialize();

    expect(store.activePlanLabel()).toBe('Dynamic Package');
    expect(store.remainingCreditsLabel()).toBe('12');
    expect(store.featurePolicy()?.approvalAvailable).toBe(false);
    expect(store.isFeatureEnabled('approvals')).toBe(false);
    expect(store.isFeatureEnabled('sharing')).toBe(true);
    expect(store.featureLimit('generatedVersionsPerRequest')?.limit).toBe(3);
  });

  it('uses feature policy to block permission checks for generated versions, approvals, and sharing', () => {
    const auth = {
      hasPermission: vi.fn(() => true),
      currentRole: signal('ADMIN').asReadonly(),
      permissions: signal(['PROJECT_VIEW']).asReadonly(),
    };
    const workspace = {
      subscription: signal({ status: 'active' }).asReadonly(),
      featurePolicy: signal({
        approvalAvailable: false,
        shareAvailable: false,
        features: {
          'generatedVersions.share': { enabled: false, reason: 'Backend disabled sharing' },
          approvals: { enabled: false, reason: 'Backend disabled approvals' },
        },
      }).asReadonly(),
      isFeatureEnabled: (featureKey: string) =>
        !['generatedVersions.share', 'approvals'].includes(featureKey),
      featureMessage: (featureKey: string) => `Backend policy disabled ${featureKey}`,
    };

    TestBed.configureTestingModule({
      providers: [
        PermissionStore,
        { provide: CurrentUserStore, useValue: auth },
        { provide: WorkspaceStore, useValue: workspace },
      ],
    });

    const permissions = TestBed.inject(PermissionStore);

    expect(permissions.canUseFeature('generatedVersions.share')).toBe(false);
    expect(permissions.canUseFeature('approvals')).toBe(false);
    expect(permissions.featureDisabledMessage('approvals')).toBe('Backend policy disabled approvals');
  });

  it('keeps crew/admin/master action visibility controlled by role and permissions', () => {
    const source = [
      read('src/app/features/brands/brands.html'),
      read('src/app/features/product-services/product-services.html'),
      read('src/app/features/projects/projects.html'),
      read('src/app/shared/layouts/dashboard-layout/dashboard-layout.ts'),
    ].join('\n');
    const permissionSource = read('src/app/core/permissions/permission.store.ts');

    expect(source).toContain('canManage()');
    expect(source).toContain('canCreate()');
    expect(source).toContain("case 'MASTER'");
    expect(source).toContain("case 'ADMIN'");
    expect(source).toContain("case 'CREW'");
    expect(permissionSource).toContain('canManageBrands');
    expect(permissionSource).toContain('canManageProducts');
    expect(permissionSource).toContain('canCreateProjects');
  });

  it('requests signed generated-version share and download URLs through API services', async () => {
    const api = {
      listByCreativeRequest: vi.fn(),
      get: vi.fn(),
      getShareUrl: vi.fn().mockResolvedValue({ url: 'https://signed-share', expiresAt: '2026-05-26T00:00:00Z' }),
      getDownloadUrl: vi.fn().mockResolvedValue({ url: 'https://signed-download', expiresAt: '2026-05-26T00:00:00Z' }),
    };

    TestBed.configureTestingModule({
      providers: [
        GeneratedVersionStore,
        {
          provide: CurrentUserStore,
          useValue: { activeWorkspaceId: signal('workspace-1').asReadonly() },
        },
        { provide: GeneratedVersionApiService, useValue: api },
        {
          provide: NotificationStateService,
          useValue: { success: vi.fn(), error: vi.fn() },
        },
      ],
    });

    const store = TestBed.inject(GeneratedVersionStore);

    await store.getShareUrl('version-1');
    await store.getDownloadUrl('version-1');

    expect(api.getShareUrl).toHaveBeenCalledWith('workspace-1', 'version-1');
    expect(api.getDownloadUrl).toHaveBeenCalledWith('workspace-1', 'version-1');
  });
});
