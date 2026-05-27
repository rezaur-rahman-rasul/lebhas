package com.lebhas.creativesaas.profile.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "user_password_history", schema = "platform")
public class UserPasswordHistory extends BaseEntity {

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    protected UserPasswordHistory() {
    }

    public static UserPasswordHistory record(UUID userId, String passwordHash) {
        UserPasswordHistory history = new UserPasswordHistory();
        history.userId = requireUserId(userId);
        history.passwordHash = requirePasswordHash(passwordHash);
        return history;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    private static UUID requireUserId(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        return userId;
    }

    private static String requirePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        return passwordHash.trim();
    }
}
