package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.usage.application.dto.UsageBillingLogView;
import com.lebhas.creativesaas.usage.infrastructure.persistence.UsageBillingLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UsageBillingQueryService {

    private final UsageBillingLogRepository usageBillingLogRepository;
    private final UsageBillingMapper usageBillingMapper;

    public UsageBillingQueryService(
            UsageBillingLogRepository usageBillingLogRepository,
            UsageBillingMapper usageBillingMapper
    ) {
        this.usageBillingLogRepository = usageBillingLogRepository;
        this.usageBillingMapper = usageBillingMapper;
    }

    @Transactional(readOnly = true)
    public List<UsageBillingLogView> findWorkspaceBillingLogs(UUID workspaceId) {
        return usageBillingLogRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(require(workspaceId, "workspaceId"))
                .stream()
                .map(usageBillingMapper::toView)
                .toList();
    }

    private <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
