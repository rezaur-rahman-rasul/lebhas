# Backend QA Audit Report

Project: Lebhas - Brand Attire  
Slogan: Create Ads Beyond Imagination  
Audit date: 2026-06-01

## Executive Summary

Status: NEEDS_FIX before production approval.

The backend compiles and the Maven test suite exits successfully. Core foundations exist for auth, RBAC, workspace tenancy, assets/R2, prompts, generation, credits, approvals, sharing, usage, billing, payment, AI provider registry, audit/activity, notifications, and operations. The codebase is broad and modular, but it is not production-approved because several critical readiness concerns remain: public Swagger/API docs and actuator routing policy need profile hardening, local storage support remains in the codebase for test/local mode, AI mock generation is enabled by default outside production, Testcontainers-backed verification did not actually run because Docker is unavailable, and multiple list/reporting repositories expose unpaginated methods that are risky at scale.

## What Was Reviewed

- Maven parent and modules: common-lib, gateway-service, auth-service, user-service, workspace-service, creative-service, billing-service, notification-service.
- Controllers under `/api/v1`, auth contract, common ApiResponse, global exception handling, security filter chain.
- Migrations V1 through V50 under `common-lib/src/main/resources/db/migration`.
- Storage, R2 signed URL support, local storage fallback, asset upload validation.
- Generation job, credit reservation/finalization/refund, Kafka topic constants.
- Payment provider foundation, webhook/idempotency classes, credential encryption classes.
- Master controllers, RBAC annotations, workspace-scoped repositories.
- Redis/Kafka configuration and production profile settings.

## Critical Findings

| ID | Finding | Evidence | Status |
|---|---|---|---|
| C-01 | Production startup previously allowed development JWT secret fallback. | `application-common.yaml` had a default `JWT_SECRET_BASE64` value. | Fixed with production override and startup guard. |
| C-02 | Production startup previously did not reject local filesystem storage if `STORAGE_PROVIDER=LOCAL`. | `StorageProvider.LOCAL`, `LocalStorageService`, `LocalAssetAccessController`. | Fixed for production profile with startup guard. |
| C-03 | AI mock provider was enabled by default and generation worker uses `MockCreativeGenerationProvider`. | `platform.ai.generation.mock.enabled` default was true; `GenerationWorkerService` depends on mock provider. | Fixed for production profile with startup guard; still allowed in local/dev. |

## High Findings

| ID | Finding | Evidence | Recommendation |
|---|---|---|---|
| H-01 | Swagger/OpenAPI endpoints are globally permitted in the security chain. | `SecurityConfiguration.PUBLIC_ENDPOINTS` includes `/swagger-ui/**` and `/v3/api-docs/**`. | Gate by profile or require Master auth outside local/dev. Production YAML now disables springdoc by default, but security matcher still permits if enabled. |
| H-02 | Actuator endpoints are globally permitted. | `SecurityConfiguration.PUBLIC_ENDPOINTS` includes `/actuator/**`. | Permit only health/liveness/readiness publicly; require auth for metrics/info or expose on management network only. Production health details now set to `never`. |
| H-03 | Docker/Testcontainers unavailable, so DB/Kafka/Redis integration tests did not provide full dependency verification. | `mvnw test` logs: no valid Docker environment. | Run CI with Docker/Testcontainers and real PostgreSQL, Redis, Kafka. |
| H-04 | Kafka publish failures are logged and swallowed in several flows. | `AuthenticationService.publishSafely`, asset publish catch blocks. | Add transactional outbox or durable retry for important events. |
| H-05 | Some list/reporting repository methods are unpaginated. | `findAllByWorkspaceIdOrderByCreatedAtDesc`, master aggregation methods. | Enforce pagination for usage, audit, payment, notification, and asset listing paths. |

## Medium Findings

| ID | Finding | Notes |
|---|---|---|
| M-01 | Local storage access endpoint remains in code for local mode. | Acceptable for local/test only; production guard now blocks LOCAL provider. |
| M-02 | Payment webhook verification foundation may return PENDING for provider-specific verification. | Do not enable production webhooks until provider clients verify signatures. |
| M-03 | Migrations include legacy/default data repair inserts. | Intended for migration compatibility, but not a clean seed-free production posture. Review before greenfield production migration. |
| M-04 | Response body contract is mostly centralized with `ApiResponse`, but binary local asset responses and health responses use `ResponseEntity`. | Health is acceptable; local binary path is local/test only. |
| M-05 | Some modules still expose "foundation_pending" responses. | Auth reset/email verification and some Master layer operations are foundations, not complete features. |

## Low Findings

| ID | Finding |
|---|---|
| L-01 | `mvn` is not installed on PATH; wrapper must be used. |
| L-02 | Mockito dynamic agent warnings appear on Java 21+. |
| L-03 | Some endpoint/table names include legacy day-specific migration/module naming. |

## Fixed Issues

- Added `ProductionReadinessGuard` to fail production startup when:
  - JWT secret is missing or equals the development fallback.
  - `platform.storage.provider=LOCAL`.
  - `platform.ai.generation.mock.enabled=true`.
- Updated production YAML to:
  - require `JWT_SECRET_BASE64`.
  - disable Swagger/OpenAPI by default.
  - hide production health details.
  - default AI mock generation to disabled in production.

## Database and Flyway Notes

- Migrations are present through V50 and include workspace, identity, assets, prompts, generation, approvals, billing, payment, AI, monitoring, profile, and operations foundations.
- Index coverage exists for many workspace/status/reference fields, especially assets, projects, generated versions, usage, and payment tables.
- Full empty-database Flyway verification was not completed because Docker/PostgreSQL was unavailable in this environment.
- Migration risk remains around legacy repair inserts and `IF NOT EXISTS` patterns masking drift; run clean schema validation in CI.

## Test Results

| Command | Result | Notes |
|---|---|---|
| `.\mvnw.cmd -q -DskipTests compile` | PASS | Full multi-module compile succeeded. |
| `.\mvnw.cmd -q test` | PASS | Test process exited 0. Logs showed Testcontainers could not find Docker, so container-backed coverage was not fully exercised. |

## Build Results

Build status: PASS for compile.  
Runtime startup: NOT VERIFIED end-to-end because PostgreSQL/Redis/Kafka/Docker are not available in this shell session.

## Remaining Risks

- Production approval requires CI with Docker/Testcontainers and real dependency startup.
- Restrict public Swagger and actuator endpoints at the security matcher level.
- Add outbox/DLQ strategy for business-critical Kafka event publication.
- Replace mock/foundation AI and payment paths with provider-specific verified clients before enabling production flows.
- Enforce pagination for large master/workspace lists and audit/usage/payment history.
