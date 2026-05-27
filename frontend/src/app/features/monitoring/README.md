# Day 10 Master Monitoring Foundation

Project: Lebhas - Brand Attire

## Scope

Day 10 Batch 1 adds the frontend foundation for Master operational monitoring.

Implemented files:

```text
src/app/features/monitoring/
|-- models/monitoring.models.ts
|-- services/monitoring-api.service.ts
|-- state/monitoring.store.ts
`-- README.md
```

## APIs

- `GET /api/v1/master/monitoring/system-health`
- `GET /api/v1/master/monitoring/alerts`
- `GET /api/v1/master/monitoring/ai-providers`
- `GET /api/v1/master/monitoring/payments`
- `GET /api/v1/master/monitoring/workspaces`

The service reuses the existing shared `ApiService`, auth interceptors, base URL handling, and `ApiResponse<T>` unwrap helper.

## State

`MonitoringStore` owns monitoring state with Angular Signals:

- system health
- monitoring alerts
- AI provider summary
- payment summary
- workspace summary
- selected severity
- unresolved-only filter
- loading
- error

Computed signals expose critical alerts, unresolved alerts, healthy services, degraded services, and down services.

## Rules

- No pages or shared UI components are included in Batch 1.
- No fake monitoring data is added.
- No WebSocket realtime delivery is implemented.
- No external monitoring integration is implemented.
- Monitoring is Master-only through existing `PermissionStore` helpers.
- Sensitive metadata must be masked before future display.

## Implemented In Batch 6

Batch 6 adds reusable monitoring UI components only:

```text
src/app/features/monitoring/components/
|-- health-status-card/
|-- monitoring-alert-card/
|-- monitoring-severity-badge/
|-- service-health-badge/
|-- monitoring-summary-card/
|-- monitoring-filter-bar/
|-- monitoring-empty-state/
`-- monitoring-loading-state/
```

Component behavior:

- `health-status-card` shows service name, friendly service label, status, last checked time, and a short non-technical explanation.
- `monitoring-alert-card` shows alert type, severity, title, description, workspace/provider references, resolved state, and timestamps.
- `monitoring-severity-badge` maps `INFO`, `WARNING`, `ERROR`, and `CRITICAL` to readable labels.
- `service-health-badge` maps `HEALTHY`, `DEGRADED`, `DOWN`, and `UNKNOWN` to friendly status labels.
- `monitoring-summary-card` is reusable for system health, alert, AI provider, payment, workspace, and failed-event summaries.
- `monitoring-filter-bar` emits alert type, severity, unresolved-only, refresh, and clear events to future parent pages.
- `monitoring-empty-state` and `monitoring-loading-state` provide consistent empty and skeleton states.

Friendly infrastructure wording:

- Kafka is displayed as event processing.
- Redis is displayed as cache/state service.
- Storage services are described without exposing raw provider details.
- AI and payment issues use calm, ordinary-user wording.

Batch 6 does not add monitoring pages, fake monitoring data, external monitoring integrations, production incident management UI, or Day 11+ modules.

## Implemented In Batch 7

Batch 7 adds Master monitoring UI pages:

```text
src/app/features/monitoring/
|-- dashboard/
|   |-- master-monitoring-dashboard.ts
|   |-- master-monitoring-dashboard.html
|   `-- master-monitoring-dashboard.scss
|-- alerts/
|   |-- monitoring-alerts.ts
|   |-- monitoring-alerts.html
|   `-- monitoring-alerts.scss
|-- system-health/
|   |-- system-health.ts
|   |-- system-health.html
|   `-- system-health.scss
`-- monitoring.routes.ts
```

Routes:

- `/master/monitoring`
- `/master/monitoring/alerts`
- `/master/monitoring/system-health`
- `/master/monitoring/ai-providers`
- `/master/monitoring/payments`
- `/master/monitoring/workspaces`

The dashboard uses `MonitoringStore.loadMonitoringDashboard()` and shows system health, active alerts, AI provider summary, payment failure summary, workspace health summary, service health cards, and recent critical alerts.

The system health page uses `MonitoringStore.loadSystemHealth()` and displays backend-reported service status with friendly labels for database, cache/state, event processing, storage, payment, and AI foundations.

The monitoring alerts page uses `MonitoringStore.loadMonitoringAlerts()` with severity, alert type, and unresolved-only filters. Unresolved critical alerts are shown first.

Integration rules:

- Day 7 AI monitoring is linked from Master monitoring through summary and route foundations; AI monitoring components are not duplicated.
- Day 9 payment monitoring is linked through summary and route foundations; payment provider setup UI is not duplicated.
- No external monitoring integrations are added.
- No fake monitoring data is added.
- No production incident management workflow is added.

## Implemented In Batch 8

Batch 8 integrates Master monitoring into protected navigation and route guards:

- `/master/monitoring`
- `/master/monitoring/alerts`
- `/master/monitoring/system-health`
- `/master/monitoring/ai-providers`
- `/master/monitoring/payments`
- `/master/monitoring/workspaces`

Navigation rules:

- `MASTER` sees Monitoring, System Health, Monitoring Alerts, AI Provider Health, Payment Monitoring, and Workspace Monitoring when permission helpers allow them.
- `ADMIN` and `CREW` do not see Master monitoring links.
- Unauthorized direct access falls back to the monitoring dashboard route, where friendly access-denied copy is shown.

Guard rules:

- `canViewMasterMonitoring` protects Master monitoring foundation routes.
- `canViewSystemHealth` protects system health.
- `canViewMonitoringAlerts` protects alerts.

Batch 8 does not add external monitoring integrations, fake monitoring data, production incident management UI, WebSocket delivery, or Day 11+ modules.
