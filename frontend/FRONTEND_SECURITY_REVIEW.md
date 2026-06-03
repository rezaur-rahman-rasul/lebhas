# Frontend Security Review

## Token handling

Status: NEEDS_REVIEW.

Access and refresh tokens are handled by `TokenStorageService` using localStorage/sessionStorage depending on persistence. This is intentional in the current architecture, but it remains XSS-sensitive. No token logging was found in the reviewed frontend sources.

## Route guards

Status: IMPROVED.

Fixed: authenticated MASTER users are now redirected away from normal admin routes unless accessing `/master/**` or `/audit-logs`. ADMIN and CREW users are redirected away from `/master/**`. `roleGuard` now redirects denied users to the default route for their current role.

## Role visibility

Status: PARTIAL.

Sidebar visibility is role and permission filtered. Backend must remain authoritative because hidden buttons are not security controls.

## Secret handling

Status: PARTIAL.

Static review did not find obvious provider/payment secrets rendered intentionally. Provider configuration forms still need explicit masked field verification in browser.

## Signed URL handling

Status: PARTIAL.

Asset upload uses a signed upload URL request and skips auth/app headers for the direct storage upload. Signed URLs are not intentionally logged in reviewed code. Live R2 flow was not verified.

## XSS risks

Status: READY in reviewed areas.

No unsafe `innerHTML` usage was found in the targeted scan. User-visible strings are rendered through Angular bindings.

## Public share risks

Status: NEEDS_REVIEW.

Public share endpoints exist in the endpoint map. The public share UI and token scoping were not fully exercised in this audit pass.

## Logging safety

Status: PARTIAL.

No obvious active `console.log` of secrets was identified in reviewed files. A broader scan should be kept in CI.

## Remaining security risks

- Token storage in web storage increases blast radius for XSS.
- Live backend 401/403 behavior was not exercised.
- Payment/provider secret redaction requires browser and API verification.
