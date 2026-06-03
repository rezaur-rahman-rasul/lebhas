# AI Creative SaaS Backend

Day 1 backend foundation for a multi-tenant AI creative SaaS platform targeting Bangladesh.

## Module Layout

```text
backend/
  common-lib/
  gateway-service/
  auth-service/
  user-service/
  workspace-service/
  creative-service/
  billing-service/
  notification-service/
  docker/
  k8s/
  scripts/
```

`common-lib` owns cross-cutting platform concerns: API response format, global exception handling, audit entities, tenant context, security baseline, request logging, Redis cache configuration, OpenAPI setup, constants, shared DTOs, and validation messages.

Each service is a Spring Boot application with the same future-ready package convention:

```text
com.lebhas.creativesaas.<module>
  application/      use cases and orchestration
  domain/           entities, value objects, domain services
  infrastructure/   persistence, messaging, external integrations
  interfaces/       REST controllers, request/response DTOs
```

## Runtime Endpoints

Every service exposes:

- `GET /health`
- `GET /health/live`
- `GET /health/ready`
- `GET /liveness`
- `GET /readiness`
- `GET /swagger-ui.html`
- `GET /v3/api-docs`

Day 1 foundation endpoints include:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- `GET /api/v1/profile/me`
- `PUT /api/v1/profile/me`
- `POST /api/v1/profile/me/change-password`
- `GET /api/v1/workspaces/my`
- `GET /api/v1/workspaces/{workspaceId}/context`
- `POST /api/v1/workspaces`
- `GET /api/v1/master/system/context`

Register request example:

```http
POST http://localhost:8080/api/v1/auth/register
Content-Type: application/json

{
  "firstName": "Hridoy",
  "lastName": "Bhuiyan",
  "email": "hridoy.bhuiyan12@example.com",
  "phone": "+1234567890",
  "password": "StrongP@ssw0rd!",
  "confirmPassword": "StrongP@ssw0rd!"
}
```

Day 4 R2 asset foundation endpoints include:

- `POST /api/v1/workspaces/{workspaceId}/assets/upload-url`
- `POST /api/v1/workspaces/{workspaceId}/assets/confirm`
- `GET /api/v1/workspaces/{workspaceId}/assets`
- `GET /api/v1/workspaces/{workspaceId}/assets/{assetId}`
- `GET /api/v1/workspaces/{workspaceId}/assets/{assetId}/preview-url`
- `GET /api/v1/workspaces/{workspaceId}/assets/{assetId}/download-url`
- `DELETE /api/v1/workspaces/{workspaceId}/assets/{assetId}`
- `GET /api/v1/workspaces/{workspaceId}/projects/{projectId}/assets`
- `POST /api/v1/workspaces/{workspaceId}/projects/{projectId}/assets/upload-url`
- `POST /api/v1/profile/me/profile-image/upload-url`
- `POST /api/v1/profile/me/profile-image/confirm`
- `DELETE /api/v1/profile/me/profile-image`

Asset storage defaults to private Cloudflare R2/S3-compatible signed URLs. Upload and preview/download URL expiry is configurable; API responses do not expose R2 credentials or raw object keys.

Actuator is available under `/actuator`.

## Local Build

```powershell
.\mvnw.cmd clean package -DskipTests
```

Build one service and required modules:

```powershell
.\backend\scripts\build-service.ps1 -Service gateway-service
```

Run local infrastructure:

```powershell
docker compose -f .\backend\docker\docker-compose.yml up -d postgres redis
```

If you use a locally installed PostgreSQL instead of Docker, the default local profile expects:

- host: `localhost`
- port: `5432`
- database: `postgres`
- username: `postgres`
- password: `admin`

Initialize those defaults with `psql` from a superuser session:

```powershell
psql -U postgres -f .\backend\scripts\init-local-postgres.sql
```

Or set these environment variables in your IntelliJ run configuration to match your existing PostgreSQL credentials:

```text
POSTGRES_DB=creative_saas
POSTGRES_USERNAME=your_user
POSTGRES_PASSWORD=your_password
```

Run a service:

```powershell
.\mvnw.cmd -pl backend/gateway-service -am spring-boot:run "-Dspring-boot.run.profiles=local"
```

## Configuration

Service-specific `application.yaml` files import `classpath:application-common.yaml` from `common-lib`. The shared config defines PostgreSQL, Redis, Flyway, cache, actuator, OpenAPI, CORS, and logging defaults for:

- `local`
- `dev`
- `staging`
- `production`

Production secrets are resolved from environment variables or Kubernetes Secrets; no production credential defaults are embedded.
