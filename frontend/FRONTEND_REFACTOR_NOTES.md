# Lebhas Frontend Refactor Notes

Batch 1 scope: audit and clean the existing Day 1 to Day 6 Angular frontend without rewriting screens or adding Day 7+ modules.

## Current Structure Audit

- `src/app/core` contains cross-cutting API, auth, guards, interceptors, layout, permissions, theme, and workspace context.
- `src/app/shared` contains reusable UI primitives, shared models, pipes, validators, and utilities.
- `src/app/features/public` owns the homepage and modal login entry.
- `src/app/features/auth` owns login/register dialog/forms and auth facade behavior.
- `src/app/features/dashboard`, `brands`, `product-services`, `projects`, `assets`, and `prompts` contain the active Day 1 to Day 6 workspace surfaces.
- `src/app/features/admin`, `crew`, and `master` contain older or role-specific module shapes that overlap with the flatter active route structure. Their inactive route map files were removed in Batch 1.

## Duplicate / Redundant Cleanup Map

### Auth Services and Models

- Keep as canonical:
  - `core/auth/auth.service.ts`
  - `core/auth/current-user.store.ts`
  - `core/auth/token-storage.service.ts`
  - `features/auth/services/auth.facade.ts`
- Consolidation candidates:
  - `core/state/auth-state.service.ts` overlaps with `core/auth/current-user.store.ts`.
  - `core/auth/auth.models.ts`, `core/auth/auth.types.ts`, and `features/auth/models/auth.models.ts` should be reconciled into one backend-aligned auth contract surface.
  - `features/auth/models/user.models.ts` overlaps with shared/core user role typing.

### Workspace Context

- Keep as canonical:
  - `core/workspace/workspace.service.ts`
  - `core/workspace/workspace.store.ts`
  - `core/workspace/workspace.models.ts`
- Consolidation candidates:
  - `features/admin/workspace/services/workspace.service.ts`
  - `features/admin/workspace/state/workspace.store.ts`
  - `features/admin/workspace/models/workspace.models.ts`
- Rule for later batches: tenant/workspace context should be sourced from core and passed into feature stores, not re-created inside admin feature folders.

### Permissions

- Keep as canonical:
  - `core/permissions/permission.store.ts`
  - `core/auth/permissions.ts`
  - route guards in `core/guards`
- Consolidation candidates:
  - `core/permissions/prompt.permissions.ts` and prompt-specific guards should stay only if they are backend-context driven.
  - Any hardcoded approval, share, download, generated version, or prompt template availability must move behind backend context/capabilities.

### Theme

- Keep as canonical:
  - `core/theme/theme.store.ts`
  - `core/theme/theme.types.ts`
- No duplicate theme service was found in the active tree. Shared/public theme toggles should continue using `ThemeStore`.

### DTOs / Models

- Backend response standard exists as `shared/models/api-response.model.ts`.
- Duplicate model families to reconcile:
  - `features/admin/assets/models/asset.models.ts` and `features/assets/models/asset.models.ts`
  - `features/admin/prompts/models/prompt.models.ts` and `features/prompts/models/prompt.models.ts`
  - `features/prompts/models/prompt-api.models.ts` and older prompt DTOs
  - `features/admin/workspace/models/*` and `core/workspace/*`
- Rule for later batches: do not hardcode pricing plans, AI providers, plan limits, generated version limits, approval availability, or share/download availability in frontend models or constants.

### Shared UI

- Canonical shared primitives:
  - `shared/components/button`
  - `shared/components/card`
  - `shared/components/modal`
  - `shared/components/modal-shell`
  - `shared/components/loading`
  - `shared/components/empty-state`
  - `shared/components/page-header`
  - `shared/components/section-header`
- Cleanup candidates:
  - Feature-specific cards/forms should stay only when they express real feature behavior.
  - Repeated asset and prompt components under both `features/admin/*` and flatter `features/*` should be merged into the active flatter feature folders.

### Layouts

- Active protected shell:
  - `core/layout/protected-layout`
- Active public shell:
  - `features/public/home`
- Cleanup candidates:
  - `features/auth/components/auth-layout` and older public component set should be kept only if still referenced by active auth flows.
  - Older `features/admin`, `features/crew`, and `features/master` page components remain as cleanup candidates after their inactive route maps were removed.

### Forms

- Active Day 1 to Day 6 forms:
  - `features/brands/brands.ts`
  - `features/product-services/product-services.ts`
  - `features/projects/projects.ts`
  - `features/assets/components/asset-uploader`
  - `features/prompts/components/prompt-*`
- Duplicate candidates:
  - `features/admin/workspace/components/brand-profile-form`
  - admin asset and prompt forms/components duplicated under `features/admin/assets` and `features/admin/prompts`

### Routes

- Public route:
  - `/`
- Protected active routes after Batch 1:
  - `/dashboard`
  - `/brands`
  - `/product-services`
  - `/projects`
  - `/projects/:projectId/assets`
  - `/projects/:projectId/prompts`
  - `/projects/:projectId/creative-requests`
  - `/creative-requests/:creativeRequestId`
  - `/generated-versions/:generatedVersionId`
  - `/approvals`
- Login now opens from the homepage dialog using `/?auth=login`.
- `/login`, `/assets`, `/prompt-templates`, and `/projects/:projectId/prompts/history` were removed from the active protected/public route map.
- Master support placeholder routes from the prior shell were removed from the active route map because they are outside the requested Batch 1 protected route list.

### Loading / Empty / Error States

- Canonical shared components exist for loading and empty states.
- Feature-local skeletons and inline errors are still used in assets, prompts, brands, products, and projects.
- Later cleanup should standardize friendly empty/error messaging through shared components while keeping feature-specific copy.

## Naming Audit

- No `.component.ts`, `.component.html`, or `.component.css` filenames were found under `src/app`.
- Existing component files already follow the requested format, for example `brand-list.ts`, `brand-list.html`, `brand-list.scss` style naming without the `.component` suffix.
- No CSS component files were found in the audited app tree; components use SCSS.

## Angular 21 Template Syntax Audit

- Active HTML templates audited in this batch do not use `*ngIf`, `*ngFor`, or `*ngSwitch`.
- Existing refactored templates use `@if`, `@for`, and `@switch`.
- No inline component templates were found in `src/app` TypeScript files.

## Backend Alignment Risks To Fix In Later Batches

- Active homepage pricing-plan copy was removed; pricing, limits, and package names must come from backend subscription/billing APIs before any pricing UI is reintroduced.
- Homepage and auth dialog still contain hardcoded social/provider/platform labels for marketing UI. Provider and pipeline behavior must come from backend context before these become actionable.
- Approval/share/download/generated-version capabilities must be read from backend capabilities, not inferred in frontend route or UI code.

## Batch 2 Shared UI Cleanup

- Canonical shared primitives now live under the requested `app-*` shared component folders:
  - `shared/components/app-button`
  - `shared/components/app-card`
  - `shared/components/app-dialog`
  - `shared/components/app-drawer`
  - `shared/components/app-empty-state`
  - `shared/components/app-error-state`
  - `shared/components/app-loading-state`
  - `shared/components/app-status-badge`
  - `shared/components/app-page-header`
  - `shared/components/app-section-header`
  - `shared/components/app-context-breadcrumb`
  - `shared/components/app-confirm-dialog`
- Compatibility exports remain at the previous paths, for example `shared/components/button/button.ts`, so existing Day 1 to Day 6 feature imports keep working while new code can use the canonical `app-*` paths.
- `app-confirm-dialog` now uses the accessible `app-dialog` foundation instead of maintaining a second modal implementation.
- `app-drawer` was added as the canonical drawer shell and the active asset preview drawer now uses it.
- `app-error-state` and `app-context-breadcrumb` were added as shared primitives for consistent error and context display without creating feature screens.
- Reusable layout shells now sit under:
  - `shared/layouts/public-layout`
  - `shared/layouts/dashboard-layout`
  - `shared/layouts/auth-shell`
- Old layout import paths are compatibility exports only.

## Batch 3 Day 1 Public Homepage / Theme / Shell Cleanup

- Public homepage was reduced to the Day 1 product story:
  - `Lebhas - Brand Attire`
  - `Create Ads Beyond Imagination`
  - Raw product image to AI creative pipeline to campaign creative output
- Removed hardcoded pricing-plan content from the active homepage so pricing and limits can remain backend-driven in later billing work.
- Login remains a modal flow using the existing split `AuthDialogComponent`; no duplicate login page/component was added.
- Homepage theme usage is read from `ThemeStore`; toggling remains centralized through `ThemeToggleComponent`.
- Public visuals use existing product/apparel and abstract AI assets only:
  - `hero-raw-product-card.png`
  - `hero-ai-processing-icon.png`
  - `hero-campaign-creative-card.png`
- Dashboard route now imports the shared `dashboard-layout` foundation, while the old core layout path remains a compatibility export.
- Mobile homepage layout was simplified to avoid horizontal overflow and reduce scroll depth.

## Batch 4 Day 2 Auth / Workspace / Permission Foundation

- Active auth state now uses `CurrentUserStore` directly; the duplicate `AuthStateService` wrapper was removed.
- `AuthService` and `WorkspaceService` now unwrap the standard `ApiResponse<T>` through the shared `unwrapApiResponse` helper instead of duplicating response handling.
- Core `WorkspaceStore` now owns the active workspace context foundation:
  - workspace name
  - current role
  - `WorkspaceSubscription`
  - `PlanFeaturePolicy`
  - feature toggles
  - usage and remaining credits/limits where provided by backend context
- `WorkspaceService` includes a backend-driven workspace context call at `/api/v1/workspaces/:workspaceId/context`; missing context is handled quietly so existing Day 1 to Day 6 screens keep working while backend support evolves.
- Dashboard shell now shows the current workspace, role, package label, and remaining credits from the centralized workspace context signals.
- `PermissionStore` is the single active permission helper and now combines role/permission checks with optional `PlanFeaturePolicy`, `WorkspaceSubscription`, and feature toggle gating.
- HTTP error normalization now maps common backend failures to friendly user messages:
  - 403 permission denial
  - quota/storage exhaustion
  - plan-limit exceeded creative version limits
- Active homepage hardcoded pricing plan data was removed during this batch because subscription and pricing display must be backend-driven.

## Batch 5 Day 2 Dashboard / Brand / Product / Project UI

- Active dashboard overview is now action-first above the fold:
  - Create Brand
  - Create Product/Service
  - Create Project
  - Upload Asset
  - Create Prompt
- Dashboard now shows the SaaS hierarchy as `Workspace -> Brand -> Product/Service -> Project/Campaign` and uses centralized workspace subscription/limit signals for package, status, and remaining credits.
- Brand UI still uses the existing single modal form and now supports required `languagePreference`:
  - `BANGLA`
  - `ENGLISH`
  - `BOTH`
- Brand create defaults `languagePreference` to `BOTH`; list and detail surfaces show the friendly language label.
- Product/service create defaults to the first available brand and shows selected brand context inside the existing modal. Creating a product/service is disabled until a brand exists.
- Project/campaign create defaults to the first available product/service, derives the brand context, and shows both inside the existing modal. Creating a project is disabled until a product/service exists.
- Project/campaign form now uses simple select controls for objective and target platform defaults to reduce typing for ordinary users.
- Brand, product/service, and project stores now use friendly normalized API errors, success toasts, and keep modals open on failed submissions so user input is not lost.
- Brand/product/project services now use the shared `unwrapApiResponse` helper instead of duplicating response unwrapping.

## Batch 6 API Service Layer / Domain Model Cleanup

- Active domain API services were normalized to the requested `*-api.service.ts` naming:
  - `core/auth/auth-api.service.ts`
  - `core/workspace/workspace-api.service.ts`
  - `features/brands/brand-api.service.ts`
  - `features/product-services/product-service-api.service.ts`
  - `features/projects/project-api.service.ts`
  - `features/assets/services/asset-api.service.ts`
  - `features/prompts/services/prompt-api.service.ts`
- Added thin Day 1 to Day 6 API foundations for existing routed downstream domains without adding UI:
  - `features/creative-requests/creative-request-api.service.ts`
  - `features/generated-versions/generated-version-api.service.ts`
  - `features/approvals/approval-api.service.ts`
- Added matching dynamic models for creative requests, generated versions, and approvals. Capability fields such as approval, sharing, and download availability are optional backend-provided values, not frontend constants.
- Active API services now unwrap the standard `ApiResponse<T>` through `unwrapApiResponse` instead of manually parsing response bodies repeatedly.
- Prompt test fixtures no longer hardcode a specific AI provider/model name; provider/model values remain backend-owned.
- Active service URLs remain encapsulated inside API services and use the central `ApiService`, which applies `environment.apiBaseUrl`.
- Legacy admin service files remain in inactive admin folders because they are not part of the active Day 2 route graph; deleting them safely requires a separate inactive-admin cleanup pass.

## Batch 7 Day 3 Asset Management UI

- The active Day 3 asset route remains project-scoped at `/projects/:projectId/assets`; no future media-library module was added.
- The project asset page now reinforces the hierarchy context:
  - Workspace
  - Brand
  - Product/Service
  - Project/Campaign
- Preview and download actions continue to request backend signed URLs through `AssetApiService`; the UI no longer implies direct or local filesystem access.
- Upload failures from backend APIs now stay visible inside the upload dialog and the submit action becomes a retry action while preserving the selected file and form input.
- Asset list load failures now use the shared empty/error-state foundation with a clear retry action instead of a one-off inline alert.
- Workspace storage usage is shown only when backend workspace context exposes storage limit or remaining storage data. No frontend storage limit value is invented.
- Asset permission checks now route through the centralized `PermissionStore` instead of duplicating permission logic inside the asset store.
- Removed inactive duplicate asset route/page files under `features/assets`:
  - `assets.routes.ts`
  - `pages/asset-library/*`
  - `pages/asset-detail/*`
- The existing drag/drop uploader, responsive grid/list, loading, empty, filter, preview drawer, and signed URL download flow were preserved and refined rather than rewritten.

## Batch 8 Day 4 Prompt Intelligence UI

- The active project-scoped prompt builder remains at `/projects/:projectId/prompts`; no duplicate prompt route/page was added.
- The builder now keeps the full context visible above the workflow:
  - Workspace
  - Brand
  - Product/Service
  - Project/Campaign
- Brand language preference now drives the default prompt language:
  - `BANGLA` defaults to Bangla
  - `ENGLISH` defaults to English
  - `BOTH` leaves language selection open for the user
- Prompt setup is simplified to platform, campaign objective, and language first. Tone is hidden behind an advanced-settings disclosure.
- Selected assets, selected platform/objective/language, enhanced output, copy, reset, suggestions, and template saving stay in the existing builder flow.
- Prompt history is searchable inside the builder and opens detail in the existing modal panel instead of sending users to another page.
- Admin/Crew UI no longer exposes provider/model details in the builder or history cards. The visible message is: `Creative quality mode is controlled by your current package.`
- Prompt template saving reuses the existing `PromptTemplateStore`, `PromptApiService`, and `PromptTemplateForm`; the form can prefill from the current prompt output.
- Prompt history and template tests were updated to match the backend-driven provider policy.

## Batch 9 Day 5 Creative Request / Generated Version UI

- Replaced the project creative-request placeholder route with an active project-scoped UI at `/projects/:projectId/creative-requests`.
- Added one signal store for creative requests:
  - `features/creative-requests/creative-request.store.ts`
- Added one signal store for generated versions:
  - `features/generated-versions/generated-version.store.ts`
- The creative request flow uses a modal-guided form and shows:
  - selected project context
  - selected prompt from prompt history
  - selected assets
  - target platform
  - requested format
  - requested version count
  - estimated credits only when backend context exposes it
- Requested version validation reads workspace feature limits from backend-driven `WorkspaceStore`; no package names or numeric version limits are hardcoded.
- Version-limit overflow shows the required friendly message:
  - `Your current package allows X version(s) per request.`
- Async generation states are normalized into user-friendly labels:
  - Queued
  - Generating
  - Completed
  - Failed
  - Refunded
- Generated version cards show preview, status, and capability-driven actions. Download/share availability comes from backend-provided capabilities.
- Replaced the generated-version placeholder route with an active detail page at `/generated-versions/:generatedVersionId`.
- Advanced generated-version metadata is kept in detail views instead of cluttering the card UI.

## Batch 10 Day 6 Approval / Share / Download UI

- Replaced the `/approvals` placeholder route with an active approval queue UI.
- Added one signal store for approvals:
  - `features/approvals/approval.store.ts`
- Approval workflow availability now reads backend-driven workspace policy and feature toggles through `WorkspaceStore`; no frontend plan names or approval rules are hardcoded.
- Approve, reject, and request-changes actions are shown only when backend capabilities and workspace policy allow them.
- Approval comments use a modal flow so reviewers can act without leaving the queue.
- Approval history is shown as a clean timeline with friendly status labels.
- Sharing availability reads backend-driven `PlanFeaturePolicy` and generated-version capabilities. Disabled sharing shows a friendly package/subscription message instead of a technical error.
- Share links and download links now use generated-version signed URL APIs:
  - `GeneratedVersionApiService.getShareUrl`
  - `GeneratedVersionApiService.getDownloadUrl`
- Generated-version detail actions now call the signed URL helpers instead of rendering inert Share/Download controls.
- No local filesystem assumptions, social publishing UI, payment UI, or Day 7+ billing screens were added.

## Batch 11 Global UX Hardening

- Shared dialogs now use mobile-safe viewport sizing, own their internal scroll area, and keep header/content structure accessible on small screens.
- Shared drawers now restore focus, close on Escape, and expose dialog title/description IDs for assistive technology.
- The protected dashboard shell now prevents horizontal page overflow and improves focus styling on sidebar controls.
- Brand, product/service, and project rosters now render simple selectable cards on mobile while preserving existing table layouts for wider screens.
- Asset upload is now usable on mobile with full-height overlay sizing, internal scrolling, accessible labels, and dark/light surface colors.
- Creative-request generated-version actions now call signed backend share/download URL helpers from the existing generated-version store.
- Technical backend limit text such as `PlanFeaturePolicy.maxGeneratedVersionsPerRequest` is normalized into ordinary-user package-limit copy.

## Batch 12 Tests And Final Day 1 To Day 6 Refactor Notes

- Added `src/app/frontend-refactor.batch-12.spec.ts` as a Day 1 to Day 6 refactor guard suite.
- The new tests cover:
  - active auth/workspace/permission foundation uniqueness
  - no active `.component.*` or `.css` naming in Day 1 to Day 6 refactored code
  - no active `*ngIf` or `*ngFor` templates
  - public homepage default route and split login dialog
  - dashboard quick actions
  - brand language preference
  - product brand selection
  - project product/service selection
  - asset upload mobile/accessibility foundation
  - prompt builder responsive structure
  - creative request version-limit copy and signed share/download behavior
  - approval and public-share feature policy controls
  - crew/admin/master visibility hooks
  - workspace subscription, usage, feature policy, and dynamic limit loading
  - loading, empty, error, retry, and inline validation foundations
- Final shared component decisions:
  - Keep shared UI primitives only where they reduce repeated markup or state handling.
  - `app-dialog` and `app-drawer` own accessibility and mobile behavior.
  - `app-empty-state`, `app-loading-state`, buttons, badges, page headers, and cards remain the common visual foundation.
- Final state management decisions:
  - Auth stays in `core/auth`.
  - Workspace context, subscription, usage, and feature policy stay in `core/workspace`.
  - Permissions stay in `core/permissions` and combine role/permission checks with workspace feature policy.
  - Domain screens use one signal store per active domain where state is needed.
- Final API service decisions:
  - Active Day 1 to Day 6 domains use one API service per domain.
  - API services unwrap the backend `ApiResponse<T>` through shared response utilities.
  - Components do not own repeated backend response parsing.
  - Signed URL flows are used for asset and generated-version preview/download/share.
- Final UX simplification decisions:
  - Keep navigation shallow and use modals/drawers when they reduce route churn.
  - Keep forms short with required fields visible and advanced settings collapsed.
  - Use card/list layouts for ordinary workflows and reserve tables for wider screens or data-heavy views.
  - Keep hierarchy context visible across brand, product/service, project, asset, prompt, request, generated-version, and approval surfaces.
- Dynamic backend behavior:
  - No pricing package names, AI provider names, generated-version limits, approval availability, or sharing availability are hardcoded in active UI logic.
  - Workspace subscription, plan feature policy, usage, limits, approval availability, and share availability remain backend-owned.
  - Tests use mock dynamic backend responses only to prove policy wiring.
- Intentionally not implemented in Day 1 to Day 6:
  - Day 7+ billing/payment UI
  - social publishing UI
  - future monitoring/analytics modules
  - Master setup screens for AI provider/pipeline configuration unless already present in inactive legacy areas
  - full media library beyond the existing project-scoped asset workflow

## Day 7 Batch 1 AI Monitoring Foundation

- Added the Day 7 AI monitoring feature foundation under `features/ai-monitoring`.
- Added protected routes:
  - `/ai-monitoring`
  - `/ai-monitoring/providers`
  - `/ai-monitoring/layers`
  - `/ai-monitoring/workspaces`
  - `/ai-monitoring/quality`
  - `/ai-monitoring/failures`
- Extended the existing `PermissionStore`; no duplicate permission system was added.
- Added AI monitoring permission hooks:
  - `canViewAiMonitoring`
  - `canViewProviderMetrics`
  - `canViewLayerAnalytics`
  - `canViewWorkspaceAiUsage`
  - `canViewQualityScores`
  - `canViewAiFailures`
- Added lightweight shell pages only. They show friendly access-denied states and backend-driven data-policy copy without fake metrics.
- Added Master navigation for AI Monitoring, Provider Health, Layer Analytics, Workspace AI Usage, and AI Failures.
- Added Admin navigation for AI Usage and Quality Scores only when permission/policy allows.
- CREW navigation remains hidden unless explicit AI monitoring permissions are present.
- Added a thin `AiMonitoringApiService` and dynamic models for later Day 7 batches; no provider names, layer names, costs, quality values, limits, health statuses, or routing recommendations are hardcoded.
- Payment, checkout, billing UI, social publishing, setup/editing screens, and Day 8+ modules were intentionally not added.

## Day 7 Batch 2 AI Monitoring Models And API Service

- Replaced the generic Batch 1 AI monitoring summary placeholder with strict Day 7 backend-aligned models.
- Added enums:
  - `ProviderHealthStatus`
  - `AiFailureType`
  - `QualityScoreLabel`
- Added strict interfaces for:
  - `AiProviderMetric`
  - `AiProviderHealth`
  - `AiLayerAnalytics`
  - `WorkspaceAiUsage`
  - `AiQualityScore`
  - `AiFailureLog`
  - `WorkspaceGenerationAnalytics`
- Updated `AiMonitoringApiService` to expose one method per Day 7 backend endpoint.
- The service reuses the centralized `ApiService` and shared `unwrapApiResponse`; no duplicate HTTP or `ApiResponse<T>` handling was added.
- Provider names, layer names, model names, costs, quality values, statuses, limits, and recommendations remain backend-owned.

## Day 7 Batch 3 AI Monitoring Signal Store

- Added one AI monitoring signal store:
  - `features/ai-monitoring/state/ai-monitoring.store.ts`
- Store state includes provider metrics, provider health, layer analytics, workspace usage, quality scores, AI failures, date range, selected provider, selected layer, loading, and error.
- Added computed analytics for healthy/degraded/down providers, total cost, average quality score, provider comparisons, highest-cost workspace, and recent failures.
- Added master and workspace loader methods that reuse the single `AiMonitoringApiService`.
- Store actions respect the existing `PermissionStore`; no duplicate permission or state system was added.
- Friendly error mapping converts backend monitoring conditions into ordinary-user messages.
- Initial state remains empty arrays and null filters. No fake analytics or hardcoded provider/layer values were introduced.

## Day 7 Batch 4 AI Monitoring Shared Components

- Added reusable AI monitoring presentation components under `features/ai-monitoring/components`.
- Components reuse existing shared primitives such as `app-card`, `app-button`, `app-empty-state`, `app-loading-state`, `app-status-badge`, and `app-icon`.
- Added cards and badges for provider health, provider metrics, layer cost, quality score, AI cost summary, routing mode, and failure reasons.
- Added a keyboard-friendly AI analytics filter bar with date range, provider, layer, refresh, and clear controls.
- Added AI monitoring empty/loading states with ordinary-user copy and skeleton card foundations.
- Components are standalone, use `templateUrl`, `styleUrl`, SCSS, Tailwind utility classes, Lucide-backed icons, and Angular `@if`/`@for`.
- Provider and layer names remain input/backend-driven. No fake analytics, payment UI, checkout UI, social publishing UI, or Day 8+ modules were added.

## Day 7 Batch 5 AI Monitoring Main Dashboard

- Implemented the `/ai-monitoring` Master/System Owner dashboard in `features/ai-monitoring/dashboard`.
- The dashboard loads data through the single `AiMonitoringStore` and stays permission-aware through the existing `PermissionStore`.
- Added summary cards for total AI requests, total AI cost, average quality score, average generation time, provider failure rate, best performing provider, cheapest provider, most reliable provider, and highest cost workspace.
- Added dynamic routing comparison summaries for cheapest, fastest, best quality, most reliable, and best quality-to-cost provider using backend-provided metrics.
- Routing policy editing is intentionally not exposed in this monitoring dashboard.
- Backend routing recommendations are shown only when available; no frontend-only recommendation is invented.
- Added filter foundation, refresh action, loading/empty/error/retry states, and recent failure previews with friendly wording.
- The dashboard avoids Redis/Kafka terminology, hardcoded provider/layer names, fake analytics, payment UI, checkout UI, publishing UI, and Day 8+ modules.

## Day 7 Batch 6 Provider Metrics And Health UI

- Implemented provider health at `/ai-monitoring/providers`.
- Added provider metrics at `/ai-monitoring/providers/metrics`.
- Both pages load data through the existing `AiMonitoringStore`; no duplicate provider service, model, or store was added.
- Provider Metrics shows backend-provided provider name, model, total requests, successful requests, failed requests, average latency, average cost, average quality score, uptime, last success, last failure, and status context.
- Provider Health shows active, degraded, failed, and cooldown provider summaries, critical issues at the top, failure reason, recovery status, last checked time, fallback availability, and backend-provided recommended action.
- Both pages are permission-aware through `PermissionStore`, include refresh/filter foundations, and support loading, empty, error, and retry states.
- Detailed provider metrics use a table on desktop and cards on mobile; health details include cards plus a responsive detail table.
- No provider names, setup/edit controls, routing policy editing, fake analytics, payment UI, checkout UI, publishing UI, or Day 8+ modules were added.

## Day 7 Batch 7 Layer Analytics UI

- Implemented the `/ai-monitoring/layers` layer analytics page.
- The page uses `AiMonitoringStore` and the single `AiMonitoringApiService`; no duplicate layer service, model, or store was added.
- Added permission-aware access through the existing `PermissionStore`.
- Added backend-driven provider and layer filter foundations.
- Added summary cards for total executions, total layer cost, average quality, and failure count.
- Added layer cards plus a detailed table for layer name, layer type, selected provider, model name, total executions, success count, failure count, average execution time, average cost, and average quality score.
- Added friendly issue summaries for costly, slow, and failing layers:
  - This layer is costing more than expected.
  - This layer is slower than usual.
  - This layer failed several times recently.
- Layer routing policy setup/editing is intentionally not exposed in this monitoring page.
- No provider names, layer names, setup screens, fake analytics, payment UI, checkout UI, publishing UI, or Day 8+ modules were added.

## Day 7 Batch 8 Workspace AI Usage And Quality Scores UI

- Implemented `/ai-monitoring/workspaces` for workspace AI usage.
- Implemented `/ai-monitoring/quality` for generated-version quality scores.
- Both pages use `AiMonitoringStore`, `AiMonitoringApiService`, `PermissionStore`, and the existing `WorkspaceStore`; no duplicate workspace context logic was added.
- MASTER loads all workspace usage and quality score data.
- ADMIN and other permitted non-MASTER roles load only the active workspace by using the existing active workspace context.
- Workspace usage shows workspace name, backend-provided active plan, generation requests, generated versions, credits consumed, estimated AI cost, failures, average generation time, update list foundation, and high-cost warnings.
- Quality scores show generated-version references, overall score, text readability, product preservation, branding, Bangla typography, composition, notes, and created date.
- Quality labels are displayed only when backend sends `qualityLabel`; the frontend does not invent score bands.
- Loading, empty, error, retry, responsive cards, and friendly high-cost copy were added.
- No hardcoded plan names, provider names, fake analytics, payment UI, checkout UI, publishing UI, or Day 8+ modules were added.

## Day 7 Batch 9 AI Failure Recovery UI

- Implemented `/ai-monitoring/failures`.
- The page uses `AiMonitoringStore`, `AiMonitoringApiService`, `PermissionStore`, and existing `WorkspaceStore`; no duplicate failure service, model, or store was added.
- MASTER loads all AI failures through the master failures endpoint.
- ADMIN and other permitted non-MASTER roles load active-workspace generation analytics and show the backend-provided failures for that workspace.
- Added provider/layer filter foundation, urgent-first sorting, summary cards, provider/layer grouping, responsive failure cards, and a detailed failure table.
- Failure cards and tables show creative request reference, layer, provider, model, failure type, friendly failure reason, retry attempt, fallback status, and created date.
- Failure type wording is ordinary-user-friendly:
  - This tool took too long to respond.
  - This tool is temporarily busy.
  - This tool is currently unavailable.
  - This tool returned an unusable result.
  - Output quality was not good enough.
  - This request exceeded the allowed AI cost.
  - Something went wrong while generating this creative.
- Fallback status uses the requested copy: Backup tool was used / No backup tool was used.
- Manual retry actions are intentionally not implemented because no backend retry API exists in Day 7.
- No provider names, layer names, fake retry flow, fake analytics, payment UI, checkout UI, publishing UI, or Day 8+ modules were added.

## Day 7 Batch 10 Global UX Hardening

- Hardened Day 7 AI monitoring screens/components only.
- Desktop detail tables for provider health, layer analytics, and AI failures are hidden on smaller screens where responsive cards/lists already provide the same operational view.
- Kept provider metrics as desktop table plus mobile cards.
- Added ARIA labels to Day 7 provider status and routing mode badges.
- Preserved keyboard-friendly date/provider/layer filter controls and refresh/clear actions.
- Replaced technical monitoring phrases with friendly copy where they may surface in provider health or failure cards:
  - This provider is slower than usual.
  - This tool failed too many times recently.
  - AI cost increased unusually for this workspace.
- Failure cards no longer display raw backend failure text for known failure types; they use ordinary-user-friendly failure messages.
- Existing loading, empty, error, and retry states remain consistent across Day 7 pages.
- No Day 1 to Day 6 screens were changed, and no new design system, fake analytics, setup screens, payment UI, checkout UI, publishing UI, or Day 8+ modules were added.

## Day 7 Batch 11 Frontend Tests

- Added `features/ai-monitoring/ai-monitoring.day7.spec.ts`.
- Covered Day 7 signal-store behavior with mock backend responses:
  - cheapest provider
  - fastest provider
  - best quality provider
  - date range filter state
  - provider filter state
  - workspace usage, quality scores, and workspace generation analytics loading
- Added Day 7 UI guard coverage for:
  - MASTER dashboard access
  - CREW/no-permission hidden access state
  - provider metrics rendering
  - provider health status badge accessibility
  - dynamic layer analytics rendering
  - ADMIN workspace usage rendering
  - quality scores and Bangla typography display
  - AI failure list and fallback status copy
  - loading, empty, error, and retry states
  - mobile overflow-safe layout patterns
  - no hardcoded provider names in production AI monitoring files
  - no `*ngIf` or `*ngFor` in Day 7 templates
  - AI monitoring API service using shared `ApiResponse<T>` unwrapping
- Test mocks use sample backend values only inside the spec; production components remain backend-driven.
