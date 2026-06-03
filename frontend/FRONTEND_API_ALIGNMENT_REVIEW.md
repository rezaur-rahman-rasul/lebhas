# Frontend API Alignment Review

## Summary

The frontend centralizes API paths in `src/app/core/api/api-endpoints.ts` and uses `ApiService` to prepend the configured base URL and preserve `/api/v1` paths. `ApiService` serializes query params while omitting `null` and `undefined`; payment and usage services also omit empty string filters. Most feature services unwrap the backend response wrapper through `unwrapApiResponse` or map `response.data`.

## Endpoint matrix

| Area | Used endpoint | Expected backend endpoint | Method | Request DTO | Response DTO | Status | Issue | Fix |
|---|---|---|---|---|---|---|---|---|
| Auth register | `/api/v1/auth/register` | `/api/v1/auth/register` | POST | `RegisterRequest` incl. `confirmPassword` | Auth session wrapper | READY | None found | Already aligned |
| Auth login | `/api/v1/auth/login` | `/api/v1/auth/login` | POST | `LoginRequest` | Auth session wrapper | READY | None found | Already aligned |
| Auth refresh | `/api/v1/auth/refresh` | `/api/v1/auth/refresh` | POST | refresh token | Auth session wrapper | READY | None found | Already aligned |
| Auth me | `/api/v1/auth/me` | `/api/v1/auth/me` | GET | none | current user wrapper | READY | Workspace name may be unavailable | Documented fallback |
| Profile | `/api/v1/profile/me/**` | `/api/v1/profile/me/**` | GET/PUT/POST/DELETE | profile/password/image DTOs | profile/session wrappers | NEEDS_FIX | Live signed image flow not verified | Requires backend runtime test |
| Workspace | `/api/v1/workspaces/my`, `/context` | Workspace controller routes | GET | workspace id | workspace context | READY | None found in static review | Already aligned |
| Brands | `/api/v1/workspaces/{id}/brands` | Brand controller routes | GET/POST/PUT/DELETE | brand DTOs | brand DTOs | READY | Not live-tested | Verify with backend |
| Products | `/api/v1/workspaces/{id}/product-services` | ProductService controller routes | GET/POST/PUT/DELETE | product/service DTOs | product/service DTOs | READY | Legacy alias exists in route only | Keep redirect only |
| Projects | `/api/v1/workspaces/{id}/projects` | Project controller routes | GET/POST/PUT/DELETE | project DTOs | project DTOs | READY | Legacy unlinked projects need clear UI | Partially handled |
| Assets | `/api/v1/workspaces/{id}/assets`, `upload-url`, `confirm` | Asset controller routes | GET/POST/PUT/DELETE | signed upload DTOs | asset DTOs | READY | Live R2 upload not verified | Requires integration test |
| Prompt builder | `/projects/{id}/prompts/**` | Prompt builder routes | GET/POST | prompt DTOs | prompt context/draft DTOs | READY | Enhancement contract not live-tested | Verify with backend |
| Generation | `/creative-requests/**`, `/generation-jobs/**` | Creative request/generation routes | GET/POST | request/generation DTOs | job/version DTOs | READY | Queue flow not live-tested | Verify with backend |
| Approvals | `/approvals/generated-versions`, generated version approval actions | Approval/version routes | GET/POST | approval action DTOs | version/history DTOs | NEEDS_FIX | Tests still flag error copy/toast consistency | Triage UI tests |
| Billing/payments | `/workspaces/{id}/payments`, `/credits/purchase`, master payment routes | Billing routes | GET/POST/PUT/PATCH | payment/provider/package DTOs | payment DTOs | NEEDS_FIX | Tests flag navigation label expectations | Triage navigation |
| Usage | `/workspaces/{id}/usage-*`, `/master/usage/**` | Usage routes | GET | filters | usage DTOs | NEEDS_FIX | Tests flag missing current month wording | Triage UI |
| Master monitoring | `/master/ai/**`, `/master/audit-logs`, health routes | Master monitoring routes | GET/POST | filters/settings | monitoring DTOs | NEEDS_FIX | Master taxonomy inconsistency | Reconcile routes/nav |
| Notifications | `/workspaces/{id}/notifications/**` | Notification routes | GET/POST | notification prefs | notification DTOs | READY | Not live-tested | Verify with backend |

## Wrapper handling

Expected success wrapper:

```json
{ "success": true, "message": "...", "data": {}, "timestamp": "..." }
```

Expected error wrapper:

```json
{ "success": false, "message": "...", "data": null, "errors": [] }
```

Status: mostly READY. `normalizeHttpError` and field-error mapping are used by auth flows; service-level `unwrapApiResponse` usage is consistent in reviewed services.

## Key API risks

- Backend runtime was not available, so method/body/path checks are static only.
- Some frontend tests still report payment, usage, profile, and approvals mismatch expectations.
- Empty list vs error handling appears implemented in several stores, but not exhaustively browser-verified.
