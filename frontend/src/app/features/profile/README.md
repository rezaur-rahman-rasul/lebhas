# Profile Management

Profile Management Batch 1 adds the frontend foundation for safe user profile, account settings, password, profile image, session, and security activity workflows.

## Implemented Scope

- Strict profile models and enums in `models/profile.models.ts`
- Profile API service in `services/profile-api.service.ts`
- Profile security helpers in `services/profile-security.ts`
- Signal store in `state/profile.store.ts`
- Permission helper foundation integrated into the existing `PermissionStore`

## Batch 2 Shared Components

- `profile-avatar`
- `profile-image-uploader`
- `profile-summary-card`
- `profile-settings-card`
- `password-strength-meter`
- `security-activity-card`
- `session-card`
- `profile-empty-state`
- `profile-loading-state`

The avatar component displays a safe backend profile image URL when available and falls back to initials for Master, Admin, and Crew users. The image uploader validates JPG, PNG, and WEBP files, validates size before emitting the selected file to a parent upload flow, previews the selected image with a temporary object URL, and never stores a signed URL or object key as UI text.

## Batch 3 Pages

- `/profile` renders the own-profile page with summary, profile information, account settings summary, security summary, profile photo foundation, and quick actions.
- `/profile/edit` renders the edit profile form for first name, last name, display name, phone number, job title, timezone, and locale.

The profile pages are own-profile only for Master, Admin, and Crew users. They use the existing auth/profile state and permission helpers instead of duplicating user state. Successful profile edits patch the shared current-user store through `ProfileStore`, so the topbar can update from the same source of truth.

## Batch 4 Profile Image Upload Flow

Profile photo upload now uses the backend-issued Cloudflare R2 signed URL flow:

1. The uploader validates JPG, PNG, and WEBP files and checks file size before upload.
2. `ProfileStore.uploadProfileImage(file)` requests a signed upload URL from `/api/v1/profile/me/profile-image/upload-url`.
3. The file is uploaded directly to the returned signed URL with upload progress.
4. The store confirms the upload through `/api/v1/profile/me/profile-image/confirm`.
5. The returned profile updates `profileImageUrl`, profile page avatars, and the topbar avatar through Angular Signals.
6. Temporary upload state is cleared after confirmation or removal.

Signed upload URLs are never shown in UI text, never stored persistently, and are sent only to the direct upload request. Raw R2 object keys remain backend-only. Profile image removal uses `DELETE /api/v1/profile/me/profile-image` and immediately returns the UI to initials fallback when backend confirms removal.

## Batch 5 Account And Security Pages

- `/profile/change-password` provides the change password form with visibility toggles, strength meter, match validation, and password fields kept only in form state.
- `/profile/settings` provides account settings for preferred language, theme preference, notification email, in-app notifications, and marketing email.
- `/profile/security` lists safe security activity summaries with activity type filtering.
- `/profile/sessions` lists current and other sessions with a confirm step before revoking non-current sessions.

Password values, access tokens, refresh tokens, session token values, provider secrets, and raw sensitive metadata must never be logged or rendered. Session management displays only safe device, masked IP, and timestamp summaries returned by backend.

## Batch 6 Topbar And Auth Integration

- The existing topbar user menu now reads display name, email, and avatar from the shared auth/profile state on first dashboard load.
- The dropdown includes `My Profile`, `Account Settings`, `Security Activity`, and `Logout` actions routed to `/profile`, `/profile/settings`, and `/profile/security`.
- Login, registration, invite acceptance, session restore, and token refresh initialize profile state after the auth session is available.
- Logout and expired-session cleanup clear in-memory profile state so another user never sees stale profile details.
- Profile updates and profile photo upload/removal continue to update the topbar through Angular Signals without a forced reload.

The login/register dialog keeps the existing split layout and profile-safe registration fields. Auth state remains the session source of truth, while profile state owns editable profile details and avatar URL. The frontend does not use localStorage profile image hacks, fake click refreshes, or forced page reloads.

## Batch 8 Master Read-Only Support View

- Added reusable `support-profile-view` under `components/support-profile-view`.
- The component loads safe Master support metadata from:
  - `GET /api/v1/master/users/{userId}/profile`
  - `GET /api/v1/master/users/{userId}/security-activity`
- It is read-only and intended for later integration into a Master user-management surface if that module is added.
- No standalone user-management module or route was created because no existing Master user-management UI exists in Day 1-10.

The support view is guarded by `canViewUserProfileSupport`. It shows safe avatar URL, fallback initials, display name, permitted email/phone fields, role/workspace context when returned, timezone, locale, updated date, and safe security activity summaries. It does not allow editing another user, changing another user's password, uploading another user's profile image, or revoking another user's sessions.

## Backend APIs

- `GET /api/v1/profile/me`
- `PUT /api/v1/profile/me`
- `PUT /api/v1/profile/me/settings`
- `POST /api/v1/profile/me/change-password`
- `POST /api/v1/profile/me/profile-image/upload-url`
- `POST /api/v1/profile/me/profile-image/confirm`
- `DELETE /api/v1/profile/me/profile-image`
- `GET /api/v1/profile/me/security-activity`
- `GET /api/v1/profile/me/sessions`
- `POST /api/v1/profile/me/sessions/{sessionId}/revoke`
- `GET /api/v1/master/users/{userId}/profile`
- `GET /api/v1/master/users/{userId}/security-activity`

## Security Rules

- Profile images use backend-issued signed upload URLs. The frontend does not perform local filesystem upload storage.
- Raw object keys, upload URLs, tokens, password hashes, refresh tokens, provider secrets, and payment secrets must never be displayed.
- Password fields are sent only to the password-change endpoint and are not stored in persistent state.
- Signed profile image upload URLs are held only as short-lived in-memory state.
- Own profile is the default scope. Master support access is read-only and guarded by existing role and permission state.
- Auth state is not duplicated. Safe profile updates patch the existing current-user store so shell/topbar consumers update from the same source.
- Master support profile views must never render password hashes, access tokens, refresh tokens, raw profile image object keys, provider secrets, payment secrets, or raw sensitive metadata.

## Batch 9 Final Verification

Profile Management is implemented as an own-profile feature with reusable support components, protected routes, topbar integration, safe cross-module user display, and a read-only Master support profile foundation. No separate auth/user/profile state copy is introduced; editable profile state lives in `ProfileStore`, and successful profile updates patch the existing current-user store for shell and topbar consumers.

Protected profile routes:

- `/profile`
- `/profile/edit`
- `/profile/change-password`
- `/profile/security`
- `/profile/sessions`
- `/profile/settings`

Backend APIs used:

- `GET /api/v1/profile/me`
- `PUT /api/v1/profile/me`
- `PUT /api/v1/profile/me/settings`
- `POST /api/v1/profile/me/change-password`
- `POST /api/v1/profile/me/profile-image/upload-url`
- `POST /api/v1/profile/me/profile-image/confirm`
- `DELETE /api/v1/profile/me/profile-image`
- `GET /api/v1/profile/me/security-activity`
- `GET /api/v1/profile/me/sessions`
- `POST /api/v1/profile/me/sessions/{sessionId}/revoke`
- `GET /api/v1/master/users/{userId}/profile`
- `GET /api/v1/master/users/{userId}/security-activity`

Permission rules:

- Master, Admin, and Crew users can view and manage only their own profile.
- Own password, settings, profile image, security activity, and sessions stay scoped to the authenticated user.
- Master support profile access is read-only and guarded by `canViewUserProfileSupport`.
- Direct another-user profile routes are not exposed.

R2 signed URL profile image flow:

- The frontend validates JPG, PNG, and WEBP files before upload.
- The backend returns the signed upload URL and upload session.
- The browser uploads directly to the signed URL without app auth headers.
- The frontend confirms the upload with the backend and uses only the returned safe `profileImageUrl`.
- Signed upload URLs and raw object keys are never rendered or persisted.

Topbar integration:

- The user menu reads display name, email, and avatar from shared auth/profile state on first load.
- `My Profile`, `Account Settings`, and `Security Activity` route to the protected profile pages.
- Profile edits and profile image changes update the topbar through Angular Signals without reloads or click refreshes.

Safe user display integrations:

- Existing creative, approval, usage, payment, notification, activity, and audit surfaces use safe display names, safe avatar URLs, and initials fallbacks where user context is available.
- Profile-related notifications use friendly labels for profile updates, profile photo updates, password changes, session revocation, and security activity.
- Audit metadata masking includes profile image object-key fields and password-like keys.

Test coverage summary:

- `profile.day9.spec.ts` covers protected route registration, own-profile loading, topbar navigation, profile summary/avatar behavior, edit profile validation and submission, settings persistence, password validation and API calls, signed URL profile image upload, upload progress, avatar removal fallback, security activity, sessions and revoke flow, profile notification labels, audit metadata masking, Master support read-only behavior, Crew own-profile access, standard API response handling, and final guardrails for Angular syntax, file naming, state duplication, fake upload logic, hardcoded image URLs, object-key exposure, and password/token persistence.

Known integration assumptions:

- Master support profile view remains reusable-only until a Master user-management surface exists.
- Email/SMS/push preference delivery is controlled by notification provider integrations outside this profile feature.
- Backend remains the source of truth for profile image URLs, session identity, security activity, and support-profile visibility.
