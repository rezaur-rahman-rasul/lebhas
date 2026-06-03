# Go-live Readiness API

`GET /api/v1/master/go-live-readiness`

Returns readiness counters and backend-computed checks:

- `ready`
- `needsAttention`
- `blocked`
- `checks[]` with `key`, `title`, `description`, `status`, `severity`, `relatedRoute`, and `lastCheckedAt`

Provider, routing, payment, audit, monitoring, and security checks are computed from backend configuration where available.
