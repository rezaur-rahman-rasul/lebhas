# Day 10 Activity Feed Foundation

Project: Lebhas - Brand Attire

## Scope

Day 10 Batch 1 adds the frontend foundation for workspace activity feed data.

Implemented files:

```text
src/app/features/activity/
|-- models/activity.models.ts
|-- services/activity-api.service.ts
|-- state/activity.store.ts
`-- README.md
```

## APIs

- `GET /api/v1/workspaces/{workspaceId}/activity-feed`
- `GET /api/v1/workspaces/{workspaceId}/activity-feed/{activityId}`

The service reuses the existing shared `ApiService`, auth interceptors, base URL handling, and `ApiResponse<T>` unwrap helper.

## State

`ActivityStore` owns activity state with Angular Signals:

- activities
- selected activity type
- selected actor
- selected date range
- loading
- error

Computed signals expose recent activities for future dashboards and panels.

## Rules

- No pages or shared UI components are included in Batch 1.
- No fake activity data is added.
- No WebSocket realtime delivery is implemented.
- Activity metadata must be treated as untrusted display data and masked where sensitive.
- Workspace isolation is enforced by workspace-scoped API paths.

## Implemented In Batch 4

Batch 4 adds reusable activity UI components and workspace activity pages:

```text
src/app/features/activity/
|-- components/
|   |-- activity-card/
|   |-- activity-type-badge/
|   |-- activity-filter-bar/
|   |-- timeline-item/
|   |-- activity-empty-state/
|   `-- activity-loading-state/
|-- feed/
|   |-- activity-feed.ts
|   |-- activity-feed.html
|   `-- activity-feed.scss
|-- timeline/
|   |-- workspace-timeline.ts
|   |-- workspace-timeline.html
|   `-- workspace-timeline.scss
`-- activity.routes.ts
```

Routes:

- `/activity-feed`
- `/workspace-timeline`

The activity feed uses `ActivityStore.loadActivityFeed(workspaceId)` and `ActivityStore.loadActivityDetail(workspaceId, activityId)`. Filters use the existing store setters for activity type, actor, and date range.

The workspace timeline reuses the same backend activity feed and presents recent activities in a simplified chronological view. It does not create analytics, summaries, or fake timeline data.

Permission behavior:

- `ADMIN` can view workspace activity when `canViewActivityFeed` allows it.
- `CREW` sees activity only when explicit permission allows it.
- Unauthorized users see friendly access-denied copy without raw permission keys.

Security and UX notes:

- Activity metadata is summarized and sensitive keys are hidden.
- Raw secrets, tokens, webhook payloads, and credentials are not displayed.
- No fake activity data is added.
- No advanced analytics or Day 11+ modules are included.

## Implemented In Batch 8

Batch 8 integrates activity routes and navigation into the protected app shell:

- `/activity-feed`
- `/workspace-timeline`

Navigation rules:

- `ADMIN` sees activity routes when `canViewActivityFeed` allows it.
- `CREW` sees activity only when explicitly allowed.
- Unauthorized users see: `You do not have access to workspace activity. Please contact your workspace admin if you need access.`

No fake activity data, advanced analytics, WebSocket delivery, or Day 11+ modules are included.
