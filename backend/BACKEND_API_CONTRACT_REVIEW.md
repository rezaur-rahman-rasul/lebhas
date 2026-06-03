# Backend API Contract Review

Base prefix reviewed: `/api/v1`  
Common success shape: `ApiResponse<T>` with `success`, `message`, `data`, `errors`, `timestamp`  
Common error shape: `ApiResponse<Void>` with structured `ApiError` entries

## Endpoint Matrix

| Module | Endpoint | Method | Status | Issue | Fix |
|---|---|---:|---|---|---|
| Auth | `/api/v1/auth/register` | POST | READY | Requires `confirmPassword`; DTO and service validate mismatch. | No change. |
| Auth | `/api/v1/auth/login` | POST | READY | Role is returned through `UserView`; Master remains role `MASTER`. | No change. |
| Auth | `/api/v1/auth/refresh` | POST | READY | Rotates refresh token and reissues access token. | No change. |
| Auth | `/api/v1/auth/logout` | POST | READY | Requires auth and revokes current token/session. | No change. |
| Auth | `/api/v1/auth/logout-all` | POST | READY | Requires auth and revokes all user refresh sessions. | No change. |
| Auth | `/api/v1/auth/me` | GET | READY | Requires bearer token. | No change. |
| Auth | `/api/v1/auth/forgot-password` | POST | NEEDS_FIX | Foundation response only; no reset delivery. | Implement reset token workflow before production. |
| Auth | `/api/v1/auth/reset-password` | POST | NEEDS_FIX | Foundation response only. | Implement token validation and password update. |
| Auth | `/api/v1/auth/verify-email` | POST | NEEDS_FIX | Foundation response only. | Implement verification token persistence. |
| Workspace | `/api/v1/workspaces/**` | GET/POST/PATCH | READY | Workspace-scoped services and membership checks are present. | Continue contract tests. |
| Workspace | `/api/v1/master/workspaces/**` | GET/POST/PATCH | READY | Master controllers have `@PreAuthorize`. | No change. |
| Master support | `/api/v1/master/support-mode/**` | POST/DELETE | READY | Explicit Master support mode exists and is auditable. | No change. |
| Brand | `/api/v1/workspaces/{workspaceId}/brands/**` | CRUD | READY | Workspace-scoped repositories/services used. | No change. |
| Product/service | `/api/v1/workspaces/{workspaceId}/product-services/**` | CRUD | READY | Brand relationship validation exists. | Add cross-workspace contract tests if missing. |
| Project/campaign | `/api/v1/workspaces/{workspaceId}/projects/**` | CRUD | READY | Product/brand hierarchy exists. | Add negative tests for cross-workspace linking. |
| Assets | `/api/v1/workspaces/{workspaceId}/assets/**` | CRUD | READY | R2 signed upload/download foundation present. | Production blocks LOCAL storage now. |
| Local asset access | `/internal/storage/local/assets/**` | GET | DEFERRED | Binary response, not ApiResponse, public signed local path. | Keep local/test only; production guard blocks LOCAL. |
| Prompt | `/api/v1/workspaces/{workspaceId}/prompt/**` | GET/POST | READY | Workspace/project-scoped prompt context, drafts, templates present. | Verify frontend paths. |
| Generation | `/api/v1/workspaces/{workspaceId}/creative-requests/**` | POST/GET | NEEDS_FIX | Credit reservation exists; duplicate generation/outbox needs stronger verification. | Add concurrency integration tests. |
| Generated versions | `/api/v1/workspaces/{workspaceId}/generated-versions/**` | GET/PATCH | READY | Approval/download/share foundations present. | Continue contract tests. |
| Approval | `/api/v1/workspaces/{workspaceId}/approvals/**` | POST/GET | READY | Status transition checks exist. | Add unauthorized transition tests. |
| Sharing public | `/api/v1/public/share/**` | GET/POST | READY | Public endpoint intentionally permitted. | Ensure response hides workspace metadata. |
| Usage/billing | `/api/v1/workspaces/{workspaceId}/usage/**` | GET | NEEDS_FIX | Some repository methods are unpaginated. | Enforce page DTOs for large histories. |
| Pricing | `/api/v1/pricing-plans/public` | GET | READY | Public plan listing exists. | No hardcoded plan names found in code path reviewed. |
| Payment | `/api/v1/workspaces/{workspaceId}/payments/**` | POST/GET | NEEDS_FIX | Foundation providers exist; webhook verification may be provider-pending. | Require provider-specific signature verification before production. |
| Payment webhook | `/api/v1/payments/webhooks/**` | POST | NEEDS_FIX | Public webhook route is expected, but verification completeness depends on provider client. | Enforce signature verification for each enabled provider. |
| AI provider master | `/api/v1/master/ai/**` | CRUD | READY | Master controllers protected; credential views hide secrets. | Add response snapshot tests. |
| Monitoring/ops | `/api/v1/master/operations/**` | GET/POST | READY | Master-only operations exist. | Confirm smoke tests do not mutate production data. |
| Health/readiness | `/health`, `/readiness`, `/actuator/health` | GET | READY | Health endpoints exist. | Production health detail hidden by config. |
| Swagger/OpenAPI | `/swagger-ui/**`, `/v3/api-docs/**` | GET | NEEDS_FIX | Security chain permits them globally. | Production disables springdoc by default; also restrict matcher by profile/auth. |

## Missing or Incomplete Endpoints

- Fully implemented forgot/reset password and email verification workflows.
- Provider-specific payment webhook verification and callback settlement for every payment provider.
- Full production AI generation provider routing without mock fallback.

## Role and Workspace Scope Notes

- All `Master*Controller` classes found in source have `@PreAuthorize`.
- Workspace-owned repository methods generally include `workspaceId`.
- Master login response preserves `MASTER`, so the backend does not force Master into Admin routing.
- Public share endpoints are intentionally unauthenticated and must remain metadata-minimal.

## Frontend Integration Notes

- Frontend register must send `confirmPassword`.
- Frontend should branch Master users by role `MASTER`.
- Empty list endpoints should continue returning successful `ApiResponse` with empty arrays/page DTOs, not 500.
- Pagination contract should be standardized before large usage/audit/history screens ship.
