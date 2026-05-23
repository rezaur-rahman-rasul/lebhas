package com.lebhas.creativesaas.identity.infrastructure.persistence;

import com.lebhas.creativesaas.identity.domain.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenIdAndDeletedFalse(UUID tokenId);

    List<RefreshTokenEntity> findAllByUserIdAndDeletedFalse(UUID userId);

    List<RefreshTokenEntity> findAllByUserIdAndDeviceIdAndDeletedFalse(UUID userId, String deviceId);
}
