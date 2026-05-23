package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.AiToolProvider;
import com.lebhas.ai.domain.ProviderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AiToolProviderRepository extends JpaRepository<AiToolProvider, UUID> {

    Optional<AiToolProvider> findByProviderCodeAndDeletedFalse(String providerCode);

    Optional<AiToolProvider> findByIdAndDeletedFalse(UUID id);

    boolean existsByProviderCodeAndDeletedFalse(String providerCode);

    List<AiToolProvider> findAllByDeletedFalseOrderByProviderNameAsc();

    List<AiToolProvider> findAllByEnabledTrueAndStatusAndDeletedFalseOrderByProviderNameAsc(ProviderStatus status);
}
