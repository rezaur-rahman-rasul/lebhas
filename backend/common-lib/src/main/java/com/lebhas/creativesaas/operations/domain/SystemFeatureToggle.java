package com.lebhas.creativesaas.operations.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "system_feature_toggles", schema = "platform")
public class SystemFeatureToggle extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "toggle_key", nullable = false, unique = true, length = 80)
    private SystemFeatureToggleKey toggleKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "reason", length = 1000)
    private String reason;

    protected SystemFeatureToggle() {
    }

    public static SystemFeatureToggle create(SystemFeatureToggleKey key, boolean enabled, String reason) {
        SystemFeatureToggle toggle = new SystemFeatureToggle();
        toggle.toggleKey = key;
        toggle.enabled = enabled;
        toggle.reason = normalize(reason);
        return toggle;
    }

    public void update(boolean enabled, String reason) {
        this.enabled = enabled;
        this.reason = normalize(reason);
    }

    public SystemFeatureToggleKey getToggleKey() { return toggleKey; }
    public boolean isEnabled() { return enabled; }
    public String getReason() { return reason; }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
