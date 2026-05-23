package com.lebhas.ai.infrastructure.persistence;

import com.lebhas.ai.domain.AiFailureLog;
import com.lebhas.ai.domain.AiFailureType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiFailureLogRepository extends JpaRepository<AiFailureLog, UUID> {

    List<AiFailureLog> findAllByCreativeRequestIdAndDeletedFalseOrderByCreatedAtDesc(UUID creativeRequestId);

    List<AiFailureLog> findAllByLayerIdAndDeletedFalseOrderByCreatedAtDesc(UUID layerId);

    List<AiFailureLog> findAllByProviderIdAndDeletedFalseOrderByCreatedAtDesc(UUID providerId);

    List<AiFailureLog> findAllByFailureTypeAndDeletedFalseOrderByCreatedAtDesc(AiFailureType failureType);
}
