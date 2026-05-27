import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  CreditLedger,
  CreditTransactionType,
  DownloadType,
  DownloadUsageLog,
  MasterAiCostUsage,
  MasterWorkspaceUsage,
  MonthlyUsageSnapshot,
  PlanUtilization,
  ShareUsageLog,
  TopCostWorkspace,
  UsageBillingLog,
  UsageType,
  WorkspaceUsageSummary,
} from './models/usage-billing.models';
import { UsageBillingApiService } from './services/usage-billing-api.service';
import { UsageBillingStore } from './state/usage-billing.store';

const featureRoot = join(process.cwd(), 'src/app/features/usage-billing');
const usageGuardPath = 'src/app/core/guards/usage-billing-access.guard.ts';
const dashboardLayoutPath = 'src/app/shared/layouts/dashboard-layout/dashboard-layout.ts';

function read(relativePath: string): string {
  return readFileSync(join(process.cwd(), relativePath), 'utf8');
}

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

function relativeFeaturePath(filePath: string): string {
  return relative(featureRoot, filePath).replaceAll('\\', '/');
}

const usageSummary: WorkspaceUsageSummary = {
  id: 'usage-summary-1',
  workspaceId: 'workspace-admin',
  usageMonth: '2026-05',
  usedCredits: 80,
  reservedCredits: 12,
  refundedCredits: 4,
  totalCreativeRequests: 9,
  totalGeneratedVersions: 24,
  totalLayerExecutions: 140,
  totalAiCostUsd: 22.5,
  totalUploads: 16,
  totalStorageBytes: 512,
  totalDownloads: 18,
  totalPublicShares: 7,
  totalPromptEnhancements: 11,
  totalGenerationFailures: 2,
  totalApiCalls: 230,
  updatedAt: '2026-05-26T08:00:00Z',
  creditLimit: {
    used: 80,
    limit: 100,
    remaining: 20,
    label: 'Backend credit package',
  },
  storageLimit: {
    used: 512,
    limit: 1024,
    remaining: 512,
    label: 'Backend storage package',
  },
  generatedVersionLimit: {
    used: 24,
    limit: 60,
    remaining: 36,
    label: 'Backend generated version package',
  },
};

const creditLedger: readonly CreditLedger[] = [
  {
    id: 'ledger-refund',
    workspaceId: 'workspace-admin',
    creativeRequestId: 'creative-request-2',
    generatedVersionId: 'generated-version-2',
    generationJobId: 'job-2',
    transactionType: CreditTransactionType.Refund,
    creditsAmount: 3,
    balanceBeforeTransaction: 77,
    balanceAfterTransaction: 80,
    referenceType: 'CREATIVE_REQUEST',
    referenceId: 'creative-request-2',
    description: 'Credits returned after failed generation.',
    createdBy: 'admin-user',
    createdAt: '2026-05-26T10:00:00Z',
  },
  {
    id: 'ledger-finalize',
    workspaceId: 'workspace-admin',
    creativeRequestId: 'creative-request-1',
    generatedVersionId: 'generated-version-1',
    generationJobId: 'job-1',
    transactionType: CreditTransactionType.Finalize,
    creditsAmount: -8,
    balanceBeforeTransaction: 88,
    balanceAfterTransaction: 80,
    referenceType: 'CREATIVE_REQUEST',
    referenceId: 'creative-request-1',
    description: 'Credits were used after successful generation.',
    createdBy: 'admin-user',
    createdAt: '2026-05-26T09:00:00Z',
  },
  {
    id: 'ledger-reserve',
    workspaceId: 'workspace-admin',
    creativeRequestId: 'creative-request-1',
    generatedVersionId: null,
    generationJobId: 'job-1',
    transactionType: CreditTransactionType.Reserve,
    creditsAmount: -8,
    balanceBeforeTransaction: 96,
    balanceAfterTransaction: 88,
    referenceType: 'CREATIVE_REQUEST',
    referenceId: 'creative-request-1',
    description: 'Credits are temporarily held for generation.',
    createdBy: 'admin-user',
    createdAt: '2026-05-26T08:30:00Z',
  },
];

const usageLogs: readonly UsageBillingLog[] = [
  {
    id: 'usage-log-1',
    workspaceId: 'workspace-admin',
    usageType: UsageType.CreativeGeneration,
    referenceType: 'CREATIVE_REQUEST',
    referenceId: 'creative-request-1',
    creditsCharged: 8,
    estimatedCostUsd: 1.25,
    pricingPlanId: 'pricing-plan-backend',
    pricingPlanName: 'Backend Dynamic Package',
    planFeaturePolicyId: 'policy-backend',
    createdAt: '2026-05-26T09:15:00Z',
  },
];

const downloadUsage: readonly DownloadUsageLog[] = [
  {
    id: 'download-1',
    workspaceId: 'workspace-admin',
    generatedVersionId: 'generated-version-1',
    assetId: 'asset-1',
    downloadedBy: 'admin-user',
    downloadedByName: 'Workspace Admin',
    downloadType: DownloadType.Final,
    ipAddress: '203.0.113.10',
    userAgent: 'Mozilla/5.0 Test Browser',
    createdAt: '2026-05-26T10:20:00Z',
  },
];

const shareUsage: readonly ShareUsageLog[] = [
  {
    id: 'share-1',
    workspaceId: 'workspace-admin',
    shareLinkId: 'share-link-1',
    generatedVersionId: 'generated-version-1',
    accessedByUserId: 'reviewer-1',
    accessedByName: 'Reviewer',
    accessIp: '203.0.113.11',
    userAgent: 'Mozilla/5.0 Test Browser',
    referrer: 'https://example.test',
    createdAt: '2026-05-26T10:30:00Z',
  },
];

const monthlySnapshots: readonly MonthlyUsageSnapshot[] = [
  {
    id: 'snapshot-1',
    workspaceId: 'workspace-admin',
    usageMonth: '2026-05',
    pricingPlanId: 'pricing-plan-backend',
    pricingPlanName: 'Backend Dynamic Package',
    subscriptionId: 'subscription-1',
    usedCredits: 80,
    generatedVersions: 24,
    creativeRequests: 9,
    aiCostUsd: 22.5,
    storageBytes: 512,
    downloads: 18,
    publicShares: 7,
    createdAt: '2026-05-26T10:45:00Z',
  },
];

const masterWorkspaceUsage: readonly MasterWorkspaceUsage[] = [
  {
    workspaceId: 'workspace-admin',
    workspaceName: 'Backend Workspace',
    currentPlanName: 'Backend Dynamic Package',
    currentMonthUsage: usageSummary,
    latestSnapshot: monthlySnapshots[0],
    updatedAt: '2026-05-26T11:00:00Z',
  },
];

const masterAiCosts: readonly MasterAiCostUsage[] = [
  {
    usageMonth: '2026-05',
    totalAiCostUsd: 22.5,
    totalLayerExecutions: 140,
    totalGeneratedVersions: 24,
    totalGenerationFailures: 2,
    updatedAt: '2026-05-26T11:00:00Z',
  },
];

const topCostWorkspaces: readonly TopCostWorkspace[] = [
  {
    workspaceId: 'workspace-admin',
    workspaceName: 'Backend Workspace',
    pricingPlanName: 'Backend Dynamic Package',
    usageMonth: '2026-05',
    totalAiCostUsd: 22.5,
    usedCredits: 80,
    generatedVersions: 24,
    highCostWarning: false,
  },
];

const planUtilization: readonly PlanUtilization[] = [
  {
    pricingPlanId: 'pricing-plan-backend',
    pricingPlanName: 'Backend Dynamic Package',
    workspaceCount: 3,
    totalUsedCredits: 240,
    totalGeneratedVersions: 72,
    totalAiCostUsd: 67.5,
    averageCreditUtilizationPercent: 80,
    averageStorageUtilizationPercent: 50,
    updatedAt: '2026-05-26T11:00:00Z',
  },
];

function configureStore(allowed = true) {
  const permission = signal(allowed).asReadonly();
  const api = {
    getUsageSummary: vi.fn().mockResolvedValue(usageSummary),
    getCurrentMonthUsage: vi.fn().mockResolvedValue(usageSummary),
    getCreditLedger: vi.fn().mockResolvedValue(creditLedger),
    getUsageBillingLogs: vi.fn().mockResolvedValue(usageLogs),
    getDownloadUsage: vi.fn().mockResolvedValue(downloadUsage),
    getShareUsage: vi.fn().mockResolvedValue(shareUsage),
    getMonthlyUsageSnapshots: vi.fn().mockResolvedValue(monthlySnapshots),
    getMasterWorkspaceUsage: vi.fn().mockResolvedValue(masterWorkspaceUsage),
    getMasterWorkspaceUsageDetail: vi.fn().mockResolvedValue(masterWorkspaceUsage[0]),
    getMasterAiCosts: vi.fn().mockResolvedValue(masterAiCosts),
    getMasterTopCostWorkspaces: vi.fn().mockResolvedValue(topCostWorkspaces),
    getMasterPlanUtilization: vi.fn().mockResolvedValue(planUtilization),
  };

  TestBed.configureTestingModule({
    providers: [
      UsageBillingStore,
      { provide: UsageBillingApiService, useValue: api },
      {
        provide: PermissionStore,
        useValue: {
          canViewUsageBilling: permission,
          canViewCreditLedger: permission,
          canViewDownloadUsage: permission,
          canViewShareUsage: permission,
          canViewMasterUsage: permission,
          canViewPlanUtilization: permission,
        },
      },
      { provide: NotificationStateService, useValue: { error: vi.fn() } },
    ],
  });

  return { store: TestBed.inject(UsageBillingStore), api };
}

describe('Day 8 Usage & Billing store behavior', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads the workspace usage dashboard for ADMIN permissions', async () => {
    const { store, api } = configureStore();

    await store.loadDashboard('workspace-admin');

    expect(api.getUsageSummary).toHaveBeenCalledWith('workspace-admin');
    expect(api.getCurrentMonthUsage).toHaveBeenCalledWith('workspace-admin');
    expect(store.currentMonthUsage()?.totalCreativeRequests).toBe(9);
    expect(store.availableCredits()).toBe(20);
  });

  it('blocks the workspace usage dashboard for CREW without permission', async () => {
    const { store, api } = configureStore(false);

    const result = await store.loadDashboard('workspace-admin');

    expect(result.ok).toBe(false);
    expect(api.getUsageSummary).not.toHaveBeenCalled();
    expect(store.error()).toBe('You do not have permission to view usage and billing data.');
  });

  it('loads the Master usage overview for MASTER permissions', async () => {
    const { store, api } = configureStore();

    await store.loadMasterUsageOverview();

    expect(api.getMasterWorkspaceUsage).toHaveBeenCalled();
    expect(api.getMasterAiCosts).toHaveBeenCalled();
    expect(api.getMasterTopCostWorkspaces).toHaveBeenCalled();
    expect(api.getMasterPlanUtilization).toHaveBeenCalled();
    expect(store.masterWorkspaceUsage()[0]?.workspaceName).toBe('Backend Workspace');
  });

  it('computes generated version and storage usage percent from backend limits', async () => {
    const { store } = configureStore();

    await store.loadDashboard('workspace-admin');

    expect(store.generatedVersionUsagePercent()).toBe(40);
    expect(store.storageUsagePercent()).toBe(50);
    expect(store.creditUsagePercent()).toBe(80);
    expect(store.isCreditLimitNear()).toBe(true);
  });

  it('loads all workspace usage history collections', async () => {
    const { store } = configureStore();

    await store.loadCreditLedger('workspace-admin');
    await store.loadUsageBillingLogs('workspace-admin');
    await store.loadDownloadUsage('workspace-admin');
    await store.loadShareUsage('workspace-admin');
    await store.loadMonthlySnapshots('workspace-admin');

    expect(store.creditLedger()).toHaveLength(3);
    expect(store.usageBillingLogs()[0]?.pricingPlanName).toBe('Backend Dynamic Package');
    expect(store.downloadUsage()[0]?.downloadType).toBe(DownloadType.Final);
    expect(store.shareUsage()[0]?.shareLinkId).toBe('share-link-1');
    expect(store.monthlySnapshots()[0]?.usageMonth).toBe('2026-05');
  });

  it('updates month, usage type, and transaction type filters', async () => {
    const { store, api } = configureStore();

    store.setSelectedMonth('2026-05');
    store.setSelectedUsageType(UsageType.Download);
    store.setSelectedTransactionType(CreditTransactionType.Refund);
    await store.loadCreditLedger('workspace-admin');

    expect(store.selectedMonth()).toBe('2026-05');
    expect(store.selectedUsageType()).toBe(UsageType.Download);
    expect(store.selectedTransactionType()).toBe(CreditTransactionType.Refund);
    expect(api.getCreditLedger).toHaveBeenCalledWith('workspace-admin', {
      month: '2026-05',
      usageType: UsageType.Download,
      transactionType: CreditTransactionType.Refund,
    });
  });
});

describe('Day 8 Usage & Billing UI coverage', () => {
  it('renders the credit balance card, reserved credits, and refunded credits', () => {
    const component = read(
      'src/app/features/usage-billing/components/credit-balance-card/credit-balance-card.html',
    );

    expect(component).toContain('Available credits');
    expect(component).toContain('Reserved credits');
    expect(component).toContain('Refunded credits');
    expect(component).toContain('Credits temporarily held while creative generation is running.');
    expect(component).toContain('Credits returned after failed generation.');
  });

  it('renders credit ledger entries, transaction badges, and lifecycle timeline', () => {
    const ledgerPage = read('src/app/features/usage-billing/credit-ledger/credit-ledger.html');
    const ledgerCard = read(
      'src/app/features/usage-billing/components/credit-ledger-card/credit-ledger-card.html',
    );
    const ledgerCardTs = read(
      'src/app/features/usage-billing/components/credit-ledger-card/credit-ledger-card.ts',
    );
    const timeline = read(
      'src/app/features/usage-billing/components/credit-lifecycle-timeline/credit-lifecycle-timeline.html',
    );
    const timelineTs = read(
      'src/app/features/usage-billing/components/credit-lifecycle-timeline/credit-lifecycle-timeline.ts',
    );

    expect(ledgerPage).toContain('Credit history');
    expect(ledgerPage).toContain('app-credit-ledger-card');
    expect(ledgerCard).toContain('transactionLabel()');
    expect(ledgerCardTs).toContain('Credits are temporarily held for generation.');
    expect(ledgerCardTs).toContain('Credits were used after successful generation.');
    expect(ledgerCardTs).toContain('Credits returned after failed generation.');
    expect(timeline).toContain('step.title');
    expect(timelineTs).toContain('Reserve Credits');
    expect(timelineTs).toContain('Generate Creative');
    expect(timelineTs).toContain('Finalize on Success');
    expect(timelineTs).toContain('Refund on Failure');
  });

  it('renders current month usage summary and backend package names', () => {
    const dashboard = read('src/app/features/usage-billing/dashboard/usage-billing-dashboard.html');
    const summaryCard = read(
      'src/app/features/usage-billing/components/usage-summary-card/usage-summary-card.html',
    );
    const planCard = read(
      'src/app/features/usage-billing/components/plan-utilization-card/plan-utilization-card.html',
    );

    expect(dashboard).toContain('Current month usage');
    expect(dashboard).toContain('app-usage-summary-card');
    expect(summaryCard).toContain('label()');
    expect(planCard).toContain('packageName()');
    expect(dashboard).toContain('You are close to your current package limit.');
    expect(dashboard).toContain('Your monthly credit limit has been reached.');
  });

  it('renders usage logs, download logs, share logs, and monthly snapshots', () => {
    const usageLogsPage = read('src/app/features/usage-billing/usage-logs/usage-billing-logs.html');
    const downloadPage = read('src/app/features/usage-billing/download-usage/download-usage.html');
    const sharePage = read('src/app/features/usage-billing/share-usage/share-usage.html');
    const snapshotsPage = read(
      'src/app/features/usage-billing/monthly-snapshots/monthly-usage-snapshots.html',
    );

    expect(usageLogsPage).toContain('Usage history');
    expect(usageLogsPage).toContain('app-usage-log-card');
    expect(downloadPage).toContain('Download usage');
    expect(downloadPage).toContain('app-download-usage-card');
    expect(sharePage).toContain('Share usage');
    expect(sharePage).toContain('app-share-usage-card');
    expect(snapshotsPage).toContain('Monthly usage snapshots');
    expect(snapshotsPage).toContain('app-monthly-snapshot-card');
  });

  it('renders loading, empty, and retryable error states', () => {
    const combined = [
      'src/app/features/usage-billing/dashboard/usage-billing-dashboard.html',
      'src/app/features/usage-billing/credit-ledger/credit-ledger.html',
      'src/app/features/usage-billing/usage-logs/usage-billing-logs.html',
      'src/app/features/usage-billing/download-usage/download-usage.html',
      'src/app/features/usage-billing/share-usage/share-usage.html',
      'src/app/features/usage-billing/monthly-snapshots/monthly-usage-snapshots.html',
      'src/app/features/usage-billing/master-usage/master-usage-overview.html',
    ]
      .map(read)
      .join('\n');

    expect(combined).toContain('app-usage-loading-state');
    expect(combined).toContain('app-usage-empty-state');
    expect(combined).toContain('app-error-state');
    expect(combined).toContain('Try again');
  });

  it('keeps mobile layouts overflow-safe with responsive cards and tables', () => {
    const combined = [
      'src/app/features/usage-billing/credit-ledger/credit-ledger.html',
      'src/app/features/usage-billing/usage-logs/usage-billing-logs.html',
      'src/app/features/usage-billing/download-usage/download-usage.html',
      'src/app/features/usage-billing/share-usage/share-usage.html',
      'src/app/features/usage-billing/monthly-snapshots/monthly-usage-snapshots.html',
      'src/app/features/usage-billing/master-usage/master-usage-overview.html',
      'src/app/features/usage-billing/components/usage-filter-bar/usage-filter-bar.html',
    ]
      .map(read)
      .join('\n');

    expect(combined).toContain('lg:hidden');
    expect(combined).toContain('class="hidden lg:block"');
    expect(combined).toContain('overflow-x-auto');
    expect(combined).toContain('min-w-0');
    expect(combined).not.toContain('w-screen');
  });
});

describe('Day 8 Usage & Billing routes and navigation', () => {
  it('registers all workspace and Master usage routes', () => {
    const routes = read('src/app/features/usage-billing/usage-billing.routes.ts');

    for (const route of [
      "path: 'usage-billing'",
      "path: 'usage-billing/credits'",
      "path: 'usage-billing/logs'",
      "path: 'usage-billing/downloads'",
      "path: 'usage-billing/shares'",
      "path: 'usage-billing/monthly-snapshots'",
      "path: 'master/usage'",
      "path: 'master/usage/workspaces'",
      "path: 'master/usage/ai-costs'",
      "path: 'master/usage/plan-utilization'",
    ]) {
      expect(routes).toContain(route);
    }

    expect(routes).toContain('creditLedgerAccessGuard');
    expect(routes).toContain('downloadUsageAccessGuard');
    expect(routes).toContain('shareUsageAccessGuard');
    expect(routes).toContain('masterUsageAccessGuard');
    expect(routes).toContain('planUtilizationAccessGuard');
  });

  it('shows Usage & Billing navigation only through permission helpers', () => {
    const layout = read(dashboardLayoutPath);
    const guard = read(usageGuardPath);

    expect(layout).toContain('canViewUsageBilling');
    expect(layout).toContain('canViewCreditLedger');
    expect(layout).toContain('canViewDownloadUsage');
    expect(layout).toContain('canViewShareUsage');
    expect(layout).toContain('canViewMasterUsage');
    expect(layout).toContain('canViewPlanUtilization');
    expect(guard).toContain("router.createUrlTree(['/usage-billing'])");
    expect(guard).toContain("router.createUrlTree(['/master/usage'])");
    expect(read('src/app/features/usage-billing/dashboard/usage-billing-dashboard.html')).toContain(
      'Please contact your workspace admin if you need access.',
    );
  });
});

describe('Day 8 static verification guardrails', () => {
  it('has all required shared Usage & Billing components', () => {
    for (const component of [
      'credit-balance-card',
      'usage-summary-card',
      'plan-utilization-card',
      'credit-ledger-card',
      'usage-log-card',
      'download-usage-card',
      'share-usage-card',
      'monthly-snapshot-card',
      'usage-progress-bar',
      'credit-lifecycle-timeline',
      'usage-filter-bar',
      'usage-empty-state',
      'usage-loading-state',
    ]) {
      expect(
        statSync(join(featureRoot, 'components', component, `${component}.ts`)).isFile(),
      ).toBe(true);
      expect(
        statSync(join(featureRoot, 'components', component, `${component}.html`)).isFile(),
      ).toBe(true);
      expect(
        statSync(join(featureRoot, 'components', component, `${component}.scss`)).isFile(),
      ).toBe(true);
    }
  });

  it('does not use hardcoded plan names in production Usage & Billing files', () => {
    const offenders = collectFiles(featureRoot, (filePath) => {
      const isProductionFile = /\.(ts|html)$/.test(filePath) && !filePath.endsWith('.spec.ts');
      return isProductionFile && /\b(Free|Basic|Pro|Enterprise)\b/.test(readFileSync(filePath, 'utf8'));
    }).map(relativeFeaturePath);

    expect(offenders).toEqual([]);
  });

  it('keeps Day 8 templates on Angular block syntax only', () => {
    const offenders = collectFiles(featureRoot, (filePath) => filePath.endsWith('.html'))
      .filter((filePath) => /\*ngIf\b|\*ngFor\b/.test(readFileSync(filePath, 'utf8')))
      .map(relativeFeaturePath);

    expect(offenders).toEqual([]);
  });

  it('keeps Day 8 files free of NgModules, inline templates, CSS files, and component naming', () => {
    const files = collectFiles(featureRoot, () => true);
    const cssFiles = files.filter((filePath) => filePath.endsWith('.css')).map(relativeFeaturePath);
    const componentNamedFiles = files
      .filter((filePath) => /\.component\./.test(filePath))
      .map(relativeFeaturePath);
    const sourceOffenders = files
      .filter((filePath) => filePath.endsWith('.ts') && !filePath.endsWith('.spec.ts'))
      .filter((filePath) => /@NgModule\b|template\s*:`|template\s*: '/.test(readFileSync(filePath, 'utf8')))
      .map(relativeFeaturePath);

    expect(cssFiles).toEqual([]);
    expect(componentNamedFiles).toEqual([]);
    expect(sourceOffenders).toEqual([]);
  });

  it('does not add payment, purchase, invoice, tax, accounting, fake billing, or Day 9 production UI', () => {
    const forbidden =
      /\b(checkout|payment|subscription purchase|credit purchase|invoice|tax|VAT|accounting|Day 9|fake billing|fake data)\b/i;
    const offenders = collectFiles(featureRoot, (filePath) => {
      const isProductionFile = /\.(ts|html)$/.test(filePath) && !filePath.endsWith('.spec.ts');
      return isProductionFile && forbidden.test(readFileSync(filePath, 'utf8'));
    }).map(relativeFeaturePath);

    expect(offenders).toEqual([]);
  });

  it('uses the standard ApiResponse<T> handling in the Usage Billing API service', () => {
    const service = read('src/app/features/usage-billing/services/usage-billing-api.service.ts');
    const coreApi = read('src/app/core/api/api.service.ts');

    expect(service).toContain('unwrapApiResponse');
    expect(service).toContain('this.api.get<T>(path');
    expect(coreApi).toContain('ApiResponse<T>');
  });
});
