# Day 9 Payment Foundation

Project: Lebhas - Brand Attire

## Scope

Day 9 Batch 1 adds the frontend foundation for payment providers, subscriptions, credit purchases, workspace payment transactions, and invoices.

This batch intentionally does not create payment pages, shared payment components, payment success screens, checkout screens, credit purchase UI, invoice UI, or Day 10+ modules.

## Implemented Files

```text
src/app/features/payments/
|-- models/
|   `-- payment.models.ts
|-- services/
|   |-- payment-api.service.ts
|   `-- payment-security.ts
|-- state/
|   `-- payment.store.ts
`-- README.md
```

The existing `PermissionStore` was extended with payment permission helpers. No duplicate role model, auth service, API client, or state management system was added.

## Models And Enums

Strict TypeScript models were added for:

- `PaymentProvider`
- `PaymentProviderConfiguration`
- `PaymentTransaction`
- `CreditPackage`
- `CreditPurchaseOrder`
- `SubscriptionOrder`
- `Invoice`
- `PaymentSessionResponse`
- pricing plans for purchase
- current subscription
- payment filters
- invoice filters
- provider configuration payloads
- credit package payloads
- subscription and credit purchase payloads

Enums added:

- `PaymentProviderType`
- `PaymentPurpose`
- `PaymentStatus`
- `BillingCycle`
- `InvoiceType`
- `EnvironmentType`

The provider enum exists only for strict model typing. Active providers, provider display names, package names, prices, credit values, and session URLs must come from backend APIs.

## API Service

One API service was added:

```text
services/payment-api.service.ts
```

It reuses the existing shared `ApiService`, base URL handling, auth headers/interceptors, and `unwrapApiResponse`. It does not duplicate common HTTP logic.

Master APIs:

- `POST /api/v1/master/payment-providers`
- `GET /api/v1/master/payment-providers`
- `PUT /api/v1/master/payment-providers/{providerId}`
- `POST /api/v1/master/payment-provider-configurations`
- `PUT /api/v1/master/payment-provider-configurations/{configurationId}`
- `POST /api/v1/master/credit-packages`
- `GET /api/v1/master/credit-packages`
- `PUT /api/v1/master/credit-packages/{creditPackageId}`

Workspace APIs:

- `POST /api/v1/workspaces/{workspaceId}/subscriptions/purchase`
- `POST /api/v1/workspaces/{workspaceId}/subscriptions/upgrade`
- `POST /api/v1/workspaces/{workspaceId}/subscriptions/renew`
- `POST /api/v1/workspaces/{workspaceId}/credits/purchase`
- `GET /api/v1/workspaces/{workspaceId}/payments`
- `GET /api/v1/workspaces/{workspaceId}/payments/{paymentTransactionId}`
- `GET /api/v1/workspaces/{workspaceId}/invoices`

Webhook APIs are backend/provider-facing only and are intentionally not exposed as normal user UI.

## Signal Store

One signal store was added:

```text
state/payment.store.ts
```

State includes:

- payment providers
- provider configurations
- credit packages
- current subscription
- pricing plans for purchase
- selected pricing plan
- selected billing cycle
- selected credit package
- payment transactions
- invoices
- active payment session
- loading
- error

Computed signals include:

- active providers
- disabled providers
- sandbox providers
- live providers
- active credit packages
- selected subscription summary
- selected credit purchase summary
- successful payments
- failed payments
- pending payments

The store uses Angular Signals and does not use NgRx.

## Permission Foundation

The existing `PermissionStore` was extended with:

- `canManagePaymentProviders`
- `canManageCreditPackages`
- `canPurchaseSubscription`
- `canPurchaseCredits`
- `canViewPayments`
- `canViewInvoices`

Permission behavior:

- `MASTER`: can manage payment providers, provider configurations, credit packages, and payment transaction foundations when backend feature policy allows it.
- `ADMIN`: can purchase, upgrade, and renew subscriptions; purchase credits; view workspace payments; and view workspace invoices when explicit permission and backend feature policy allow it.
- `CREW`: cannot purchase unless explicitly allowed and can view payment info only if permission and backend feature policy allow it.

## Payment Safety Rules

The frontend must never calculate the final payment amount as source of truth.

The frontend may display only backend-returned:

- package details
- credit package details
- prices
- currency
- payment status
- payment session URL

Backend remains responsible for:

- final amount
- credits
- subscription price
- provider selection/session
- provider payment validation

The frontend must not hardcode payment providers, credit packages, pricing plans, payment amounts, or package prices.

## Secret Handling

Payment secret display helpers were added:

- `maskSecret(value)`
- `maskProviderTransactionId(value)`
- `safeDisplaySecret(value)`

Rules:

- never show raw secrets after save
- do not store secrets in localStorage
- do not log secrets
- do not expose raw webhook payloads
- do not expose provider secret fields in normal tables

## Intentional Exclusions

Day 9 Batch 1 intentionally excludes:

- payment pages
- shared payment components
- payment success UI
- checkout UI
- credit purchase UI
- invoice UI
- webhook user UI
- provider credential table UI
- fake payment data
- fake payment success behavior
- Day 10+ modules

## Implemented In Batch 2

Batch 2 adds reusable, presentational Payment UI components only:

```text
src/app/features/payments/components/
|-- billing-cycle-toggle/
|-- credit-package-card/
|-- credit-package-form/
|-- credit-purchase-card/
|-- invoice-card/
|-- payment-empty-state/
|-- payment-loading-state/
|-- payment-provider-card/
|-- payment-provider-form/
|-- payment-purpose-badge/
|-- payment-status-badge/
|-- payment-transaction-card/
|-- provider-configuration-form/
`-- subscription-plan-card/
```

Component behavior:

- provider cards and forms display backend provider records and emit backend-shaped payloads
- provider configuration form uses password-style fields and `Saved securely` placeholders for secrets
- credit package cards and forms display backend package values and do not calculate final purchase amount
- subscription plan cards display backend package names, prices, features, and limits dynamically
- credit purchase cards show backend credit package values and a best-value badge only when backend data provides it
- payment transaction cards mask provider transaction IDs and do not expose webhook payloads
- invoice cards display invoice metadata and expose only a placeholder download action when a parent enables it
- payment status and purpose badges map backend enum values to friendly labels
- billing cycle toggle emits the selected cycle without calculating final payment amount
- empty and loading states provide reusable payment-safe states for future pages

Security behavior:

- raw provider secrets are not displayed after save
- secret inputs use password-style controls
- secret placeholders use `Saved securely`
- provider transaction IDs are masked for display
- no component stores secrets in localStorage
- no component logs secrets
- no component exposes raw webhook payloads

Batch 2 still does not add checkout logic, fake payment success UI, payment pages, credit purchase pages, invoice pages, or Day 10+ modules.

## Implemented In Batch 3

Batch 3 adds Master-only payment provider setup screens:

```text
src/app/features/payments/
|-- providers/
|   |-- payment-providers.ts
|   |-- payment-providers.html
|   `-- payment-providers.scss
|-- provider-configurations/
|   |-- payment-provider-configurations.ts
|   |-- payment-provider-configurations.html
|   `-- payment-provider-configurations.scss
`-- payment.routes.ts
```

Protected Master routes:

- `/master/payments/providers`
- `/master/payments/provider-configurations`

The provider setup page uses:

- `PaymentStore.loadPaymentProviders()`
- `PaymentStore.createPaymentProvider(payload)`
- `PaymentStore.updatePaymentProvider(providerId, payload)`
- `payment-provider-card`
- `payment-provider-form`
- `payment-empty-state`
- `payment-loading-state`

The provider configuration page uses:

- `PaymentStore.loadPaymentProviders()`
- `PaymentStore.createProviderConfiguration(payload)`
- `PaymentStore.updateProviderConfiguration(configurationId, payload)`
- `provider-configuration-form`
- `payment-empty-state`
- `payment-loading-state`

Access is controlled by the existing `PermissionStore.canManagePaymentProviders` helper. Unauthorized ADMIN and CREW users see:

```text
You do not have access to payment provider settings.
Please contact the system owner if you need access.
```

Secret masking and handling rules remain in force:

- raw secrets are never shown after save
- secret fields use password-style inputs
- saved secrets are represented with `Saved securely`
- no raw webhook payloads are exposed
- no user-facing webhook calling UI is implemented

Batch 3 does not implement subscription purchase, credit purchase, invoices, workspace payment pages, webhook-calling UI, or Day 10+ modules.

## Implemented In Batch 4

Batch 4 adds the Master-only credit package setup screen:

```text
src/app/features/payments/
|-- credit-packages/
|   |-- credit-packages.ts
|   |-- credit-packages.html
|   `-- credit-packages.scss
`-- payment.routes.ts
```

Protected Master route:

- `/master/payments/credit-packages`

The credit package page uses:

- `PaymentStore.loadCreditPackages()`
- `PaymentStore.createCreditPackage(payload)`
- `PaymentStore.updateCreditPackage(creditPackageId, payload)`
- `credit-package-card`
- `credit-package-form`
- `payment-empty-state`
- `payment-loading-state`

Access is controlled by the existing `PermissionStore.canManageCreditPackages` helper. Unauthorized ADMIN and CREW users see:

```text
You do not have access to credit package settings.
Please contact the system owner if you need access.
```

Dynamic credit package rules:

- package names come from backend
- package prices come from backend
- credit amounts come from backend
- bonus credit values come from backend
- currency values come from backend
- sort order comes from backend-managed package configuration

The frontend does not calculate final credit purchase amount as source of truth. Backend remains responsible for calculating payment amount, credits, bonus credits, and purchase session details.

Batch 4 does not implement workspace credit purchase pages, subscription purchase pages, checkout, fake payment success, invoices, or Day 10+ modules.

## Implemented In Batch 5

Batch 5 adds the workspace subscription purchase foundation:

```text
src/app/features/payments/
|-- subscription-purchase/
|   |-- subscription-purchase.ts
|   |-- subscription-purchase.html
|   `-- subscription-purchase.scss
`-- payment.routes.ts
```

Protected workspace route:

- `/payments/subscription`

The page uses:

- `PaymentStore.purchaseSubscription(workspaceId, payload)`
- `PaymentStore.upgradeSubscription(workspaceId, payload)`
- `PaymentStore.renewSubscription(workspaceId, payload)`
- `PaymentStore.setSelectedPricingPlan(plan)`
- `PaymentStore.setSelectedBillingCycle(cycle)`
- `subscription-plan-card`
- `billing-cycle-toggle`
- `payment-status-badge`
- `payment-empty-state`
- `payment-loading-state`

The page reads the active workspace ID and current subscription context from the existing `WorkspaceStore`; it does not duplicate workspace state or hardcode a workspace ID.

Backend payment redirect rule:

- frontend sends only `pricingPlanId`, `billingCycle`, and an optional `returnUrl`
- backend creates the payment session and returns `paymentTransactionId`, `paymentStatus`, `providerName`, `providerSessionId`, `paymentRedirectUrl`, and `expiresAt`
- when `paymentRedirectUrl` is returned, the page shows a confirmation summary and a `Continue to Payment` button
- redirect opens in the same tab
- success or failure is not faked after redirect and must come from backend verification/status

Payment safety rules remain in force:

- no frontend final amount calculation
- no hardcoded package names
- no hardcoded package limits
- no hardcoded provider names
- no fake payment success state
- no checkout UI beyond backend redirect foundation

Batch 5 does not implement credit purchase pages, invoice pages, fake payment success, Day 10+ modules, or any frontend-owned payment amount calculation.

## Implemented In Batch 6

Batch 6 adds the workspace credit purchase foundation:

```text
src/app/features/payments/
|-- credit-purchase/
|   |-- credit-purchase.ts
|   |-- credit-purchase.html
|   `-- credit-purchase.scss
`-- payment.routes.ts
```

Protected workspace route:

- `/payments/credits`

The page uses:

- `PaymentStore.loadCreditPackages()`
- `PaymentStore.purchaseCredits(workspaceId, payload)`
- `PaymentStore.setSelectedCreditPackage(creditPackage)`
- `PaymentStore.activeCreditPackages`
- `PaymentStore.selectedCreditPurchaseSummary`
- `PaymentStore.activePaymentSession`
- `credit-purchase-card`
- `payment-status-badge`
- `payment-empty-state`
- `payment-loading-state`

The page reuses the existing `WorkspaceStore` for active workspace context and reads current credit balance from the existing Usage & Billing store when available. It does not duplicate usage or billing credit state.

Backend payment redirect rule:

- frontend sends only `creditPackageId` and an optional `returnUrl`
- backend creates the payment session and returns `paymentTransactionId`, `paymentStatus`, `providerName`, `providerSessionId`, `paymentRedirectUrl`, and `expiresAt`
- when `paymentRedirectUrl` is returned, the page shows a confirmation summary and a `Continue to Payment` button
- redirect opens in the same tab
- success or failure is not faked after redirect and must come from backend verification/status

Credit purchase safety rules remain in force:

- no frontend final price calculation
- no hardcoded credit package names
- no hardcoded credit amounts
- no hardcoded bonus credit rules
- no hardcoded package prices
- no fake discounts
- best-value display is allowed only when backend package data includes it
- no fake payment success state

Batch 6 does not implement invoice pages, payment transaction pages, fake payment success, Day 10+ modules, or frontend-owned payment amount calculation.

## Implemented In Batch 7

Batch 7 adds payment transaction history and invoice foundation screens:

```text
src/app/features/payments/
|-- transactions/
|   |-- payment-transactions.ts
|   |-- payment-transactions.html
|   `-- payment-transactions.scss
|-- invoices/
|   |-- invoices.ts
|   |-- invoices.html
|   `-- invoices.scss
`-- payment.routes.ts
```

Protected routes:

- `/payments/transactions`
- `/payments/invoices`
- `/master/payments/transactions`

The workspace payment history page uses:

- `PaymentStore.loadWorkspacePayments(workspaceId)`
- `PaymentStore.paymentTransactions`
- `PaymentStore.successfulPayments`
- `PaymentStore.failedPayments`
- `PaymentStore.pendingPayments`
- `payment-transaction-card`
- `payment-status-badge`
- `payment-purpose-badge`
- `payment-empty-state`
- `payment-loading-state`

The invoice page uses:

- `PaymentStore.loadWorkspaceInvoices(workspaceId)`
- `PaymentStore.invoices`
- `invoice-card`
- `payment-status-badge`
- `payment-empty-state`
- `payment-loading-state`

The Master payment transaction route is a foundation route only. The existing payment API service does not expose a global Master payment transaction endpoint, so no fake Master transaction records are generated.

Security and safety rules:

- provider transaction IDs remain masked through the existing transaction card
- raw webhook payloads are not shown
- provider secret fields are not shown
- raw sensitive provider metadata is not shown
- invoice PDF generation is not implemented because no backend invoice download endpoint exists in the current payment API service
- no accounting, tax, VAT, refund dispute, or Day 10+ workflow is implemented

## Implemented In Batch 8

Batch 8 adds the workspace Payment Dashboard, payment route guards, navigation integration, and final integration cleanup:

```text
src/app/features/payments/
|-- dashboard/
|   |-- payment-dashboard.ts
|   |-- payment-dashboard.html
|   `-- payment-dashboard.scss
`-- payment.routes.ts

src/app/core/guards/
`-- payment-access.guard.ts
```

Workspace payment routes:

- `/payments`
- `/payments/subscription`
- `/payments/credits`
- `/payments/transactions`
- `/payments/invoices`

Master payment routes:

- `/master/payments/providers`
- `/master/payments/provider-configurations`
- `/master/payments/credit-packages`
- `/master/payments/transactions`

The Payment Dashboard shows:

- current subscription summary from workspace context
- current credit balance from Usage & Billing when available
- recent payment transactions
- recent invoices
- quick actions for subscription, credit purchase, payment history, and invoices

Navigation rules:

- `ADMIN` sees the Payments entry and permission-aware payment subitems
- `MASTER` sees provider management, provider configurations, credit packages, and payment transaction foundation entries
- `CREW` payment entries remain hidden unless an explicit permission helper allows that route

Route/permission rules:

- subscription purchase uses `canPurchaseSubscription`
- credit purchase uses `canPurchaseCredits`
- payment history uses `canViewPayments`
- invoices use `canViewInvoices`
- provider setup uses `canManagePaymentProviders`
- credit package setup uses `canManageCreditPackages`
- unauthorized payment subroutes redirect to `/payments`, where the user sees a friendly access-denied state

Batch 8 keeps payment safety boundaries:

- no fake payment success UI
- no fake payment data
- no frontend final amount calculation
- no hardcoded payment providers as business logic
- no hardcoded credit packages, pricing plans, or payment amounts
- no provider secrets
- no raw webhook payload exposure
- no tax, VAT, accounting, refund dispute, or Day 10+ workflow

## Final Day 9 Notes

Implemented Day 9 scope:

- strict payment models and enums
- one payment API service
- one signal-based payment store
- permission helper integration
- reusable payment components
- Master payment provider setup
- Master provider configuration setup
- Master credit package setup
- workspace subscription purchase foundation
- workspace credit purchase foundation
- workspace payment transaction history
- workspace invoice foundation
- workspace payment dashboard
- payment navigation and route guards
- Day 9 verification tests

Routes:

- `/payments`
- `/payments/subscription`
- `/payments/credits`
- `/payments/transactions`
- `/payments/invoices`
- `/master/payments/providers`
- `/master/payments/provider-configurations`
- `/master/payments/credit-packages`
- `/master/payments/transactions`

Backend APIs used:

- `GET /api/v1/master/payment-providers`
- `POST /api/v1/master/payment-providers`
- `PUT /api/v1/master/payment-providers/{providerId}`
- `POST /api/v1/master/payment-provider-configurations`
- `PUT /api/v1/master/payment-provider-configurations/{configurationId}`
- `GET /api/v1/master/credit-packages`
- `POST /api/v1/master/credit-packages`
- `PUT /api/v1/master/credit-packages/{creditPackageId}`
- `POST /api/v1/workspaces/{workspaceId}/subscriptions/purchase`
- `POST /api/v1/workspaces/{workspaceId}/subscriptions/upgrade`
- `POST /api/v1/workspaces/{workspaceId}/subscriptions/renew`
- `POST /api/v1/workspaces/{workspaceId}/credits/purchase`
- `GET /api/v1/workspaces/{workspaceId}/payments`
- `GET /api/v1/workspaces/{workspaceId}/payments/{paymentTransactionId}`
- `GET /api/v1/workspaces/{workspaceId}/invoices`

Permission rules:

- `MASTER` can manage payment providers, provider configurations, credit packages, and payment transaction foundations when backend feature policy allows it.
- `ADMIN` can manage workspace subscription purchase, credit purchase, payment history, and invoices when permission and backend feature policy allow it.
- `CREW` remains hidden or blocked unless explicit payment permissions are provided.
- Unauthorized users are redirected to `/payments`, which shows friendly access-denied copy instead of technical permission details.

Dynamic provider/package/pricing rule:

- active providers come from backend
- provider names come from backend
- credit package names, credits, bonus credits, prices, currencies, and sort order come from backend
- pricing package names, features, limits, prices, and currencies come from backend when available
- frontend does not hardcode payment provider business logic, credit package names, pricing plan names, limits, or payment amounts

Payment safety:

- frontend never calculates final payment amount as source of truth
- frontend sends selected identifiers such as `pricingPlanId`, `billingCycle`, or `creditPackageId`
- backend creates payment sessions and returns `paymentTransactionId`, `paymentStatus`, `providerName`, `providerSessionId`, `paymentRedirectUrl`, and `expiresAt`
- payment redirects open in the same tab using the backend-returned URL
- frontend does not fake success after redirect
- success, failure, cancellation, expiry, or refund status must come from backend verification/status

Provider secret masking:

- provider configuration inputs use password-style fields
- saved secret placeholders show `Saved securely`
- raw provider secrets are not displayed after save
- secrets are not stored in local storage
- secrets are not logged
- raw webhook payloads are not exposed in user-facing screens

Test coverage summary:

- `payments.day9.spec.ts` covers payment store API calls, selected subscription and credit summaries, backend redirect session handling, permission denial, payment route registration, route guards, navigation entries, shared component presence, loading/empty/error states, status/purpose display foundations, invoice foundations, and static payment-safety guardrails.

Known integration assumptions:

- current subscription and credit balance are read from existing workspace and Usage & Billing state when available.
- available pricing plans for subscription purchase depend on backend/store data exposed in `PaymentStore.pricingPlansForPurchase`.
- Master global payment transaction route is a foundation route because the current payment API service does not expose a global Master transaction endpoint.
- invoice download remains disabled until backend exposes a supported invoice download endpoint.
