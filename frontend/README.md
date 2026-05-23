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
