package com.lebhas.creativesaas.identity.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "onboarding_reward_policies", schema = "platform")
public class OnboardingRewardPolicy extends BaseEntity {

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "signup_free_credits", nullable = false, precision = 18, scale = 4)
    private BigDecimal signupFreeCredits;

    @Column(name = "enable_signup_free_credits", nullable = false)
    private boolean enableSignupFreeCredits;

    @Column(name = "email_reward_credits", nullable = false, precision = 18, scale = 4)
    private BigDecimal emailRewardCredits;

    @Column(name = "enable_email_reward", nullable = false)
    private boolean enableEmailReward;

    @Column(name = "facebook_reward_credits", nullable = false, precision = 18, scale = 4)
    private BigDecimal facebookRewardCredits;

    @Column(name = "enable_facebook_reward", nullable = false)
    private boolean enableFacebookReward;

    @Column(name = "instagram_reward_credits", nullable = false, precision = 18, scale = 4)
    private BigDecimal instagramRewardCredits;

    @Column(name = "enable_instagram_reward", nullable = false)
    private boolean enableInstagramReward;

    @Column(name = "enable_profile_rewards", nullable = false)
    private boolean enableProfileRewards;

    @Column(name = "reward_only_once", nullable = false)
    private boolean rewardOnlyOnce;

    @Column(name = "enable_mobile_otp_login", nullable = false)
    private boolean enableMobileOtpLogin;

    @Column(name = "otp_expiry_minutes", nullable = false)
    private int otpExpiryMinutes;

    @Column(name = "otp_resend_cooldown_seconds", nullable = false)
    private int otpResendCooldownSeconds;

    @Column(name = "max_otp_attempts", nullable = false)
    private int maxOtpAttempts;

    protected OnboardingRewardPolicy() {
    }

    public static OnboardingRewardPolicy create(
            boolean active,
            boolean enableSignupFreeCredits,
            BigDecimal signupFreeCredits,
            boolean enableEmailReward,
            BigDecimal emailRewardCredits,
            boolean enableFacebookReward,
            BigDecimal facebookRewardCredits,
            boolean enableInstagramReward,
            BigDecimal instagramRewardCredits,
            boolean rewardOnlyOnce,
            boolean enableMobileOtpLogin,
            int otpExpiryMinutes,
            int otpResendCooldownSeconds,
            int maxOtpAttempts
    ) {
        OnboardingRewardPolicy policy = new OnboardingRewardPolicy();
        policy.update(
                active,
                enableSignupFreeCredits,
                signupFreeCredits,
                enableEmailReward,
                emailRewardCredits,
                enableFacebookReward,
                facebookRewardCredits,
                enableInstagramReward,
                instagramRewardCredits,
                rewardOnlyOnce,
                enableMobileOtpLogin,
                otpExpiryMinutes,
                otpResendCooldownSeconds,
                maxOtpAttempts);
        return policy;
    }

    public boolean isActive() { return active; }
    public boolean isEnableSignupFreeCredits() { return enableSignupFreeCredits; }
    public BigDecimal getSignupFreeCredits() { return signupFreeCredits; }
    public boolean isEnableEmailReward() { return enableEmailReward; }
    public BigDecimal getEmailRewardCredits() { return emailRewardCredits; }
    public boolean isEnableFacebookReward() { return enableFacebookReward; }
    public BigDecimal getFacebookRewardCredits() { return facebookRewardCredits; }
    public boolean isEnableInstagramReward() { return enableInstagramReward; }
    public BigDecimal getInstagramRewardCredits() { return instagramRewardCredits; }
    public boolean isEnableProfileRewards() { return enableProfileRewards; }
    public boolean isRewardOnlyOnce() { return rewardOnlyOnce; }
    public boolean isEnableMobileOtpLogin() { return enableMobileOtpLogin; }
    public int getOtpExpiryMinutes() { return otpExpiryMinutes; }
    public int getOtpResendCooldownSeconds() { return otpResendCooldownSeconds; }
    public int getMaxOtpAttempts() { return maxOtpAttempts; }

    public void update(
            boolean active,
            boolean enableSignupFreeCredits,
            BigDecimal signupFreeCredits,
            boolean enableEmailReward,
            BigDecimal emailRewardCredits,
            boolean enableFacebookReward,
            BigDecimal facebookRewardCredits,
            boolean enableInstagramReward,
            BigDecimal instagramRewardCredits,
            boolean rewardOnlyOnce,
            boolean enableMobileOtpLogin,
            int otpExpiryMinutes,
            int otpResendCooldownSeconds,
            int maxOtpAttempts
    ) {
        this.active = active;
        this.enableSignupFreeCredits = enableSignupFreeCredits;
        this.signupFreeCredits = nonNegative(signupFreeCredits);
        this.enableEmailReward = enableEmailReward;
        this.emailRewardCredits = nonNegative(emailRewardCredits);
        this.enableFacebookReward = enableFacebookReward;
        this.facebookRewardCredits = nonNegative(facebookRewardCredits);
        this.enableInstagramReward = enableInstagramReward;
        this.instagramRewardCredits = nonNegative(instagramRewardCredits);
        this.enableProfileRewards = enableEmailReward || enableFacebookReward || enableInstagramReward;
        this.rewardOnlyOnce = rewardOnlyOnce;
        this.enableMobileOtpLogin = enableMobileOtpLogin;
        this.otpExpiryMinutes = Math.max(1, otpExpiryMinutes);
        this.otpResendCooldownSeconds = Math.max(15, otpResendCooldownSeconds);
        this.maxOtpAttempts = Math.max(1, maxOtpAttempts);
    }

    private static BigDecimal nonNegative(BigDecimal value) {
        BigDecimal resolved = value == null ? BigDecimal.ZERO : value;
        return resolved.signum() < 0 ? BigDecimal.ZERO.setScale(4) : resolved.setScale(4);
    }
}
