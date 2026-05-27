import { Routes } from '@angular/router';

import {
  creditPackageManagementAccessGuard,
  creditPurchaseAccessGuard,
  invoicesAccessGuard,
  paymentProviderManagementAccessGuard,
  paymentTransactionsAccessGuard,
  subscriptionPurchaseAccessGuard,
} from '@app/core/guards/payment-access.guard';

export const PAYMENT_ROUTES: Routes = [
  {
    path: 'payments',
    loadComponent: () =>
      import('./dashboard/payment-dashboard').then((m) => m.PaymentDashboardPage),
  },
  {
    path: 'master/payments/providers',
    canActivate: [paymentProviderManagementAccessGuard],
    loadComponent: () =>
      import('./providers/payment-providers').then((m) => m.PaymentProvidersPage),
  },
  {
    path: 'master/payments/provider-configurations',
    canActivate: [paymentProviderManagementAccessGuard],
    loadComponent: () =>
      import('./provider-configurations/payment-provider-configurations').then(
        (m) => m.PaymentProviderConfigurationsPage,
      ),
  },
  {
    path: 'master/payments/credit-packages',
    canActivate: [creditPackageManagementAccessGuard],
    loadComponent: () =>
      import('./credit-packages/credit-packages').then((m) => m.CreditPackagesPage),
  },
  {
    path: 'payments/subscription',
    canActivate: [subscriptionPurchaseAccessGuard],
    loadComponent: () =>
      import('./subscription-purchase/subscription-purchase').then(
        (m) => m.SubscriptionPurchasePage,
      ),
  },
  {
    path: 'payments/credits',
    canActivate: [creditPurchaseAccessGuard],
    loadComponent: () =>
      import('./credit-purchase/credit-purchase').then((m) => m.CreditPurchasePage),
  },
  {
    path: 'payments/transactions',
    canActivate: [paymentTransactionsAccessGuard],
    loadComponent: () =>
      import('./transactions/payment-transactions').then((m) => m.PaymentTransactionsPage),
  },
  {
    path: 'payments/invoices',
    canActivate: [invoicesAccessGuard],
    loadComponent: () => import('./invoices/invoices').then((m) => m.InvoicesPage),
  },
  {
    path: 'master/payments/transactions',
    canActivate: [paymentTransactionsAccessGuard],
    loadComponent: () =>
      import('./transactions/payment-transactions').then((m) => m.PaymentTransactionsPage),
    data: { scope: 'master' },
  },
];
