package com.lebhas.creativesaas.texttool.infrastructure.persistence;

import com.lebhas.creativesaas.common.jpa.TenantAwareRepository;
import com.lebhas.creativesaas.texttool.domain.CreativeTextToolOutput;

import java.util.UUID;

public interface CreativeTextToolOutputRepository extends TenantAwareRepository<CreativeTextToolOutput> {
}
