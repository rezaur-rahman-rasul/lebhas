ALTER TABLE platform.notifications
    ADD COLUMN IF NOT EXISTS notification_channel VARCHAR(40) NOT NULL DEFAULT 'IN_APP';

ALTER TABLE platform.notifications
    ADD COLUMN IF NOT EXISTS notification_priority VARCHAR(30) NOT NULL DEFAULT 'NORMAL';

ALTER TABLE platform.notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_notification_type;

ALTER TABLE platform.notifications
    ADD CONSTRAINT chk_notifications_notification_type
        CHECK (notification_type IN (
            'APPROVAL_ASSIGNED',
            'APPROVAL_SUBMITTED',
            'APPROVAL_APPROVED',
            'APPROVAL_REJECTED',
            'APPROVAL_CHANGES_REQUESTED',
            'APPROVAL_RESUBMITTED',
            'PAYMENT_TRANSACTION_SUCCEEDED',
            'PAYMENT_TRANSACTION_FAILED',
            'SUBSCRIPTION_ACTIVATED',
            'SUBSCRIPTION_CHANGED',
            'CREDIT_PURCHASE_COMPLETED',
            'INVOICE_PAID',
            'CREDIT_LOW_BALANCE',
            'CREDIT_USAGE_UPDATED',
            'AI_GENERATION_COMPLETED',
            'AI_GENERATION_FAILED',
            'AI_PROVIDER_HEALTH_CHANGED',
            'ASSET_UPLOAD_COMPLETED',
            'ASSET_UPLOAD_FAILED',
            'SHARE_LINK_CREATED',
            'SYSTEM_HEALTH_ALERT',
            'MONITORING_ALERT'
        ));

ALTER TABLE platform.notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_notification_channel;

ALTER TABLE platform.notifications
    ADD CONSTRAINT chk_notifications_notification_channel
        CHECK (notification_channel IN ('IN_APP'));

ALTER TABLE platform.notifications
    DROP CONSTRAINT IF EXISTS chk_notifications_notification_priority;

ALTER TABLE platform.notifications
    ADD CONSTRAINT chk_notifications_notification_priority
        CHECK (notification_priority IN ('LOW', 'NORMAL', 'HIGH', 'URGENT'));

CREATE INDEX IF NOT EXISTS idx_notifications_workspace_created_at
    ON platform.notifications (workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient_status_created_at
    ON platform.notifications (recipient_user_id, notification_status, created_at DESC);

CREATE TABLE IF NOT EXISTS platform.notification_preferences (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES platform.users (id) ON DELETE CASCADE,
    notification_type VARCHAR(80) NOT NULL,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    push_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_notification_preferences_workspace_user_type UNIQUE (workspace_id, user_id, notification_type),
    CONSTRAINT chk_notification_preferences_type_not_blank CHECK (LENGTH(BTRIM(notification_type)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_notification_preferences_workspace_user
    ON platform.notification_preferences (workspace_id, user_id);

CREATE INDEX IF NOT EXISTS idx_notification_preferences_type
    ON platform.notification_preferences (notification_type);

CREATE TABLE IF NOT EXISTS platform.activity_feed_entries (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    source_event_id VARCHAR(120) NOT NULL,
    actor_user_id UUID REFERENCES platform.users (id) ON DELETE SET NULL,
    activity_category VARCHAR(40) NOT NULL,
    activity_type VARCHAR(120) NOT NULL,
    title VARCHAR(180) NOT NULL,
    description VARCHAR(2000),
    reference_type VARCHAR(80),
    reference_id UUID,
    activity_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_activity_feed_source_event_id UNIQUE (source_event_id),
    CONSTRAINT chk_activity_feed_category CHECK (activity_category IN (
        'WORKSPACE', 'CREATIVE_REQUEST', 'GENERATED_VERSION', 'APPROVAL', 'ASSET',
        'SHARE', 'USAGE', 'PAYMENT', 'AI', 'SYSTEM'
    )),
    CONSTRAINT chk_activity_feed_type_not_blank CHECK (LENGTH(BTRIM(activity_type)) > 0),
    CONSTRAINT chk_activity_feed_title_not_blank CHECK (LENGTH(BTRIM(title)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_activity_feed_workspace_created_at
    ON platform.activity_feed_entries (workspace_id, activity_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_feed_actor_created_at
    ON platform.activity_feed_entries (actor_user_id, activity_at DESC);

CREATE INDEX IF NOT EXISTS idx_activity_feed_reference
    ON platform.activity_feed_entries (reference_type, reference_id);

CREATE INDEX IF NOT EXISTS idx_activity_feed_category
    ON platform.activity_feed_entries (activity_category);

CREATE TABLE IF NOT EXISTS platform.audit_logs (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    source_event_id VARCHAR(120) NOT NULL,
    actor_user_id UUID REFERENCES platform.users (id) ON DELETE SET NULL,
    action_type VARCHAR(40) NOT NULL,
    outcome VARCHAR(30) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id UUID,
    summary VARCHAR(500) NOT NULL,
    metadata_json TEXT,
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    audit_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_audit_logs_source_event_id UNIQUE (source_event_id),
    CONSTRAINT chk_audit_logs_action_type CHECK (action_type IN (
        'CREATE', 'UPDATE', 'DELETE', 'READ', 'LOGIN', 'LOGOUT',
        'APPROVE', 'REJECT', 'PURCHASE', 'PROCESS', 'ACCESS', 'SYSTEM'
    )),
    CONSTRAINT chk_audit_logs_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED')),
    CONSTRAINT chk_audit_logs_entity_type_not_blank CHECK (LENGTH(BTRIM(entity_type)) > 0),
    CONSTRAINT chk_audit_logs_summary_not_blank CHECK (LENGTH(BTRIM(summary)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_workspace_created_at
    ON platform.audit_logs (workspace_id, audit_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_created_at
    ON platform.audit_logs (actor_user_id, audit_at DESC);

CREATE INDEX IF NOT EXISTS idx_audit_logs_reference
    ON platform.audit_logs (entity_type, entity_id);

CREATE INDEX IF NOT EXISTS idx_audit_logs_action_outcome
    ON platform.audit_logs (action_type, outcome);

CREATE TABLE IF NOT EXISTS platform.system_health_events (
    id UUID PRIMARY KEY,
    workspace_id UUID REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    source_event_id VARCHAR(120) NOT NULL,
    component_type VARCHAR(40) NOT NULL,
    component_name VARCHAR(120) NOT NULL,
    health_status VARCHAR(40) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    details_json TEXT,
    event_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_system_health_events_source_event_id UNIQUE (source_event_id),
    CONSTRAINT chk_system_health_events_component_type CHECK (component_type IN (
        'AUTH', 'WORKSPACE', 'CREATIVE', 'APPROVAL', 'ASSET', 'USAGE', 'PAYMENT',
        'AI', 'NOTIFICATION', 'REDIS', 'KAFKA', 'POSTGRES', 'STORAGE', 'APPLICATION'
    )),
    CONSTRAINT chk_system_health_events_status CHECK (health_status IN ('HEALTHY', 'DEGRADED', 'UNAVAILABLE', 'RECOVERED')),
    CONSTRAINT chk_system_health_events_severity CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    CONSTRAINT chk_system_health_events_component_name_not_blank CHECK (LENGTH(BTRIM(component_name)) > 0),
    CONSTRAINT chk_system_health_events_message_not_blank CHECK (LENGTH(BTRIM(message)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_system_health_events_component_created_at
    ON platform.system_health_events (component_type, event_at DESC);

CREATE INDEX IF NOT EXISTS idx_system_health_events_status_severity
    ON platform.system_health_events (health_status, severity);

CREATE INDEX IF NOT EXISTS idx_system_health_events_workspace_created_at
    ON platform.system_health_events (workspace_id, event_at DESC);

CREATE TABLE IF NOT EXISTS platform.monitoring_alerts (
    id UUID PRIMARY KEY,
    workspace_id UUID REFERENCES platform.workspaces (id) ON DELETE CASCADE,
    alert_key VARCHAR(160) NOT NULL,
    component_type VARCHAR(40) NOT NULL,
    component_name VARCHAR(120) NOT NULL,
    severity VARCHAR(30) NOT NULL,
    alert_status VARCHAR(40) NOT NULL DEFAULT 'OPEN',
    title VARCHAR(180) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    triggered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_monitoring_alerts_alert_key UNIQUE (alert_key),
    CONSTRAINT chk_monitoring_alerts_component_type CHECK (component_type IN (
        'AUTH', 'WORKSPACE', 'CREATIVE', 'APPROVAL', 'ASSET', 'USAGE', 'PAYMENT',
        'AI', 'NOTIFICATION', 'REDIS', 'KAFKA', 'POSTGRES', 'STORAGE', 'APPLICATION'
    )),
    CONSTRAINT chk_monitoring_alerts_severity CHECK (severity IN ('INFO', 'WARNING', 'ERROR', 'CRITICAL')),
    CONSTRAINT chk_monitoring_alerts_status CHECK (alert_status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    CONSTRAINT chk_monitoring_alerts_key_not_blank CHECK (LENGTH(BTRIM(alert_key)) > 0),
    CONSTRAINT chk_monitoring_alerts_title_not_blank CHECK (LENGTH(BTRIM(title)) > 0),
    CONSTRAINT chk_monitoring_alerts_description_not_blank CHECK (LENGTH(BTRIM(description)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_monitoring_alerts_status_severity
    ON platform.monitoring_alerts (alert_status, severity);

CREATE INDEX IF NOT EXISTS idx_monitoring_alerts_component_created_at
    ON platform.monitoring_alerts (component_type, triggered_at DESC);

CREATE INDEX IF NOT EXISTS idx_monitoring_alerts_workspace_created_at
    ON platform.monitoring_alerts (workspace_id, triggered_at DESC);
