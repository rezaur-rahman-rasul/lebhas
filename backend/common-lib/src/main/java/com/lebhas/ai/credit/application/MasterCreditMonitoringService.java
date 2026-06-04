package com.lebhas.ai.credit.application;

import com.lebhas.ai.credit.application.dto.MasterCreditOverviewView;
import com.lebhas.ai.credit.application.dto.MasterWorkspaceCreditView;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolView;
import com.lebhas.creativesaas.credit.domain.CreditWalletEntity;
import com.lebhas.creativesaas.credit.infrastructure.persistence.CreditWalletRepository;
import com.lebhas.creativesaas.usage.application.AdminFreeCreditAllocationService;
import com.lebhas.creativesaas.usage.application.CreditBalanceService;
import com.lebhas.creativesaas.usage.application.CreditLedgerService;
import com.lebhas.creativesaas.usage.application.CreditUsageMapper;
import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;
import com.lebhas.creativesaas.usage.domain.CreditLedger;
import com.lebhas.creativesaas.usage.domain.CreditLedgerTransactionType;
import com.lebhas.creativesaas.usage.infrastructure.persistence.CreditLedgerRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class MasterCreditMonitoringService {

    private final ProviderCreditPoolService providerCreditPoolService;
    private final CreditWalletRepository creditWalletRepository;
    private final CreditLedgerRepository creditLedgerRepository;
    private final CreditUsageMapper creditUsageMapper;
    private final CreditBalanceService creditBalanceService;
    private final CreditLedgerService creditLedgerService;
    private final AdminFreeCreditAllocationService freeCreditAllocationService;

    public MasterCreditMonitoringService(
            ProviderCreditPoolService providerCreditPoolService,
            CreditWalletRepository creditWalletRepository,
            CreditLedgerRepository creditLedgerRepository,
            CreditUsageMapper creditUsageMapper,
            CreditBalanceService creditBalanceService,
            CreditLedgerService creditLedgerService,
            AdminFreeCreditAllocationService freeCreditAllocationService
    ) {
        this.providerCreditPoolService = providerCreditPoolService;
        this.creditWalletRepository = creditWalletRepository;
        this.creditLedgerRepository = creditLedgerRepository;
        this.creditUsageMapper = creditUsageMapper;
        this.creditBalanceService = creditBalanceService;
        this.creditLedgerService = creditLedgerService;
        this.freeCreditAllocationService = freeCreditAllocationService;
    }

    @Transactional(readOnly = true)
    public MasterCreditOverviewView overview() {
        List<ProviderCreditPoolView> pools = providerCreditPoolService.listPools();
        List<CreditWalletEntity> wallets = creditWalletRepository.findAll();
        List<CreditLedger> ledger = creditLedgerRepository.findAll();
        List<CreditLedgerView> recentLedger = creditLedgerRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20))
                .map(creditUsageMapper::toLedgerView)
                .getContent();

        return new MasterCreditOverviewView(
                pools.stream().map(ProviderCreditPoolView::internalCreditEquivalent).reduce(zero(), BigDecimal::add),
                wallets.stream().map(CreditWalletEntity::getAvailableBalance).reduce(zero(), BigDecimal::add),
                wallets.stream().map(CreditWalletEntity::getReservedBalance).reduce(zero(), BigDecimal::add),
                ledger.stream()
                        .filter(entry -> entry.getTransactionType() == CreditLedgerTransactionType.FINALIZE)
                        .map(CreditLedger::getCreditsAmount)
                        .reduce(zero(), BigDecimal::add),
                ledger.stream()
                        .filter(entry -> entry.getTransactionType() == CreditLedgerTransactionType.FREE_SIGNUP_CREDIT_GRANTED)
                        .map(CreditLedger::getCreditsAmount)
                        .reduce(zero(), BigDecimal::add),
                pools,
                pools.stream().filter(ProviderCreditPoolView::lowBalance).toList(),
                recentLedger);
    }

    @Transactional(readOnly = true)
    public MasterWorkspaceCreditView workspaceCredits(UUID workspaceId) {
        Instant grantedAt = freeCreditAllocationService.grantedAt(workspaceId).orElse(null);
        return new MasterWorkspaceCreditView(
                creditBalanceService.getBalance(workspaceId),
                grantedAt != null,
                grantedAt,
                creditLedgerService.findWorkspaceLedger(workspaceId));
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(4);
    }
}
