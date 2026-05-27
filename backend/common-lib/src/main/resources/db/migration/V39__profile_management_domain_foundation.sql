CREATE TABLE IF NOT EXISTS platform.user_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES platform.users (id) ON DELETE CASCADE,
    first_name VARCHAR(80) NOT NULL,
    last_name VARCHAR(80) NOT NULL,
    display_name VARCHAR(160) NOT NULL,
    phone_number VARCHAR(30),
    job_title VARCHAR(120),
    profile_image_asset_id UUID REFERENCES platform.assets (id) ON DELETE SET NULL,
    profile_image_object_key VARCHAR(500),
    profile_image_url_cached VARCHAR(1000),
    timezone VARCHAR(80) NOT NULL DEFAULT 'Asia/Dhaka',
    locale VARCHAR(20) NOT NULL DEFAULT 'en',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_user_profiles_user_id UNIQUE (user_id),
    CONSTRAINT chk_user_profiles_first_name_not_blank CHECK (LENGTH(BTRIM(first_name)) > 0),
    CONSTRAINT chk_user_profiles_last_name_not_blank CHECK (LENGTH(BTRIM(last_name)) > 0),
    CONSTRAINT chk_user_profiles_display_name_not_blank CHECK (LENGTH(BTRIM(display_name)) > 0),
    CONSTRAINT chk_user_profiles_timezone_not_blank CHECK (LENGTH(BTRIM(timezone)) > 0),
    CONSTRAINT chk_user_profiles_locale_not_blank CHECK (LENGTH(BTRIM(locale)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_user_id
    ON platform.user_profiles (user_id);

CREATE INDEX IF NOT EXISTS idx_user_profiles_profile_image_asset_id
    ON platform.user_profiles (profile_image_asset_id);

CREATE INDEX IF NOT EXISTS idx_user_profiles_updated_at
    ON platform.user_profiles (updated_at DESC);

CREATE TABLE IF NOT EXISTS platform.user_account_settings (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES platform.users (id) ON DELETE CASCADE,
    preferred_language VARCHAR(20) NOT NULL DEFAULT 'ENGLISH',
    theme_preference VARCHAR(20) NOT NULL DEFAULT 'SYSTEM',
    notification_email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notification_in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    marketing_email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_user_account_settings_user_id UNIQUE (user_id),
    CONSTRAINT chk_user_account_settings_preferred_language CHECK (preferred_language IN ('BANGLA', 'ENGLISH', 'BOTH')),
    CONSTRAINT chk_user_account_settings_theme_preference CHECK (theme_preference IN ('SYSTEM', 'DARK', 'LIGHT'))
);

CREATE INDEX IF NOT EXISTS idx_user_account_settings_user_id
    ON platform.user_account_settings (user_id);

CREATE INDEX IF NOT EXISTS idx_user_account_settings_updated_at
    ON platform.user_account_settings (updated_at DESC);

CREATE TABLE IF NOT EXISTS platform.user_security_activities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES platform.users (id) ON DELETE CASCADE,
    activity_type VARCHAR(40) NOT NULL,
    ip_address VARCHAR(80),
    user_agent VARCHAR(500),
    location_hint VARCHAR(160),
    success BOOLEAN NOT NULL DEFAULT TRUE,
    failure_reason VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_user_security_activities_type CHECK (activity_type IN (
        'LOGIN',
        'LOGOUT',
        'TOKEN_REFRESH',
        'PASSWORD_CHANGED',
        'PROFILE_UPDATED',
        'PROFILE_IMAGE_UPDATED',
        'PROFILE_IMAGE_REMOVED',
        'ACCOUNT_SETTINGS_UPDATED',
        'SESSION_REVOKED',
        'SESSIONS_REVOKED',
        'ACCOUNT_LOCKED',
        'LOGIN_FAILED'
    )),
    CONSTRAINT chk_user_security_activities_failure_reason CHECK (
        success = TRUE OR failure_reason IS NULL OR LENGTH(BTRIM(failure_reason)) > 0
    )
);

CREATE INDEX IF NOT EXISTS idx_user_security_activities_user_created_at
    ON platform.user_security_activities (user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_security_activities_type_created_at
    ON platform.user_security_activities (activity_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_user_security_activities_user_type_created_at
    ON platform.user_security_activities (user_id, activity_type, created_at DESC);

CREATE TABLE IF NOT EXISTS platform.user_password_history (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES platform.users (id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(120),
    updated_by VARCHAR(120),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_user_password_history_hash_not_blank CHECK (LENGTH(BTRIM(password_hash)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_user_password_history_user_created_at
    ON platform.user_password_history (user_id, created_at DESC);
