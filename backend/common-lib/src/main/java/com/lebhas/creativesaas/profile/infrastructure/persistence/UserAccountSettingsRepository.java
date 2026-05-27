package com.lebhas.creativesaas.profile.infrastructure.persistence;

import com.lebhas.creativesaas.profile.domain.UserAccountSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountSettingsRepository extends JpaRepository<UserAccountSettings, UUID> {

    Optional<UserAccountSettings> findByUserIdAndDeletedFalse(UUID userId);

    boolean existsByUserIdAndDeletedFalse(UUID userId);
}
