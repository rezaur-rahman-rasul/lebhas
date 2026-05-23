package com.lebhas.creativesaas.asset.infrastructure.persistence;

import com.lebhas.creativesaas.asset.domain.UploadSessionEntity;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

public interface UploadSessionRepository extends TenantAwareRepository<UploadSessionEntity> {
}
