package com.lebhas.creativesaas.identity.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registration_sessions", schema = "platform")
public class RegistrationSession extends BaseEntity {

    @Column(name = "session_token_hash", nullable = false, length = 128, unique = true)
    private String sessionTokenHash;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "current_step", nullable = false, length = 40)
    private String currentStep;

    @Column(name = "mobile_number", nullable = false, length = 30)
    private String mobileNumber;

    @Column(name = "pending_email", length = 160)
    private String pendingEmail;

    @Column(name = "selected_brand_id")
    private UUID selectedBrandId;

    @Column(name = "selected_product_service_id")
    private UUID selectedProductServiceId;

    @Column(name = "selected_project_campaign_id")
    private UUID selectedProjectCampaignId;

    @Column(name = "new_user", nullable = false)
    private boolean newUser;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected RegistrationSession() {
    }

    public static RegistrationSession create(
            String sessionTokenHash,
            UUID userId,
            UUID workspaceId,
            String mobileNumber,
            boolean newUser,
            Instant expiresAt
    ) {
        RegistrationSession session = new RegistrationSession();
        session.sessionTokenHash = sessionTokenHash;
        session.userId = userId;
        session.workspaceId = workspaceId;
        session.mobileNumber = mobileNumber;
        session.newUser = newUser;
        session.currentStep = "MOBILE_OTP";
        session.expiresAt = expiresAt;
        return session;
    }

    public String getSessionTokenHash() { return sessionTokenHash; }
    public UUID getUserId() { return userId; }
    public UUID getWorkspaceId() { return workspaceId; }
    public String getCurrentStep() { return currentStep; }
    public String getMobileNumber() { return mobileNumber; }
    public String getPendingEmail() { return pendingEmail; }
    public UUID getSelectedBrandId() { return selectedBrandId; }
    public UUID getSelectedProductServiceId() { return selectedProductServiceId; }
    public UUID getSelectedProjectCampaignId() { return selectedProjectCampaignId; }
    public boolean isNewUser() { return newUser; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCompletedAt() { return completedAt; }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean isCompleted() {
        return completedAt != null;
    }

    public void advanceTo(String step) {
        this.currentStep = step;
    }

    public void attachPendingEmail(String pendingEmail) {
        this.pendingEmail = pendingEmail;
        this.currentStep = "EMAIL_OTP";
    }

    public void clearPendingEmail() {
        this.pendingEmail = null;
    }

    public void selectBrand(UUID brandId) {
        this.selectedBrandId = brandId;
        this.currentStep = "PRODUCT_SERVICE_NAME";
    }

    public void selectProductService(UUID productServiceId) {
        this.selectedProductServiceId = productServiceId;
        this.currentStep = "PROJECT_CAMPAIGN_NAME";
    }

    public void selectProjectCampaign(UUID projectCampaignId) {
        this.selectedProjectCampaignId = projectCampaignId;
    }

    public void complete(Instant completedAt) {
        this.completedAt = completedAt;
        this.currentStep = "COMPLETED";
    }
}
