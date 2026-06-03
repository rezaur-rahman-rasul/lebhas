# Backend Production Readiness Checklist

Status values: READY, NEEDS_FIX, DEFERRED, NOT_APPLICABLE

## 1. Configuration

| Item | Status | Notes |
|---|---|---|
| Java 21 | READY | Parent pom uses Java 21. |
| Profiles | READY | local/dev/staging/production profiles exist. |
| Secrets externalized | NEEDS_FIX | Production JWT now required; verify all deployment secrets. |
| CORS | READY | Config-driven allowed origins. |
| JWT | READY | Production guard rejects missing/default secret. |
| DB | NEEDS_FIX | PostgreSQL config exists; clean migration not verified here. |
| Redis | NEEDS_FIX | Config exists; outage behavior needs integration verification. |
| Kafka | NEEDS_FIX | Apache Kafka configured; DLQ/outbox incomplete. |
| R2 | READY | R2 provider and signed URL support exist. |
| Payment | NEEDS_FIX | Provider foundation exists; signature verification must be completed per provider. |
| AI provider | NEEDS_FIX | Registry exists; production mock is blocked, real provider setup remains required. |

## 2. Security

| Item | Status | Notes |
|---|---|---|
| Auth | READY | Register/login/refresh/logout foundations verified by code and tests. |
| RBAC | READY | Master controllers protected with `@PreAuthorize`. |
| Rate limiting | READY | Auth/prompt/generation limits configured. |
| Secret handling | NEEDS_FIX | Keep validating credential DTOs and logs. |
| File upload safety | READY | Validation and signed URL flow exist. |
| Public endpoints | NEEDS_FIX | Swagger and actuator security matchers remain too broad. |
| Audit logs | READY | Audit/activity modules and events exist. |

## 3. Data

| Item | Status | Notes |
|---|---|---|
| Migrations | NEEDS_FIX | Present V1-V50; clean run not verified due unavailable Docker/Postgres. |
| Indexes | READY | Broad workspace/status/reference indexes present. |
| Backup readiness | DEFERRED | Deployment/DB ops decision. |
| Data integrity checks | READY | Day 15 operations foundation present. |
| Soft delete strategy | READY | Many entities use deleted/deleted_at patterns. |

## 4. Reliability

| Item | Status | Notes |
|---|---|---|
| Retry policy | NEEDS_FIX | Generation retry exists; Kafka DLQ/outbox needs hardening. |
| Idempotency | NEEDS_FIX | Payment webhook idempotency exists; requires provider integration tests. |
| Kafka DLQ | NEEDS_FIX | Not fully verified. |
| Redis fallback | NEEDS_FIX | Some failures logged; full behavior needs tests. |
| Transaction safety | READY | Critical services use `@Transactional`. |
| Health/readiness | READY | Health/readiness endpoints exist; production hides details. |

## 5. Observability

| Item | Status | Notes |
|---|---|---|
| Logs | READY | Structured console pattern includes correlation/workspace. |
| Metrics | READY | Actuator metrics configured; public exposure must be restricted. |
| Correlation ID | READY | Common header and MDC pattern exist. |
| Audit | READY | Audit/activity modules exist. |
| Monitoring endpoints | READY | Master monitoring and operations controllers exist. |

## 6. Testing

| Item | Status | Notes |
|---|---|---|
| Unit tests | READY | `mvnw test` exits 0. |
| Integration tests | NEEDS_FIX | Docker unavailable; Testcontainers did not fully run. |
| Security tests | READY | Auth/security tests exist. |
| Contract tests | NEEDS_FIX | Expand for frontend endpoint matrix and pagination. |
| Migration tests | NEEDS_FIX | Needs clean PostgreSQL run. |

## 7. Deployment

| Item | Status | Notes |
|---|---|---|
| Docker readiness | DEFERRED | Not verified in this shell session. |
| Environment variables | NEEDS_FIX | Production requires explicit JWT/DB/Redis/R2/AI/payment values. |
| Startup order | NEEDS_FIX | DB/Redis/Kafka readiness must be validated. |
| Healthcheck | READY | Health/readiness endpoints exist. |
| No local filesystem dependency | READY | Production guard rejects LOCAL storage. |
