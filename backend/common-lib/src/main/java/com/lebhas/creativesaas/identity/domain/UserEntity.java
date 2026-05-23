package com.lebhas.creativesaas.identity.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import com.lebhas.creativesaas.common.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Locale;

@Entity
@Table(
        name = "users",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
public class UserEntity extends BaseEntity {

    @Column(name = "first_name", nullable = false, length = 80)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 80)
    private String lastName;

    @Column(name = "email", nullable = false, length = 160)
    private String email;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "password", nullable = false, length = 120)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "is_master", nullable = false)
    private boolean master;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "is_email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts;

    @Column(name = "last_failed_login_at")
    private Instant lastFailedLoginAt;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    protected UserEntity() {
    }

    public static UserEntity register(
            String firstName,
            String lastName,
            String email,
            String phone,
            String passwordHash,
            Role role,
            UserStatus status,
            boolean emailVerified
    ) {
        UserEntity user = new UserEntity();
        user.firstName = firstName.trim();
        user.lastName = lastName.trim();
        user.email = normalizeEmail(email);
        user.phone = phone == null ? null : phone.trim();
        user.password = passwordHash;
        user.role = role;
        user.master = role != null && role.isMaster();
        user.status = status;
        user.emailVerified = emailVerified;
        return user;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getPassword() {
        return password;
    }

    public Role getRole() {
        return role;
    }

    public boolean isMaster() {
        return master;
    }

    public UserStatus getStatus() {
        return status;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public Instant getLastFailedLoginAt() {
        return lastFailedLoginAt;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void updateProfile(String firstName, String lastName, String email, String phone) {
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.email = normalizeEmail(email);
        this.phone = phone == null ? null : phone.trim();
    }

    public void assignRole(Role role) {
        this.role = role;
        this.master = role != null && role.isMaster();
    }

    public void changeStatus(UserStatus status) {
        this.status = status;
    }

    public void updatePasswordHash(String passwordHash) {
        this.password = passwordHash;
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    public void markLastLogin(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public void recordFailedLogin(Instant failedAt, long attempts, Instant lockedUntil) {
        this.failedLoginAttempts = (int) Math.min(attempts, Integer.MAX_VALUE);
        this.lastFailedLoginAt = failedAt;
        this.lockedUntil = lockedUntil;
    }

    public void clearFailedLoginState() {
        this.failedLoginAttempts = 0;
        this.lastFailedLoginAt = null;
        this.lockedUntil = null;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
