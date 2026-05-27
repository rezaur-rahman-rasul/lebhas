# Dashboard Sidebar Structure

The dashboard sidebar uses one component:

`src/app/shared/layouts/dashboard-layout/components/app-sidebar/`

Navigation is centralized in `dashboard-navigation.ts`. The dashboard layout filters those items by the current role and the existing permission signals, then passes the allowed items into the sidebar.

Sidebar behavior covered here:

- consistent desktop width and compact width from `LayoutStateService`
- mobile drawer state from the existing dashboard layout backdrop/open button
- parent-route active matching for child pages such as `/brands/:id` and `/projects/:id/assets`
- one access scope card pinned below the scrollable navigation
- long brand, workspace, and navigation text truncation
- Lucide icons through the shared `IconComponent`
- no duplicate sidebar item component or duplicate navigation config

The sidebar intentionally does not add unimplemented future routes. New navigation items should be added to `dashboard-navigation.ts` only after the matching route exists and the existing permission foundation can control visibility.

## Initial Render Flow

The dashboard shell renders its `router-outlet` eagerly. Route content is not wrapped in `@defer`, because the protected dashboard must settle from auth, workspace, permission, and dashboard store signals without waiting for idle work or user interaction.

After login or refresh, the auth guard restores the user session, the shell initializes `WorkspaceStore`, and `DashboardOverviewComponent` loads dashboard data from `DashboardStore` when an active workspace id is available. The dashboard shows the intentional skeleton while auth, workspace context, or dashboard data is still loading. Once the store reports ready, quick actions, hierarchy cards, package fallback text, and stats render automatically.

Missing subscription or usage data is treated as unavailable state, not as a loading state:

- Package details unavailable
- Usage details unavailable

Dashboard layout fixes must keep this flow store-driven. Do not use click handlers, `setTimeout`, deferred router outlet rendering, manual DOM refreshes, or forced change detection to make dashboard content appear.

## Master Shell Stability

MASTER users can land on the dashboard before choosing a workspace. The shell still keeps the same first-render structure:

- `WorkspaceStore.initialize()` is triggered from an auth-driven signal effect so delayed auth restoration still loads workspace options.
- the workspace switcher always reserves the same desktop width and renders `Loading workspaces`, `Select workspace`, or loaded options inside the same control.
- the user profile slot keeps a stable width and shows fallback copy (`User`, `Email unavailable`) until the authenticated profile is available.
- the sidebar keeps one scrollable navigation area with a stable scrollbar gutter and the access scope card pinned outside that scroll area.
- the dashboard outlet stays eager so Master dashboard cards, package unavailable copy, and stats render without waiting for a click.

## Admin UI Consistency Rules

Admin workspace screens keep the same shell, page header rhythm, and hierarchy order: Workspace -> Brand -> Product/Service -> Project/Campaign. Dashboard quick actions must be hierarchy-aware: products require a brand, projects require a product or service, and asset or prompt actions require a project.

Create/edit dialogs use the shared `app-modal` shell with a sticky header and a sticky `.admin-modal-footer` so Cancel and submit actions remain visible on desktop and mobile. Product and project create flows should not open when their required parent layer is missing; show disabled controls with a clear reason instead.

Page-level load failures should render the shared empty/error state in the page body and avoid duplicate toast notifications. Permission-sensitive Admin routes, especially Payments, should either be hidden by the centralized navigation permission key or render the standard access-denied state inside the page header layout.
