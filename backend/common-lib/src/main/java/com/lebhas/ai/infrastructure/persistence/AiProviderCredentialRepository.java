package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.AiProviderCredential;
import com.lebhas.ai.domain.ProviderEnvironment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiProviderCredentialRepository extends JpaRepository<AiProviderCredential, UUID> {

    List<AiProviderCredential> findAllByProviderIdAndDeletedFalseOrderByCredentialNameAsc(UUID providerId);

    Optional<AiProviderCredential> findByIdAndProviderIdAndDeletedFalse(UUID id, UUID providerId);

    Optional<AiProviderCredential> findFirstByProviderIdAndActiveTrueAndDeletedFalseOrderByUpdatedAtDesc(UUID providerId);

    Optional<AiProviderCredential> findFirstByProviderIdAndEnvironmentAndDeletedFalseOrderByUpdatedAtDesc(UUID providerId, ProviderEnvironment environment);
}
