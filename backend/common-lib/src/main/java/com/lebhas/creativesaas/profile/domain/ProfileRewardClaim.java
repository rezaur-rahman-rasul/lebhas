package com.lebhas.creativesaas.profile.domain;

import com.lebhas.creativesaas.common.audit.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "profile_reward_claims",
        schema = "platform",
        uniqueConstraints = @UniqueConstraint(name = "uk_profile_reward_claim_workspace_user_type", columnNames = {"workspace_id", "user_id", "reward_type"})
)
public class ProfileRewardClaim extends BaseEntity {

    @Column(name = "workspace_id", nullable = false)
    private UUID workspaceId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reward_type", nullable = false, length = 30)
    private ProfileRewardType rewardType;

    @Column(name = "credits_amount", nullable = false, precision = 18, scale = 4)
    private BigDecimal creditsAmount;

    @Column(name = "ledger_entry_id")
    private UUID ledgerEntryId;

    protected ProfileRewardClaim() {
    }

    public static ProfileRewardClaim create(UUID workspaceId, UUID userId, ProfileRewardType rewardType, BigDecimal creditsAmount, UUID ledgerEntryId) {
        ProfileRewardClaim claim = new ProfileRewardClaim();
        claim.workspaceId = workspaceId;
        claim.userId = userId;
        claim.rewardType = rewardType;
        claim.creditsAmount = creditsAmount;
        claim.ledgerEntryId = ledgerEntryId;
        return claim;
    }
}
