package com.lebhas.creativesaas.campaignpackage.infrastructure.persistence;

import com.lebhas.creativesaas.campaignpackage.domain.BulkGenerationJob;
import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;

public interface BulkGenerationJobRepository extends TenantAwareRepository<BulkGenerationJob> {
}
