# Lebhas Day 7 Frontend: AI Monitoring Notes

Project: Lebhas - Brand Attire  
Slogan: Create Ads Beyond Imagination

## Scope Completed

Day 7 adds the frontend monitoring surface for AI cost, provider performance, provider health, layer analytics, workspace usage, creative quality scores, routing optimization summaries, and AI failure visibility.

The implementation is monitoring-only. It does not add setup, editing, billing, payment, publishing, or future Day 8+ modules.

## Feature Structure

Day 7 lives under:

```text
src/app/features/ai-monitoring/
├── dashboard/
├── provider-health/
├── provider-metrics/
├── layer-analytics/
├── workspace-usage/
├── quality-scores/
├── failures/
├── components/
├── models/
├── services/
└── state/
```

Reusable AI monitoring presentation components are kept in `components/` and reuse existing shared primitives such as `app-card`, `app-button`, `app-empty-state`, `app-error-state`, `app-loading-state`, `app-status-badge`, `app-page-header`, and `app-icon`.

## Routes Added

Protected Day 7 routes:

```text
/ai-monitoring
/ai-monitoring/providers
/ai-monitoring/providers/metrics
/ai-monitoring/layers
/ai-monitoring/workspaces
/ai-monitoring/quality
/ai-monitoring/failures
```

Existing Day 1 to Day 6 routes were not rewritten.

## API Service

One Day 7 API service was added:

```text
src/app/features/ai-monitoring/services/ai-monitoring-api.service.ts
```

It reuses the existing centralized `ApiService` and shared `unwrapApiResponse` helper for the standard backend response format:

```json
{
  "success": true,
  "message": "",
  "data": {},
  "errors": [],
  "timestamp": ""
}
```

The service exposes workspace-scoped and master-scoped AI monitoring endpoints. Components do not manually parse `ApiResponse<T>`.

## Signal Store

One signal-based Day 7 store was added:

```text
src/app/features/ai-monitoring/state/ai-monitoring.store.ts
```

State includes:

- provider metrics
- provider health
- layer analytics
- workspace AI usage
- quality scores
- AI failures
- selected date range
- selected provider
- selected layer
- loading
- error

Computed signals include healthy/degraded/down providers, total cost, average quality score, cheapest provider, fastest provider, best quality provider, most reliable provider, highest cost workspace, and recent failures.

## Models And Enums

Day 7 models and enums live in:

```text
src/app/features/ai-monitoring/models/ai-monitoring.models.ts
```

Models include provider metrics, provider health, layer analytics, workspace AI usage, quality scores, failure logs, and workspace generation analytics.

Enums include:

- `ProviderHealthStatus`
- `AiFailureType`
- `QualityScoreLabel`

Provider names, layer names, model names, costs, quality values, limits, statuses, and recommendations remain backend-owned.

## Permission Behavior

Day 7 extends the existing permission foundation. No duplicate permission system was added.

Permission hooks:

- `canViewAiMonitoring`
- `canViewProviderMetrics`
- `canViewLayerAnalytics`
- `canViewWorkspaceAiUsage`
- `canViewQualityScores`
- `canViewAiFailures`

Access rules:

- `MASTER`: can access full AI monitoring when the feature is enabled by backend policy.
- `ADMIN`: can access workspace AI usage and quality/failure data for the active workspace when explicit permission and feature policy allow.
- `CREW`: hidden unless explicit permission and backend feature policy allow.

Unauthorized users see friendly access-restricted states.

## UI Summary

### Main Dashboard

`/ai-monitoring` shows AI monitoring summary cards for total requests, total cost, average quality, average generation time, provider failure rate, best performing provider, cheapest provider, most reliable provider, highest cost workspace, routing comparisons, and recent failures.

Dynamic routing optimization display is read-only. It compares cheapest, fastest, best quality, most reliable, and best quality-to-cost providers from backend metrics. Backend recommendations are shown only when present; the frontend does not invent recommendations.

### Provider Metrics

`/ai-monitoring/providers/metrics` shows provider name, model, total requests, successful requests, failed requests, average latency, average cost, average quality score, uptime, last success, last failure, and reported status context.

Desktop uses a detail table. Mobile uses provider cards.

### Provider Health

`/ai-monitoring/providers` shows active, degraded, failed, and cooldown providers, plus critical provider issues at the top.

The UI displays failure reason, recovery status, last checked time, fallback availability, and backend-provided recommended action using friendly wording.

### Layer Analytics

`/ai-monitoring/layers` shows dynamic layer analytics from backend data: layer name, layer type, selected provider, model, total executions, success count, failure count, average execution time, average cost, and average quality score.

It includes friendly cost, speed, and failure attention cards:

- This layer is costing more than expected.
- This layer is slower than usual.
- This layer failed several times recently.

Routing policy editing is intentionally not available.

### Workspace AI Usage

`/ai-monitoring/workspaces` shows workspace name, backend-provided active plan, total generation requests, generated versions, credits consumed, estimated AI cost, total failures, average generation time, high-cost warning, and a lightweight usage trend list foundation.

For high-cost warnings, the UI shows:

```text
AI cost increased unusually for this workspace.
```

### Quality Scores

`/ai-monitoring/quality` shows generated version reference, overall score, text readability, product preservation, branding, Bangla typography, composition, quality notes, and created date.

Quality labels are displayed only if the backend sends `qualityLabel`. The frontend does not invent score bands.

### AI Failures

`/ai-monitoring/failures` shows creative request reference, layer, provider, model, failure type, friendly failure reason, retry attempt, fallback status, and created date.

Known failure types are mapped to ordinary-user copy:

- `TIMEOUT`: This tool took too long to respond.
- `RATE_LIMIT`: This tool is temporarily busy.
- `PROVIDER_DOWN`: This tool is currently unavailable.
- `INVALID_RESPONSE`: This tool returned an unusable result.
- `QUALITY_FAILURE`: Output quality was not good enough.
- `COST_LIMIT_EXCEEDED`: This request exceeded the allowed AI cost.
- `UNKNOWN`: Something went wrong while generating this creative.

Fallback display:

- Backup tool was used.
- No backup tool was used.

No manual retry action was added because there is no Day 7 backend retry API.

## Loading, Empty, And Error States

Day 7 pages use consistent states:

- loading skeleton cards
- friendly empty states
- friendly error states
- retry buttons where loading can be retried

Default empty copy follows the requested ordinary-user pattern:

```text
No AI usage data yet. Once creatives are generated, analytics will appear here.
```

Technical backend messages are mapped to friendly monitoring copy where they may surface:

- This provider is slower than usual.
- This tool failed too many times recently.
- AI cost increased unusually for this workspace.

No stack traces are shown.

## Responsive And Accessibility Notes

Day 7 screens are responsive across desktop, laptop, tablet, and mobile.

Patterns used:

- responsive summary grids
- mobile cards for provider metrics
- desktop-only detail tables where cards/lists provide the mobile view
- keyboard-friendly date/provider/layer filters
- accessible refresh and clear actions
- ARIA labels on provider status and routing status badges
- no icon-only actions without labels
- no horizontal overflow patterns in production Day 7 files

## Testing Notes

Day 7 tests were added in:

```text
src/app/features/ai-monitoring/ai-monitoring.day7.spec.ts
```

Coverage includes:

- MASTER dashboard access
- CREW/no-permission hidden access
- provider metrics rendering
- provider health status badge rendering
- dynamic layer analytics rendering
- ADMIN active-workspace usage loading
- quality score rendering
- Bangla typography score display
- AI failure list and fallback status copy
- cheapest provider computed signal
- fastest provider computed signal
- best quality provider computed signal
- loading, empty, and error-with-retry states
- date range filter state
- provider filter state
- mobile overflow guard patterns
- no hardcoded provider names in production AI monitoring files
- no `*ngIf` or `*ngFor` in Day 7 templates
- API service using shared `ApiResponse<T>` unwrapping

Mocks use sample backend values inside tests only. Production code remains backend-driven.

## Intentional Exclusions

The following were intentionally not implemented in Day 7:

- payment UI
- subscription checkout
- billing payment UI
- social publishing UI
- AI provider setup/editing UI
- routing policy editing UI
- Day 8+ modules
- fake analytics
- hardcoded providers
- hardcoded layers
- hardcoded pricing plans or plan limits

## Final Cleanup Checks

Day 7 production files were checked for:

- no `.component` naming
- no CSS files
- no inline templates
- no `*ngIf` or `*ngFor`
- no hardcoded provider names
- no hardcoded layer names
- no hardcoded plan names
- no payment, checkout, billing, or publishing UI references
- no duplicate Day 7 API services, signal stores, or model files
