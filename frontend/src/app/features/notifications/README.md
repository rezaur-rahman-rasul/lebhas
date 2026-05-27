# Day 10 Notifications Foundation

Project: Lebhas - Brand Attire

## Scope

Day 10 Batch 1 adds the frontend foundation for workspace notifications and notification preferences.

Implemented files:

```text
src/app/features/notifications/
|-- models/notification.models.ts
|-- services/notification-api.service.ts
|-- state/notification.store.ts
`-- README.md
```

## APIs

- `GET /api/v1/workspaces/{workspaceId}/notifications`
- `GET /api/v1/workspaces/{workspaceId}/notifications/unread-count`
- `POST /api/v1/workspaces/{workspaceId}/notifications/{notificationId}/read`
- `POST /api/v1/workspaces/{workspaceId}/notifications/read-all`
- `GET /api/v1/workspaces/{workspaceId}/notification-preferences`
- `PUT /api/v1/workspaces/{workspaceId}/notification-preferences`

The service reuses the existing shared `ApiService`, auth interceptors, base URL handling, and `ApiResponse<T>` unwrap helper.

## State

`NotificationStore` owns notification state with Angular Signals:

- notifications
- unread count
- preferences
- selected notification type
- selected priority
- unread-only filter
- loading
- error

Computed signals expose unread and critical notifications.

## Rules

- No pages or UI components are included in Batch 1.
- No fake notification data is added.
- No WebSocket realtime delivery is implemented.
- No real email, SMS, or push provider integration is implemented.
- Preferences are only frontend/backend configuration foundation.
- Workspace isolation is enforced by workspace-scoped API paths.
- Sensitive metadata must be masked before future display.

## Implemented In Batch 2

Batch 2 adds reusable notification UI components only:

```text
src/app/features/notifications/components/
|-- notification-card/
|-- notification-priority-badge/
|-- notification-type-icon/
|-- notification-filter-bar/
|-- unread-count-pill/
|-- notification-empty-state/
`-- notification-loading-state/
```

Component behavior:

- `notification-card` shows compact notification details, unread highlighting, priority, reference foundation, and a mark-as-read action.
- `notification-priority-badge` maps `LOW`, `NORMAL`, `HIGH`, and `CRITICAL` to friendly labels.
- `notification-type-icon` maps backend notification types to Lucide icons and user-friendly labels such as `One AI tool is currently failing.` and `A payment needs attention.`
- `notification-filter-bar` emits unread-only, notification type, priority, date, refresh, and clear events to future parent pages.
- `unread-count-pill` displays unread counts with an accessible status label.
- `notification-empty-state` and `notification-loading-state` provide consistent empty and skeleton states.

Unread notifications are visually highlighted without changing the underlying data. Critical notifications use visible but restrained styling.

Batch 2 still does not add pages, route integration, WebSocket delivery, email/SMS/push provider integration, fake notification data, or Day 11+ modules.

## Implemented In Batch 3

Batch 3 adds workspace notification pages:

```text
src/app/features/notifications/
|-- center/
|   |-- notification-center.ts
|   |-- notification-center.html
|   `-- notification-center.scss
|-- preferences/
|   |-- notification-preferences.ts
|   |-- notification-preferences.html
|   `-- notification-preferences.scss
`-- notification.routes.ts
```

Protected routes:

- `/notifications`
- `/notifications/preferences`

The notification center uses:

- `NotificationStore.loadNotifications(workspaceId)`
- `NotificationStore.loadUnreadCount(workspaceId)`
- `NotificationStore.markNotificationAsRead(workspaceId, notificationId)`
- `NotificationStore.markAllNotificationsAsRead(workspaceId)`
- `NotificationStore.setSelectedNotificationType(type)`
- `NotificationStore.setSelectedPriority(priority)`
- `NotificationStore.setUnreadOnly(value)`

The preferences page uses:

- `NotificationStore.loadNotificationPreferences(workspaceId)`
- `NotificationStore.updateNotificationPreferences(workspaceId, payload)`

Workspace ID comes from the existing `WorkspaceStore`. No workspace state is duplicated and no workspace ID is hardcoded.

Permission rules:

- `ADMIN` can view workspace notifications when `canViewNotifications` allows it.
- `ADMIN` can manage notification preferences when `canManageNotificationPreferences` allows it.
- `CREW` can view own notifications and manage preferences only when explicit permission allows it.
- Unauthorized users see friendly access-denied copy with no raw permission keys.

Batch 3 still does not implement WebSocket delivery, real email/SMS/push provider integration, fake notification data, or Day 11+ modules. Email, SMS, and push controls are preference foundations only until providers are enabled.

## Implemented In Batch 8

Batch 8 integrates notifications into the protected app shell:

- Header notification bell added to the existing dashboard topbar.
- Bell uses `NotificationStore.unreadCount`.
- Unread count loads from the backend for the active workspace.
- Bell links to `/notifications` and works without WebSocket delivery.
- `/notifications/preferences` keeps the existing preference access guard and redirects unauthorized users to the notification center, where friendly access-denied copy is shown.

Navigation rules:

- `ADMIN` sees notifications and notification preferences when permission allows.
- `CREW` sees notifications only when permission allows.
- Notification settings stay hidden unless `canManageNotificationPreferences` allows them.

Batch 8 does not add realtime delivery, email/SMS/push provider integration, fake notifications, or Day 11+ modules.
