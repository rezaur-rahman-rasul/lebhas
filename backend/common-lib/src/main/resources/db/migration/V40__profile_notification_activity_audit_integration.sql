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
            'MONITORING_ALERT',
            'PROFILE_UPDATED',
            'PROFILE_IMAGE_UPDATED',
            'PASSWORD_CHANGED',
            'SESSION_REVOKED',
            'SECURITY_ACTIVITY_DETECTED'
        ));

ALTER TABLE platform.audit_logs
    DROP CONSTRAINT IF EXISTS chk_audit_logs_action_type;

ALTER TABLE platform.audit_logs
    ADD CONSTRAINT chk_audit_logs_action_type
        CHECK (action_type IN (
            'CREATE',
            'UPDATE',
            'DELETE',
            'READ',
            'LOGIN',
            'LOGOUT',
            'APPROVE',
            'REJECT',
            'PURCHASE',
            'PROCESS',
            'ACCESS',
            'SYSTEM',
            'PROFILE_UPDATED',
            'SETTINGS_UPDATED',
            'PROFILE_IMAGE_UPLOAD_REQUESTED',
            'PROFILE_IMAGE_UPDATED',
            'PROFILE_IMAGE_REMOVED',
            'PASSWORD_CHANGED',
            'PASSWORD_CHANGE_FAILED',
            'SESSION_REVOKED',
            'UNAUTHORIZED_PROFILE_ACCESS_ATTEMPT'
        ));
