# Day 10 Audit Foundation

Project: Lebhas - Brand Attire

## Scope

Day 10 Batch 1 adds the frontend foundation for workspace audit logs and sensitive metadata display helpers.

Implemented files:

```text
src/app/features/audit/
|-- models/audit.models.ts
|-- services/audit-api.service.ts
|-- services/sensitive-metadata.ts
|-- state/audit.store.ts
`-- README.md
```

## APIs

- `GET /api/v1/workspaces/{workspaceId}/audit-logs`

The service reuses the existing shared `ApiService`, auth interceptors, base URL handling, and `ApiResponse<T>` unwrap helper.

## State

`AuditStore` owns audit state with Angular Signals:

- audit logs
- selected module
- selected severity
- selected action
- selected date range
- loading
- error

## Sensitive Metadata

The `sensitive-metadata.ts` helper provides:

- `maskSensitiveMetadata(value)`
- `summarizeIpAddress(value)`
- `summarizeUserAgent(value)`
- `safeMetadataPreview(value)`

Future audit UI must not expose provider credentials, payment secrets, webhook payloads, JWT/session values, or raw sensitive metadata.

## Rules

- No pages or shared UI components are included in Batch 1.
- No fake audit data is added.
- No external monitoring integration is implemented.
- Workspace isolation is enforced by workspace-scoped API paths.

## Implemented In Batch 5

Batch 5 adds the workspace audit log UI:

```text
src/app/features/audit/
|-- components/
|   |-- audit-log-card/
|   |-- audit-severity-badge/
|   |-- audit-filter-bar/
|   |-- audit-detail-drawer/
|   |-- audit-empty-state/
|   `-- audit-loading-state/
|-- logs/
|   |-- audit-logs.ts
|   |-- audit-logs.html
|   `-- audit-logs.scss
`-- audit.routes.ts
```

Route:

- `/audit-logs`

The page uses the existing `AuditStore` and workspace context. It loads audit logs through `AuditStore.loadAuditLogs(workspaceId)` and filters by module, action, severity, actor, and date range.

Permission behavior:

- `ADMIN` can view workspace audit logs when `canViewAuditLogs` allows it.
- `MASTER` can view audit logs when the backend and feature policy allow it.
- `CREW` is hidden unless explicitly allowed.
- Unauthorized users see friendly access-denied copy without raw permission keys.

Security notes:

- Audit metadata is labeled as `Activity details`.
- Sensitive metadata is masked through `sensitive-metadata.ts`.
- Raw secrets, provider credentials, payment secrets, webhook payloads, JWT values, and session data are not exposed.
- No fake audit data or Day 11+ modules are included.

## Implemented In Batch 8

Batch 8 integrates audit logs into protected navigation and routing:

- `/audit-logs`

Navigation rules:

- `ADMIN` sees audit logs when `canViewAuditLogs` allows it.
- `MASTER` sees audit logs when the backend and feature policy allow it.
- `CREW` is hidden unless explicitly allowed.
- Unauthorized users see: `You do not have access to audit logs. Please contact your workspace admin if you need access.`

Security rules remain unchanged:

- Sensitive metadata stays masked.
- Raw webhook payloads, secrets, provider credentials, payment secrets, JWT values, and session data are not exposed.
- No fake audit data or Day 11+ modules are included.
