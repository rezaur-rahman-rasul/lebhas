package com.lebhas.creativesaas.profile.infrastructure.persistence;

import com.lebhas.creativesaas.profile.domain.ProfileRewardClaim;
import com.lebhas.creativesaas.profile.domain.ProfileRewardType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProfileRewardClaimRepository extends JpaRepository<ProfileRewardClaim, UUID> {

    boolean existsByWorkspaceIdAndUserIdAndRewardType(UUID workspaceId, UUID userId, ProfileRewardType rewardType);

    List<ProfileRewardClaim> findAllByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);
}
