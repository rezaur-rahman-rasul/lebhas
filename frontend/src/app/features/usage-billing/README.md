# Day 8 Usage & Billing

Project: Lebhas - Brand Attire

## Implemented Scope

Day 8 adds the Usage & Billing frontend foundation and workspace/Master reporting UI for existing Day 1 to Day 7 flows. It does not add payment, checkout, credit purchase, invoice, tax/VAT, accounting, or Day 9+ modules.

Implemented structure:

```text
src/app/features/usage-billing/
|-- components/
|-- credit-ledger/
|-- dashboard/
|-- download-usage/
|-- master-usage/
|-- models/
|-- monthly-snapshots/
|-- services/
|-- share-usage/
|-- state/
|-- usage-logs/
|-- README.md
`-- usage-billing.routes.ts
```

## Routes

Workspace/Admin routes:

- `/usage-billing`
- `/usage-billing/credits`
- `/usage-billing/logs`
- `/usage-billing/downloads`
- `/usage-billing/shares`
- `/usage-billing/monthly-snapshots`

Master routes:

- `/master/usage`
- `/master/usage/workspaces`
- `/master/usage/ai-costs`
- `/master/usage/plan-utilization`

Routes are registered inside the existing protected route tree and reuse the existing dashboard layout, auth foundation, workspace context, and permission system.

## Backend APIs

Workspace APIs:

- `GET /api/v1/workspaces/{workspaceId}/usage-summary`
- `GET /api/v1/workspaces/{workspaceId}/usage-summary/current-month`
- `GET /api/v1/workspaces/{workspaceId}/credit-ledger`
- `GET /api/v1/workspaces/{workspaceId}/usage-billing-logs`
- `GET /api/v1/workspaces/{workspaceId}/download-usage`
- `GET /api/v1/workspaces/{workspaceId}/share-usage`
- `GET /api/v1/workspaces/{workspaceId}/monthly-usage-snapshots`

Master APIs:

- `GET /api/v1/master/usage/workspaces`
- `GET /api/v1/master/usage/workspaces/{workspaceId}`
- `GET /api/v1/master/usage/ai-costs`
- `GET /api/v1/master/usage/top-cost-workspaces`
- `GET /api/v1/master/usage/plan-utilization`

`UsageBillingApiService` reuses the shared `ApiService`, centralized auth/base URL handling, and `unwrapApiResponse`. It does not duplicate HTTP response parsing.

## Models And State

Strict TypeScript models and enums live in `models/usage-billing.models.ts`:

- credit ledger
- workspace usage summary
- usage billing logs
- download usage logs
- share usage logs
- monthly usage snapshots
- Master workspace usage
- Master AI cost usage
- top cost workspace
- plan utilization
- usage filters
- transaction, usage, and download type enums

`UsageBillingStore` is the single signal-based state owner for Usage & Billing. It stores workspace usage, recent history, Master usage summaries, selected filters, loading state, and friendly errors.

Computed signals include:

- available credits
- credit usage percent
- storage usage percent
- generated version usage percent
- near-limit states
- recent transactions
- recent usage logs

## Shared Components

Reusable Usage & Billing components live under `components/`:

- `credit-balance-card`
- `usage-summary-card`
- `plan-utilization-card`
- `credit-ledger-card`
- `usage-log-card`
- `download-usage-card`
- `share-usage-card`
- `monthly-snapshot-card`
- `usage-progress-bar`
- `credit-lifecycle-timeline`
- `usage-filter-bar`
- `usage-empty-state`
- `usage-loading-state`

These components use standalone Angular, templateUrl/styleUrl, SCSS, Tailwind CSS, Angular block syntax, and existing shared UI primitives where useful.

## Workspace UI

The workspace dashboard at `/usage-billing` shows:

- active package/subscription summary from workspace context/backend data
- current month usage
- available, reserved, used, and refunded credits
- package limit progress
- total creative requests and generated versions
- estimated AI cost
- uploads, storage, downloads, public shares, prompt enhancements, generation failures, and API calls
- recent credit history
- recent usage history
- credit lifecycle explanation

Detail pages show:

- `/usage-billing/credits`: credit history with transaction filters
- `/usage-billing/logs`: usage history with usage type filters
- `/usage-billing/downloads`: download usage without exposing raw sensitive details by default
- `/usage-billing/shares`: share access usage without exposing raw sensitive details by default
- `/usage-billing/monthly-snapshots`: monthly usage report cards

All workspace pages use the active workspace from `WorkspaceStore`. No workspace ID is hardcoded.

## Master UI

The Master usage overview at `/master/usage` and related Master routes show:

- workspace usage summaries
- AI cost usage
- top cost workspaces
- plan utilization from backend package data

The Master UI avoids provider credentials, secrets, internal API keys, setup screens, payment screens, and deep Day 9+ analytics.

## Permission Rules

Usage & Billing extends the existing `PermissionStore`; it does not introduce a duplicate role or permission system.

Permission helpers:

- `canViewUsageBilling`
- `canViewCreditLedger`
- `canViewDownloadUsage`
- `canViewShareUsage`
- `canViewMasterUsage`
- `canViewPlanUtilization`

Access behavior:

- `MASTER`: can view Master usage summaries, all workspace usage, AI costs, and plan utilization when backend policy allows it.
- `ADMIN`: can view own workspace usage, credit ledger, usage logs, downloads, shares, and monthly snapshots when permission and backend policy allow it.
- `CREW`: hidden unless explicitly allowed. Unauthorized users see a friendly access-denied state.

Access-denied copy:

```text
You do not have access to usage and billing.
Please contact your workspace admin if you need access.
```

## Dynamic Package And Limit Rule

Package names, pricing values, credit limits, storage limits, generated version limits, feature availability, and plan utilization values are backend-owned.

The frontend does not hardcode package names such as Free, Basic, Pro, or Enterprise. It also does not hardcode generated version limits, storage limits, or credit limits.

## UX And Accessibility

Day 8 screens use ordinary-user-friendly wording:

- Usage & Billing
- Credit history
- Usage history
- Package limits
- AI steps used
- Estimated AI cost
- Shared links used

Every page includes loading, empty, error, retry, and access-denied states where relevant. Layouts use responsive cards and desktop lists/tables, with mobile-safe filters and no intended horizontal overflow.

Sensitive raw details such as full user-agent strings or raw IP addresses are not exposed by default in download/share usage cards.

## Test Coverage

Batch 8 adds `usage-billing.day8.spec.ts` covering:

- ADMIN workspace dashboard loading
- CREW unauthorized dashboard behavior
- MASTER usage overview loading
- credit balance, reserved credits, and refunded credits
- credit ledger cards, transaction badges, and lifecycle timeline
- current month usage summary
- backend package name display
- generated version and storage usage percentage computations
- usage logs, download logs, share logs, and monthly snapshots
- month, usage type, and transaction type filter state
- loading, empty, and retryable error states
- mobile overflow guardrails
- route registration and permission-aware navigation
- standard `ApiResponse<T>` handling
- template and file naming guardrails

## Final Verification Notes

Day 8 production files are expected to remain free of:

- NgModules
- legacy `*ngIf` or `*ngFor`
- inline templates
- CSS files
- `.component` naming
- hardcoded package names
- hardcoded credit, storage, or generated-version limits
- fake billing data
- payment checkout UI
- subscription purchase UI
- credit purchase UI
- invoice payment UI
- tax/VAT UI
- accounting UI
- Day 9+ modules

## Intentional Exclusions

Day 8 intentionally excludes:

- payment UI
- subscription checkout
- billing payment UI
- credit purchase UI
- invoice UI
- tax/VAT UI
- accounting UI
- social publishing UI
- provider credential UI
- Day 9+ modules
- fake analytics or fake billing data
