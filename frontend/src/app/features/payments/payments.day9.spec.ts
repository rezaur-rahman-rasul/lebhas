import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';

import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import {
  creditPackageManagementAccessGuard,
  creditPurchaseAccessGuard,
  paymentProviderManagementAccessGuard,
} from '@app/core/guards/payment-access.guard';
import { PermissionStore } from '@app/core/permissions/permission.store';
import { NotificationStateService } from '@app/core/state/notification-state.service';
import {
  BillingCycle,
  CreditPackage,
  EnvironmentType,
  Invoice,
  InvoiceType,
  PaymentProvider,
  PaymentProviderConfiguration,
  PaymentProviderType,
  PaymentPurpose,
  PaymentSessionResponse,
  PaymentStatus,
  PaymentTransaction,
  PricingPlanForPurchase,
} from './models/payment.models';
import { PaymentApiService } from './services/payment-api.service';
import { PaymentStore } from './state/payment.store';

const featureRoot = join(process.cwd(), 'src/app/features/payments');

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

const paymentProviders: readonly PaymentProvider[] = [
  {
    id: 'provider-enabled',
    name: 'Mock Enabled Provider',
    code: 'mock-enabled',
    providerType: PaymentProviderType.Manual,
    isEnabled: true,
    sandboxEnabled: true,
    liveEnabled: false,
    priority: 1,
    createdAt: '2026-05-26T08:00:00Z',
    updatedAt: '2026-05-26T09:00:00Z',
  },
  {
    id: 'provider-disabled',
    name: 'Mock Disabled Provider',
    code: 'mock-disabled',
    providerType: PaymentProviderType.Stripe,
    isEnabled: false,
    sandboxEnabled: false,
    liveEnabled: true,
    priority: 2,
    createdAt: '2026-05-26T08:00:00Z',
    updatedAt: '2026-05-26T09:00:00Z',
  },
];

const providerConfiguration: PaymentProviderConfiguration = {
  id: 'configuration-1',
  providerId: 'provider-enabled',
  providerName: 'Mock Enabled Provider',
  environmentType: EnvironmentType.Sandbox,
  apiBaseUrl: 'https://payments.example.test',
  merchantId: 'merchant-1',
  successUrl: 'https://app.example.test/success',
  failureUrl: 'https://app.example.test/failure',
  cancelUrl: 'https://app.example.test/cancel',
  isActive: true,
  createdAt: '2026-05-26T08:00:00Z',
  updatedAt: '2026-05-26T09:00:00Z',
};

const creditPackages: readonly CreditPackage[] = [
  {
    id: 'credit-package-1',
    name: 'Mock Credit Package',
    code: 'mock-credit-package',
    credits: 500,
    bonusCredits: 50,
    price: 25,
    currency: 'USD',
    isActive: true,
    sortOrder: 2,
    createdAt: '2026-05-26T08:00:00Z',
    updatedAt: '2026-05-26T09:00:00Z',
  },
  {
    id: 'credit-package-2',
    name: 'Mock Priority Credit Package',
    code: 'mock-priority-credit-package',
    credits: 1000,
    bonusCredits: 150,
    price: 45,
    currency: 'USD',
    isActive: true,
    sortOrder: 1,
    createdAt: '2026-05-26T08:00:00Z',
    updatedAt: '2026-05-26T09:00:00Z',
  },
];

const pricingPlans: readonly PricingPlanForPurchase[] = [
  {
    id: 'pricing-plan-1',
    name: 'Mock Backend Package',
    code: 'mock-backend-package',
    billingCycle: BillingCycle.Monthly,
    price: 35,
    currency: 'USD',
    description: 'Mock package returned by backend.',
    features: { approvals: true, sharing: true },
    limits: { credits: 1000, storage: '10 GB' },
    isActive: true,
    sortOrder: 1,
  },
];

const paymentSession: PaymentSessionResponse = {
  paymentTransactionId: 'transaction-session-1',
  paymentStatus: PaymentStatus.Pending,
  providerName: 'Mock Enabled Provider',
  providerSessionId: 'provider-session-1',
  paymentRedirectUrl: 'https://payments.example.test/session/provider-session-1',
  expiresAt: '2026-05-26T10:00:00Z',
};

const paymentTransactions: readonly PaymentTransaction[] = [
  {
    id: 'transaction-success',
    workspaceId: 'workspace-admin',
    userId: 'admin-user',
    providerId: 'provider-enabled',
    providerName: 'Mock Enabled Provider',
    paymentPurpose: PaymentPurpose.SubscriptionPurchase,
    referenceType: 'SUBSCRIPTION_ORDER',
    referenceId: 'subscription-order-1',
    amount: 35,
    currency: 'USD',
    providerTransactionId: 'provider-transaction-secret-123456',
    providerSessionId: 'provider-session-1',
    status: PaymentStatus.Success,
    failureReason: null,
    initiatedAt: '2026-05-26T08:00:00Z',
    completedAt: '2026-05-26T08:10:00Z',
    cancelledAt: null,
    createdAt: '2026-05-26T08:00:00Z',
    updatedAt: '2026-05-26T08:10:00Z',
  },
  {
    id: 'transaction-failed',
    workspaceId: 'workspace-admin',
    userId: 'admin-user',
    providerId: 'provider-enabled',
    providerName: 'Mock Enabled Provider',
    paymentPurpose: PaymentPurpose.CreditPurchase,
    referenceType: 'CREDIT_PURCHASE_ORDER',
    referenceId: 'credit-order-1',
    amount: 25,
    currency: 'USD',
    providerTransactionId: 'provider-transaction-failed-123456',
    providerSessionId: 'provider-session-2',
    status: PaymentStatus.Failed,
    failureReason: 'Payment failed. Please try again or use another available provider.',
    initiatedAt: '2026-05-26T09:00:00Z',
    completedAt: null,
    cancelledAt: null,
    createdAt: '2026-05-26T09:00:00Z',
    updatedAt: '2026-05-26T09:10:00Z',
  },
];

const invoices: readonly Invoice[] = [
  {
    id: 'invoice-1',
    workspaceId: 'workspace-admin',
    paymentTransactionId: 'transaction-success',
    invoiceNumber: 'INV-MOCK-001',
    invoiceType: InvoiceType.Subscription,
    amount: 35,
    currency: 'USD',
    status: PaymentStatus.Success,
    issuedAt: '2026-05-26T08:11:00Z',
    paidAt: '2026-05-26T08:12:00Z',
    createdAt: '2026-05-26T08:11:00Z',
  },
];

function configurePaymentStore(options: {
  readonly allow?: boolean;
  readonly canPurchaseCredits?: boolean;
  readonly canPurchaseSubscription?: boolean;
} = {}) {
  const allow = signal(options.allow ?? true).asReadonly();
  const canPurchaseCredits = signal(options.canPurchaseCredits ?? options.allow ?? true).asReadonly();
  const canPurchaseSubscription = signal(
    options.canPurchaseSubscription ?? options.allow ?? true,
  ).asReadonly();
  const api = {
    getPaymentProviders: vi.fn().mockResolvedValue(paymentProviders),
    createPaymentProvider: vi.fn().mockResolvedValue(paymentProviders[0]),
    updatePaymentProvider: vi.fn().mockResolvedValue(paymentProviders[1]),
    createProviderConfiguration: vi.fn().mockResolvedValue(providerConfiguration),
    updateProviderConfiguration: vi.fn().mockResolvedValue(providerConfiguration),
    getCreditPackages: vi.fn().mockResolvedValue(creditPackages),
    createCreditPackage: vi.fn().mockResolvedValue(creditPackages[0]),
    updateCreditPackage: vi.fn().mockResolvedValue(creditPackages[0]),
    purchaseSubscription: vi.fn().mockResolvedValue(paymentSession),
    upgradeSubscription: vi.fn().mockResolvedValue(paymentSession),
    renewSubscription: vi.fn().mockResolvedValue(paymentSession),
    purchaseCredits: vi.fn().mockResolvedValue(paymentSession),
    getWorkspacePayments: vi.fn().mockResolvedValue(paymentTransactions),
    getWorkspacePaymentDetail: vi.fn().mockResolvedValue(paymentTransactions[0]),
    getWorkspaceInvoices: vi.fn().mockResolvedValue(invoices),
  };

  TestBed.configureTestingModule({
    providers: [
      PaymentStore,
      { provide: PaymentApiService, useValue: api },
      {
        provide: PermissionStore,
        useValue: {
          canViewPayments: allow,
          canViewInvoices: allow,
          canManagePaymentProviders: allow,
          canManageCreditPackages: allow,
          canPurchaseSubscription,
          canPurchaseCredits,
        },
      },
      { provide: NotificationStateService, useValue: { error: vi.fn() } },
    ],
  });

  return { store: TestBed.inject(PaymentStore), api };
}

describe('Day 9 payment store behavior', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('loads Master payment providers and computes provider status groups', async () => {
    const { store, api } = configurePaymentStore();

    await store.loadPaymentProviders();

    expect(api.getPaymentProviders).toHaveBeenCalled();
    expect(store.paymentProviders()).toHaveLength(2);
    expect(store.activeProviders()[0]?.id).toBe('provider-enabled');
    expect(store.disabledProviders()[0]?.id).toBe('provider-disabled');
    expect(store.sandboxProviders()[0]?.id).toBe('provider-enabled');
    expect(store.liveProviders()[0]?.id).toBe('provider-disabled');
  });

  it('blocks provider setup for non-MASTER style permissions', async () => {
    const { store, api } = configurePaymentStore({ allow: false });

    const result = await store.createPaymentProvider({
      name: 'Denied Provider',
      code: 'denied-provider',
      providerType: PaymentProviderType.Manual,
      isEnabled: true,
      sandboxEnabled: true,
      liveEnabled: false,
      priority: 1,
    });

    expect(result.ok).toBe(false);
    expect(api.createPaymentProvider).not.toHaveBeenCalled();
    expect(store.error()).toBe('You do not have permission to access payment information.');
  });

  it('renders dynamic credit packages and selected credit package summary', async () => {
    const { store, api } = configurePaymentStore();

    await store.loadCreditPackages();
    store.setSelectedCreditPackage(creditPackages[0]);

    expect(api.getCreditPackages).toHaveBeenCalled();
    expect(store.activeCreditPackages()[0]?.id).toBe('credit-package-2');
    expect(store.selectedCreditPurchaseSummary()).toEqual({
      creditPackageId: 'credit-package-1',
      packageName: 'Mock Credit Package',
      credits: 500,
      bonusCredits: 50,
      amount: 25,
      currency: 'USD',
    });
  });

  it('updates selected billing cycle and selected pricing package summary', () => {
    const { store } = configurePaymentStore();

    store.setSelectedPricingPlan(pricingPlans[0]);
    store.setSelectedBillingCycle(BillingCycle.Yearly);

    expect(store.selectedBillingCycle()).toBe(BillingCycle.Yearly);
    expect(store.selectedSubscriptionSummary()).toEqual({
      pricingPlanId: 'pricing-plan-1',
      packageName: 'Mock Backend Package',
      billingCycle: BillingCycle.Yearly,
      amount: 35,
      currency: 'USD',
    });
  });

  it('calls backend APIs for subscription purchase and stores redirect session from backend', async () => {
    const { store, api } = configurePaymentStore();

    const result = await store.purchaseSubscription('workspace-admin', {
      pricingPlanId: 'pricing-plan-1',
      billingCycle: BillingCycle.Monthly,
      returnUrl: 'https://app.example.test/payments/subscription',
    });

    expect(result.ok).toBe(true);
    expect(api.purchaseSubscription).toHaveBeenCalledWith('workspace-admin', {
      pricingPlanId: 'pricing-plan-1',
      billingCycle: BillingCycle.Monthly,
      returnUrl: 'https://app.example.test/payments/subscription',
    });
    expect(store.activePaymentSession()?.paymentRedirectUrl).toBe(
      'https://payments.example.test/session/provider-session-1',
    );
  });

  it('calls backend APIs for credit purchase and blocks CREW without permission', async () => {
    const allowed = configurePaymentStore();

    await allowed.store.purchaseCredits('workspace-admin', {
      creditPackageId: 'credit-package-1',
      returnUrl: 'https://app.example.test/payments/credits',
    });

    expect(allowed.api.purchaseCredits).toHaveBeenCalledWith('workspace-admin', {
      creditPackageId: 'credit-package-1',
      returnUrl: 'https://app.example.test/payments/credits',
    });
    expect(allowed.store.activePaymentSession()?.paymentRedirectUrl).toBe(
      'https://payments.example.test/session/provider-session-1',
    );

    TestBed.resetTestingModule();
    const denied = configurePaymentStore({ allow: true, canPurchaseCredits: false });
    const result = await denied.store.purchaseCredits('workspace-admin', {
      creditPackageId: 'credit-package-1',
    });

    expect(result.ok).toBe(false);
    expect(denied.api.purchaseCredits).not.toHaveBeenCalled();
  });

  it('loads payment transactions and invoices from backend', async () => {
    const { store, api } = configurePaymentStore();

    await store.loadWorkspacePayments('workspace-admin');
    await store.loadWorkspaceInvoices('workspace-admin');

    expect(api.getWorkspacePayments).toHaveBeenCalledWith('workspace-admin', undefined);
    expect(api.getWorkspaceInvoices).toHaveBeenCalledWith('workspace-admin');
    expect(store.paymentTransactions()).toHaveLength(2);
    expect(store.successfulPayments()[0]?.status).toBe(PaymentStatus.Success);
    expect(store.failedPayments()[0]?.failureReason).toContain('Payment failed');
    expect(store.invoices()[0]?.invoiceNumber).toBe('INV-MOCK-001');
  });
});

describe('Day 9 payment UI coverage', () => {
  it('loads Master provider setup and displays provider enable/disable, sandbox, and live status', () => {
    const page = read('src/app/features/payments/providers/payment-providers.html');
    const card = read('src/app/features/payments/components/payment-provider-card/payment-provider-card.html');

    expect(page).toContain('Payment providers');
    expect(page).toContain('app-payment-provider-card');
    expect(page).toContain('permissions.canManagePaymentProviders()');
    expect(card).toContain('statusLabel()');
    expect(card).toContain('provider().sandboxEnabled');
    expect(card).toContain('provider().liveEnabled');
    expect(card).toContain('Provider status:');
  });

  it('validates payment provider and credit package forms with required fields', () => {
    const providerForm = read(
      'src/app/features/payments/components/payment-provider-form/payment-provider-form.ts',
    );
    const providerFormTemplate = read(
      'src/app/features/payments/components/payment-provider-form/payment-provider-form.html',
    );
    const creditPackageForm = read(
      'src/app/features/payments/components/credit-package-form/credit-package-form.ts',
    );
    const creditPackageTemplate = read(
      'src/app/features/payments/components/credit-package-form/credit-package-form.html',
    );

    expect(providerForm).toContain('Validators.required');
    expect(providerForm).toContain('this.form.invalid');
    expect(providerFormTemplate).toContain('Provider name is required.');
    expect(providerFormTemplate).toContain('Provider code is required.');
    expect(creditPackageForm).toContain('Validators.required');
    expect(creditPackageForm).toContain('this.form.invalid');
    expect(creditPackageTemplate).toContain('Package name');
    expect(creditPackageTemplate).toContain('Currency');
  });

  it('masks provider configuration secrets and supports sandbox/live environments', () => {
    const form = read(
      'src/app/features/payments/components/provider-configuration-form/provider-configuration-form.html',
    );
    const source = read(
      'src/app/features/payments/components/provider-configuration-form/provider-configuration-form.ts',
    );

    expect(form).toContain('placeholder="Saved securely"');
    expect(form).toContain('type="password"');
    expect(form).toContain('Secrets are sent only when saved.');
    expect(source).toContain('Object.values(EnvironmentType)');
    expect(source).toContain('EnvironmentType.Sandbox');
  });

  it('renders subscription purchase page, billing cycle toggle, selected package summary, and redirect handling', () => {
    const page = read('src/app/features/payments/subscription-purchase/subscription-purchase.html');
    const source = read('src/app/features/payments/subscription-purchase/subscription-purchase.ts');
    const toggle = read('src/app/features/payments/components/billing-cycle-toggle/billing-cycle-toggle.html');
    const toggleSource = read('src/app/features/payments/components/billing-cycle-toggle/billing-cycle-toggle.ts');

    expect(page).toContain('app-billing-cycle-toggle');
    expect(page).toContain('app-subscription-plan-card');
    expect(page).toContain('Selected package summary');
    expect(page).toContain('Continue to Payment');
    expect(source).toContain('purchaseSubscription(workspaceId, payload)');
    expect(source).toContain('upgradeSubscription(workspaceId, payload)');
    expect(source).toContain('renewSubscription(workspaceId, payload)');
    expect(source).toContain('window.location.assign(redirectUrl)');
    expect(toggle).toContain('(click)="select(cycle)"');
    expect(toggleSource).toContain('selectedChange.emit(cycle)');
  });

  it('renders credit purchase page, selected credit summary, and redirect handling', () => {
    const page = read('src/app/features/payments/credit-purchase/credit-purchase.html');
    const source = read('src/app/features/payments/credit-purchase/credit-purchase.ts');

    expect(page).toContain('app-credit-purchase-card');
    expect(page).toContain('Selected package summary');
    expect(page).toContain('Buy credits');
    expect(page).toContain('Continue to Payment');
    expect(page).toContain('Credits are used when generating creatives.');
    expect(source).toContain('purchaseCredits(workspaceId, payload)');
    expect(source).toContain('creditPackageId: creditPackage.id');
    expect(source).toContain('window.location.assign(redirectUrl)');
  });

  it('renders transactions, status badges, failed messages, and invoices', () => {
    const transactionPage = read('src/app/features/payments/transactions/payment-transactions.html');
    const transactionCard = read(
      'src/app/features/payments/components/payment-transaction-card/payment-transaction-card.html',
    );
    const statusBadge = read(
      'src/app/features/payments/components/payment-status-badge/payment-status-badge.ts',
    );
    const invoicePage = read('src/app/features/payments/invoices/invoices.html');
    const invoiceCard = read('src/app/features/payments/components/invoice-card/invoice-card.html');

    expect(transactionPage).toContain('Payment history');
    expect(transactionPage).toContain('app-payment-transaction-card');
    expect(transactionCard).toContain('app-payment-purpose-badge');
    expect(transactionCard).toContain('app-payment-status-badge');
    expect(transactionCard).toContain('transaction().failureReason');
    expect(transactionCard).toContain('maskedProviderTransactionId()');
    expect(statusBadge).toContain('Payment started');
    expect(statusBadge).toContain('Waiting for payment confirmation');
    expect(statusBadge).toContain('Payment successful');
    expect(statusBadge).toContain('Payment failed');
    expect(statusBadge).toContain('Payment session expired');
    expect(invoicePage).toContain('Invoices');
    expect(invoicePage).toContain('app-invoice-card');
    expect(invoiceCard).toContain('invoice().invoiceNumber');
    expect(invoiceCard).toContain('paymentTransactionId');
  });

  it('renders loading, empty, error-with-retry, and access denied states', () => {
    const combined = [
      'src/app/features/payments/dashboard/payment-dashboard.html',
      'src/app/features/payments/providers/payment-providers.html',
      'src/app/features/payments/provider-configurations/payment-provider-configurations.html',
      'src/app/features/payments/credit-packages/credit-packages.html',
      'src/app/features/payments/subscription-purchase/subscription-purchase.html',
      'src/app/features/payments/credit-purchase/credit-purchase.html',
      'src/app/features/payments/transactions/payment-transactions.html',
      'src/app/features/payments/invoices/invoices.html',
    ]
      .map(read)
      .join('\n');

    expect(combined).toContain('app-payment-loading-state');
    expect(combined).toContain('app-payment-empty-state');
    expect(combined).toContain('app-error-state');
    expect(combined).toContain('Try again');
    expect(combined).toContain('You do not have access');
  });

  it('keeps payment pages responsive and overflow-safe', () => {
    const combined = [
      'src/app/features/payments/dashboard/payment-dashboard.html',
      'src/app/features/payments/providers/payment-providers.html',
      'src/app/features/payments/provider-configurations/payment-provider-configurations.html',
      'src/app/features/payments/credit-packages/credit-packages.html',
      'src/app/features/payments/subscription-purchase/subscription-purchase.html',
      'src/app/features/payments/credit-purchase/credit-purchase.html',
      'src/app/features/payments/transactions/payment-transactions.html',
      'src/app/features/payments/invoices/invoices.html',
    ]
      .map(read)
      .join('\n');

    expect(combined).toContain('grid gap-4');
    expect(combined).toContain('min-w-0');
    expect(combined).not.toContain('w-screen');
  });
});

describe('Day 9 routes, guards, and navigation', () => {
  it('registers all payment routes', () => {
    const routes = read('src/app/features/payments/payment.routes.ts');

    for (const route of [
      "path: 'payments'",
      "path: 'payments/transactions'",
      "path: 'payments/invoices'",
      "path: 'payments/subscription'",
      "path: 'payments/credits'",
      "path: 'master/payments/providers'",
      "path: 'master/payments/provider-configurations'",
      "path: 'master/payments/credit-packages'",
      "path: 'master/payments/transactions'",
    ]) {
      expect(routes).toContain(route);
    }

    expect(routes).toContain('paymentProviderManagementAccessGuard');
    expect(routes).toContain('creditPackageManagementAccessGuard');
    expect(routes).toContain('subscriptionPurchaseAccessGuard');
    expect(routes).toContain('creditPurchaseAccessGuard');
    expect(routes).toContain('paymentTransactionsAccessGuard');
    expect(routes).toContain('invoicesAccessGuard');
  });

  it('guards provider and credit purchase routes through permission helpers', async () => {
    const createUrlTree = vi.fn((commands: readonly string[]) => ({ commands }));
    const canManagePaymentProviders = signal(false).asReadonly();
    const canManageCreditPackages = signal(false).asReadonly();
    const canPurchaseCredits = signal(false).asReadonly();

    TestBed.configureTestingModule({
      providers: [
        {
          provide: PermissionStore,
          useValue: {
            canManagePaymentProviders,
            canManageCreditPackages,
            canPurchaseCredits,
          },
        },
        { provide: Router, useValue: { createUrlTree } },
      ],
    });

    const providerResult = await TestBed.runInInjectionContext(() =>
      paymentProviderManagementAccessGuard({} as never, {} as never),
    );
    const creditPackageResult = await TestBed.runInInjectionContext(() =>
      creditPackageManagementAccessGuard({} as never, {} as never),
    );
    const creditPurchaseResult = await TestBed.runInInjectionContext(() =>
      creditPurchaseAccessGuard({} as never, {} as never),
    );

    expect(providerResult).toEqual({ commands: ['/payments'] });
    expect(creditPackageResult).toEqual({ commands: ['/payments'] });
    expect(creditPurchaseResult).toEqual({ commands: ['/payments'] });
    expect(createUrlTree).toHaveBeenCalledWith(['/payments']);
  });

  it('integrates ADMIN, MASTER, and CREW payment navigation through permission helpers', () => {
    const navigation = read('src/app/shared/layouts/dashboard-layout/dashboard-navigation.ts');
    const layout = read('src/app/shared/layouts/dashboard-layout/dashboard-layout.ts');

    expect(navigation).toContain("label: 'Payments'");
    expect(navigation).toContain("label: 'Subscription'");
    expect(navigation).toContain("label: 'Buy Credits'");
    expect(navigation).toContain("label: 'Payment History'");
    expect(navigation).toContain("label: 'Invoices'");
    expect(navigation).toContain("label: 'Payment Providers'");
    expect(navigation).toContain("label: 'Provider Management'");
    expect(navigation).toContain("label: 'Credit Packages'");
    expect(navigation).toContain("label: 'Payment Transactions'");
    expect(navigation).toContain("roles: ['ADMIN', 'CREW']");
    expect(navigation).toContain("roles: ['MASTER']");
    expect(layout).toContain('canPurchaseSubscription');
    expect(layout).toContain('canPurchaseCredits');
    expect(layout).toContain('canViewPayments');
    expect(layout).toContain('canViewInvoices');
    expect(layout).toContain('canManagePaymentProviders');
    expect(layout).toContain('canManageCreditPackages');
  });
});

describe('Payment History summary layout', () => {
  it('renders compact summary cards in a responsive single-row desktop grid', () => {
    const template = read('src/app/features/payments/transactions/payment-transactions.html');
    const styles = read('src/app/features/payments/transactions/payment-transactions.scss');
    const source = read('src/app/features/payments/transactions/payment-transactions.ts');

    expect(template).toContain('class="payment-summary-grid"');
    expect(template).toContain('@for (card of summaryCards(); track card.key)');
    expect(template).toContain('<app-card [padded]="false">');
    expect(template).toContain('payment-summary-card');
    expect(source).toContain("label: 'Total payments'");
    expect(source).toContain("label: 'Successful payments'");
    expect(source).toContain("label: 'Pending or failed'");
    expect(source).toContain("description: 'Shown after filters are applied.'");
    expect(source).toContain("description: 'Backend-confirmed successful payments.'");
    expect(source).toContain("description: 'Payments waiting for confirmation or requiring attention.'");
    expect(styles).toContain('grid-template-columns: repeat(3, minmax(0, 1fr))');
    expect(styles).toContain('grid-template-columns: repeat(2, minmax(0, 1fr))');
    expect(styles).toContain('grid-template-columns: 1fr');
    expect(styles).toContain('min-height: 128px');
    expect(styles).toContain('padding: 1.25rem');
    expect(styles).toContain('rgb(var(--color-text-primary))');
    expect(styles).toContain('rgb(var(--color-text-muted))');
  });
});

describe('Day 9 static verification guardrails', () => {
  it('has all required payment components', () => {
    for (const component of [
      'payment-provider-card',
      'payment-provider-form',
      'provider-configuration-form',
      'credit-package-card',
      'credit-package-form',
      'subscription-plan-card',
      'credit-purchase-card',
      'payment-transaction-card',
      'invoice-card',
      'payment-status-badge',
      'payment-purpose-badge',
      'billing-cycle-toggle',
      'payment-empty-state',
      'payment-loading-state',
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

  it('does not use legacy Angular syntax, CSS files, inline templates, or component naming', () => {
    const files = collectFiles(featureRoot, () => true);
    const htmlOffenders = files
      .filter((filePath) => filePath.endsWith('.html'))
      .filter((filePath) => /\*ngIf\b|\*ngFor\b/.test(readFileSync(filePath, 'utf8')))
      .map(relativeFeaturePath);
    const sourceOffenders = files
      .filter((filePath) => filePath.endsWith('.ts') && !filePath.endsWith('.spec.ts'))
      .filter((filePath) => /@NgModule\b|template\s*:`|template\s*: '/.test(readFileSync(filePath, 'utf8')))
      .map(relativeFeaturePath);
    const cssFiles = files.filter((filePath) => filePath.endsWith('.css')).map(relativeFeaturePath);
    const componentNamedFiles = files
      .filter((filePath) => /\.component\./.test(filePath))
      .map(relativeFeaturePath);

    expect(htmlOffenders).toEqual([]);
    expect(sourceOffenders).toEqual([]);
    expect(cssFiles).toEqual([]);
    expect(componentNamedFiles).toEqual([]);
  });

  it('does not hardcode provider names, credit package names, plan names, or payment amounts in production files', () => {
    const forbidden = /\b(Free|Basic|Pro|Enterprise|OpenAI|Gemini|Stripe Test|SSLCommerz Test|Mock Credit Package|Mock Backend Package)\b|\b9999\b/;
    const offenders = collectFiles(featureRoot, (filePath) => {
      const productionFile = /\.(ts|html)$/.test(filePath) && !filePath.endsWith('.spec.ts');
      return productionFile && forbidden.test(readFileSync(filePath, 'utf8'));
    }).map(relativeFeaturePath);

    expect(offenders).toEqual([]);
  });

  it('does not include fake payment data, fake success UI, secrets exposure, storage of secrets, tax, accounting, refund dispute, or Day 10 modules', () => {
    const forbidden =
      /fake payment|fake success|fake data|localStorage|sessionStorage|console\.log|raw webhook|webhook payload|\btax\b|\bVAT\b|\baccounting\b|refund dispute|Day 10/i;
    const offenders = collectFiles(featureRoot, (filePath) => {
      const productionFile = /\.(ts|html)$/.test(filePath) && !filePath.endsWith('.spec.ts');
      return productionFile && forbidden.test(readFileSync(filePath, 'utf8'));
    }).map(relativeFeaturePath);

    expect(offenders).toEqual([]);
  });

  it('uses standard ApiResponse handling in the payment API service', () => {
    const service = read('src/app/features/payments/services/payment-api.service.ts');
    const coreApi = read('src/app/core/api/api.service.ts');
    const unwrap = read('src/app/shared/utils/api-response.ts');

    expect(service).toContain('unwrapApiResponse');
    expect(service).toContain('this.api.get<T>(path');
    expect(service).toContain('this.api.post<T, TBody>(path');
    expect(coreApi).toContain('ApiResponse<T>');
    expect(unwrap).toContain('ApiResponse<T>');
  });
});
