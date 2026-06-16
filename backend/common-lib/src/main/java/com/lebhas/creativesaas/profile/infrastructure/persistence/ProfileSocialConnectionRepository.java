package com.lebhas.creativesaas.profile.infrastructure.persistence;

import com.lebhas.creativesaas.profile.domain.ProfileSocialConnection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileSocialConnectionRepository extends JpaRepository<ProfileSocialConnection, UUID> {

    Optional<ProfileSocialConnection> findByUserIdAndProviderAndDeletedFalse(UUID userId, String provider);
}
