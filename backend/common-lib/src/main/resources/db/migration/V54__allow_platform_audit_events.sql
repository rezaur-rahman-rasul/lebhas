ALTER TABLE platform.audit_logs
    ALTER COLUMN workspace_id DROP NOT NULL;

COMMENT ON COLUMN platform.audit_logs.workspace_id IS
    'Nullable for platform-level master audit events; populated for workspace-scoped audit events.';
