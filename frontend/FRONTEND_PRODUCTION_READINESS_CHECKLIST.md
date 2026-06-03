# Frontend Production Readiness Checklist

| Section | Status | Notes |
|---|---|---|
| Routing | NEEDS_FIX | MASTER/admin route isolation fixed; master nav taxonomy still inconsistent. |
| Auth | READY | Login/register integration present; register sends `confirmPassword`. |
| RBAC | NEEDS_FIX | Guards improved; permission visibility still needs browser/API verification. |
| API alignment | NEEDS_FIX | Centralized endpoints align statically; live backend verification pending. |
| UI consistency | NEEDS_FIX | Static tests still flag dashboard, profile, usage, master nav, and theme consistency. |
| Theme | NEEDS_FIX | Tokens exist; light-mode tests still fail. |
| Accessibility | NEEDS_FIX | Basic labels present; keyboard/focus/browser checks deferred. |
| Responsiveness | NEEDS_FIX | Responsive CSS exists; requested viewport matrix not fully tested. |
| Error handling | READY | Toast dedupe and page-level error strategy exist; approvals tests still flag copy. |
| Testing | NEEDS_FIX | `npm run test` failed: 28 failed, 169 passed. |
| Build | READY | `npm run build` passed with warnings. |
| Deployment | NEEDS_FIX | Initial bundle exceeds 500 kB warning; no deployment smoke test performed. |
| Security | NEEDS_FIX | Route guards improved; token/storage/secret behavior needs final review. |

## Commands run

| Command | Result |
|---|---|
| `npm run build` | READY: passed with warnings. |
| `npm run test` | NEEDS_FIX: 28 failed, 169 passed. |
| `npm run lint` | NEEDS_FIX: missing script. |
| Angular syntax scan | READY: active legacy syntax not found; matches were test assertions. |

## Build warnings to address

- Initial bundle: 506.56 kB, above 500 kB warning budget.
- Several component styles exceed the 4 kB warning budget.
