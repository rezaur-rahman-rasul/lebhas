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
