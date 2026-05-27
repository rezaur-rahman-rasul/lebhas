# Lebhas Creative Maker Frontend

Final revised Day 1 frontend foundation for `Lebhas Creative Maker`, an AI-powered multi-tenant creative operating system for brands and marketing agencies.

## Stack

- Angular 21 standalone application
- Angular Signals and modern control flow (`@if`, `@for`, `@switch`)
- Tailwind CSS 3
- SCSS
- Lucide Angular icons
- Playwright end-to-end coverage

## Day 1 Scope

- Public landing page on `/`
- Login modal flow on `/login`
- Dark mode default with persistent light mode support
- Authentication UI foundation for `POST /api/v1/auth/login`
- Protected dashboard shell on `/dashboard`
- Workspace-aware frontend state foundation
- Role-aware sidebar foundations for `MASTER`, `ADMIN`, and `CREW`
- Shared UI components for buttons, inputs, password inputs, cards, badges, modals, navbar, sidebar, loading, empty states, and section headers

## Project Structure

```text
src/app/
  core/
    api/
    auth/
    guards/
    icons/
    interceptors/
    layout/
    state/
    theme/
    workspace/
  features/
    auth/
    dashboard/
    public/
    workspace/
  shared/
    components/
    layouts/
```

## Local Development

```powershell
npm.cmd start
```

Default local URL:

- `http://127.0.0.1:4200`

## Verification

Build the local configuration:

```powershell
npm.cmd run build:local
```

Run the Playwright suite:

```powershell
npm.cmd run e2e
```

The Day 1 Playwright coverage checks:

- default home route behavior
- product-focused public hero rendering
- dark/light theme behavior and persistence
- login modal open/close, validation, and password reveal
- responsive navbar and mobile sidebar behavior
- no horizontal overflow on mobile
- role-aware sidebar foundations for `MASTER`, `ADMIN`, and `CREW`
- shared component rendering expectations

## Backend Integration Notes

The frontend expects the backend envelope:

```json
{
  "success": true,
  "message": "",
  "data": {},
  "errors": [],
  "timestamp": ""
}
```

The current foundation wires:

- `Authorization: Bearer <token>`
- `X-Workspace-ID`
- `X-Correlation-ID`
- refresh-token session recovery
- normalized API error handling
- workspace list loading from `/api/v1/workspaces/me`

## Final UI Consistency Rules

- Logged-in pages render inside the shared dashboard shell and `dashboard-page-container`.
- Master navigation is grouped as Overview, AI Operations, System Monitoring, Usage & Billing, and Payments.
- Use the shared page header, error state, empty state, loading state, sidebar, and toast components as the only source of truth.
- Toasts are deduplicated by message context and limited to two visible notifications below the topbar.
- Backend-unavailable states should use friendly page-level copy with retry actions instead of raw exception text.

## Theme Token Rules

- Dark mode remains the default theme.
- Light mode is driven by `src/styles.scss` CSS variables such as `--color-canvas`, `--color-surface`, `--color-border`, `--color-ink`, `--color-muted`, `--color-input`, and `--color-dropdown`.
- Tailwind semantic colors map to those variables in `tailwind.config.js`; prefer `bg-surface`, `bg-panel`, `text-ink`, `text-muted`, and `border-border` over page-specific color patches.
- Shared shell, sidebar, dropdown, form, error, empty, loading, button, and badge styles should consume these tokens so dark and light mode stay aligned.
