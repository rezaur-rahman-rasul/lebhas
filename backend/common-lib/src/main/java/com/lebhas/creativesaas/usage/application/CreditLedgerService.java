package com.lebhas.creativesaas.usage.application;

import com.lebhas.creativesaas.common.api.PagedResult;
import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;
import com.lebhas.creativesaas.usage.domain.CreditLedger;
import com.lebhas.creativesaas.usage.domain.CreditLedgerTransactionType;
import com.lebhas.creativesaas.usage.infrastructure.persistence.CreditLedgerRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class CreditLedgerService {

    private final CreditLedgerRepository creditLedgerRepository;
    private final CreditUsageMapper creditUsageMapper;

    public CreditLedgerService(
            CreditLedgerRepository creditLedgerRepository,
            CreditUsageMapper creditUsageMapper
    ) {
        this.creditLedgerRepository = creditLedgerRepository;
        this.creditUsageMapper = creditUsageMapper;
    }

    @Transactional
    public CreditLedger append(
            UUID workspaceId,
            UUID creativeRequestId,
            UUID generatedVersionId,
            UUID generationJobId,
            CreditLedgerTransactionType transactionType,
            BigDecimal creditsAmount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String referenceType,
            UUID referenceId,
            String description,
            UUID createdBy
    ) {
        return creditLedgerRepository.save(CreditLedger.create(
                workspaceId,
                creativeRequestId,
                generatedVersionId,
                generationJobId,
                transactionType,
                creditsAmount,
                balanceBefore,
                balanceAfter,
                referenceType,
                referenceId,
                description,
                createdBy));
    }

    @Transactional(readOnly = true)
    public List<CreditLedgerView> findWorkspaceLedger(UUID workspaceId) {
        return creditLedgerRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .map(creditUsageMapper::toLedgerView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResult<CreditLedgerView> findWorkspaceLedger(UUID workspaceId, Pageable pageable) {
        return PagedResult.from(creditLedgerRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId, pageable)
                .map(creditUsageMapper::toLedgerView));
    }

    @Transactional(readOnly = true)
    public BigDecimal outstandingReservedCredits(UUID workspaceId, String referenceType, UUID referenceId) {
        List<CreditLedger> entries = creditLedgerRepository.findAllByWorkspaceIdAndReferenceTypeAndReferenceId(
                workspaceId,
                referenceType,
                referenceId);
        BigDecimal reserved = zero();
        BigDecimal settled = zero();
        for (CreditLedger entry : entries) {
            if (entry.getTransactionType() == CreditLedgerTransactionType.RESERVE) {
                reserved = reserved.add(entry.getCreditsAmount());
            } else if (entry.getTransactionType() == CreditLedgerTransactionType.FINALIZE
                    || entry.getTransactionType() == CreditLedgerTransactionType.REFUND) {
                settled = settled.add(entry.getCreditsAmount());
            }
        }
        BigDecimal outstanding = reserved.subtract(settled).setScale(4, RoundingMode.HALF_UP);
        return outstanding.signum() > 0 ? outstanding : zero();
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
    }
}
