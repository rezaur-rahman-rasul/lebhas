# Backend Security Review

Overall status: NEEDS_FIX before production approval.

## Critical Issues

| ID | Issue | Status |
|---|---|---|
| SEC-C-01 | Development JWT secret fallback was usable unless overridden. | Fixed for production profile with `ProductionReadinessGuard` and production YAML override. |
| SEC-C-02 | Local filesystem storage could be selected by configuration. | Fixed for production profile; LOCAL storage now fails startup in production. |
| SEC-C-03 | Mock AI generation was enabled by default. | Fixed for production profile; production startup fails if mock is enabled. |

## High Issues

| ID | Issue | Risk | Recommendation |
|---|---|---|---|
| SEC-H-01 | `/swagger-ui/**` and `/v3/api-docs/**` are permitted in `SecurityConfiguration`. | API discovery exposure. | Require auth or profile-gate security matchers; production springdoc is disabled by default. |
| SEC-H-02 | `/actuator/**` is permitted. | Metrics/info exposure if enabled. | Permit only health probes publicly; move metrics behind auth/network controls. |
| SEC-H-03 | Payment webhooks are public and provider verification is foundation-level for some clients. | Forged callbacks could allocate credits if miswired. | Require provider-specific signature verification and idempotency tests before production. |
| SEC-H-04 | Kafka publish failures can be swallowed. | Audit/activity/credit event loss. | Use outbox and DLQ for critical events. |

## Medium Issues

| ID | Issue | Recommendation |
|---|---|---|
| SEC-M-01 | Local signed asset access endpoint remains for local/test mode. | Keep disabled in production; guard now enforces that. |
| SEC-M-02 | Some logs include storage keys and provider/model names. | Ensure no signed URLs, tokens, API keys, or prompts are logged. |
| SEC-M-03 | Public share token endpoints require ongoing entropy and expiry tests. | Add brute-force and expiry contract tests. |
| SEC-M-04 | Rate limiting exists for auth/prompt/generation but needs production load validation. | Validate Redis-backed limits under concurrency. |

## Low Issues

| ID | Issue |
|---|---|
| SEC-L-01 | Mockito dynamic agent warnings in tests should be addressed for future JDK compatibility. |
| SEC-L-02 | Local script `scripts/init-local-postgres.sql` contains local password `admin`; acceptable only for local tooling. |

## Fixed Issues

- Added production startup guard for JWT secret, local storage, and mock AI generation.
- Disabled Swagger/OpenAPI by default in production config.
- Changed production health details to `never`.

## Auth Security

- BCrypt strength 12 is configured.
- Register validates `confirmPassword` at DTO and service level.
- Login blocks inactive/locked users and records failed attempts.
- Refresh token rotation uses Redis lock and server-side token state.
- Logout revokes refresh/session state and current access token ID where available.

## RBAC and Tenant Isolation

- Method security is enabled.
- Master controllers found in source have `@PreAuthorize`.
- Workspace membership is resolved during login and workspace actions.
- Most tenant repositories include `workspaceId`; continue adding negative IDOR tests.

## Secret Handling

- R2/payment/AI credentials are externalized through config/properties.
- Payment and AI credential services include encryption/write-only response foundations.
- Do not expose credential-bearing DTOs in frontend-facing APIs.

## Upload Security

- Asset and profile image signed URL flows exist.
- File type/content and size validation services exist.
- Production now prevents LOCAL storage provider.

## Public Endpoint Security

- Public endpoints should be limited to auth bootstrap, public pricing/packages, public share, webhooks, and health probes.
- Swagger and broad actuator exposure are the main remaining public-surface issues.

## Deferred Issues

- Full penetration/DAST testing.
- Provider-specific payment signature verification certification.
- CI-backed Testcontainers migration/security tests.
