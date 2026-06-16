package com.lebhas.creativesaas.identity.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_otp_challenges", schema = "platform")
public class AuthOtpChallenge extends BaseEntity {

    @Column(name = "otp_token_hash", nullable = false, length = 128, unique = true)
    private String otpTokenHash;

    @Column(name = "mobile_number", length = 30)
    private String mobileNumber;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "challenge_type", nullable = false, length = 20)
    private String challengeType;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "otp_hash", nullable = false, length = 120)
    private String otpHash;

    @Column(name = "is_new_user", nullable = false)
    private boolean newUser;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "resend_available_at", nullable = false)
    private Instant resendAvailableAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    protected AuthOtpChallenge() {
    }

    public static AuthOtpChallenge create(
            String otpTokenHash,
            String mobileNumber,
            UUID userId,
            String otpHash,
            boolean newUser,
            int maxAttempts,
            Instant expiresAt,
            Instant resendAvailableAt
    ) {
        AuthOtpChallenge challenge = new AuthOtpChallenge();
        challenge.otpTokenHash = otpTokenHash;
        challenge.mobileNumber = mobileNumber;
        challenge.challengeType = "MOBILE";
        challenge.userId = userId;
        challenge.otpHash = otpHash;
        challenge.newUser = newUser;
        challenge.maxAttempts = maxAttempts;
        challenge.expiresAt = expiresAt;
        challenge.resendAvailableAt = resendAvailableAt;
        return challenge;
    }

    public static AuthOtpChallenge createEmail(
            String otpTokenHash,
            String email,
            UUID userId,
            String otpHash,
            int maxAttempts,
            Instant expiresAt,
            Instant resendAvailableAt
    ) {
        AuthOtpChallenge challenge = new AuthOtpChallenge();
        challenge.otpTokenHash = otpTokenHash;
        challenge.email = email;
        challenge.challengeType = "EMAIL";
        challenge.userId = userId;
        challenge.otpHash = otpHash;
        challenge.newUser = false;
        challenge.maxAttempts = maxAttempts;
        challenge.expiresAt = expiresAt;
        challenge.resendAvailableAt = resendAvailableAt;
        return challenge;
    }

    public String getOtpTokenHash() {
        return otpTokenHash;
    }

    public String getMobileNumber() {
        return mobileNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getChallengeType() {
        return challengeType;
    }

    public boolean isMobileChallenge() {
        return "MOBILE".equals(challengeType);
    }

    public boolean isEmailChallenge() {
        return "EMAIL".equals(challengeType);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOtpHash() {
        return otpHash;
    }

    public boolean isNewUser() {
        return newUser;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getResendAvailableAt() {
        return resendAvailableAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public boolean isVerified() {
        return verifiedAt != null;
    }

    public void recordAttempt() {
        this.attempts++;
    }

    public void markVerified(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}
