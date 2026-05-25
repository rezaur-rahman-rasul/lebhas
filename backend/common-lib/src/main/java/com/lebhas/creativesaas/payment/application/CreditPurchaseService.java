package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchaseCommand;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchasePaymentSessionView;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchaseSettlementView;
import com.lebhas.creativesaas.payment.cache.PaymentSessionCacheService;
import com.lebhas.creativesaas.payment.domain.CreditPackage;
import com.lebhas.creativesaas.payment.domain.CreditPurchaseOrder;
import com.lebhas.creativesaas.payment.domain.PaymentOrderStatus;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.PaymentTransactionStatus;
import com.lebhas.creativesaas.payment.infrastructure.persistence.CreditPackageRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.CreditPurchaseOrderRepository;
import com.lebhas.creativesaas.usage.application.CreditUsageService;
import com.lebhas.creativesaas.usage.application.dto.CreditPurchaseCreditCommand;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageResult;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class CreditPurchaseService {

    private static final String CREDIT_PURCHASE_ORDER_REFERENCE = "credit_purchase_order";

    private final WorkspaceRepository workspaceRepository;
    private final CreditPackageRepository creditPackageRepository;
    private final CreditPurchaseOrderRepository creditPurchaseOrderRepository;
    private final PaymentProviderResolver paymentProviderResolver;
    private final PaymentProviderRouter paymentProviderRouter;
    private final PaymentTransactionService paymentTransactionService;
    private final InvoiceService invoiceService;
    private final PaymentSessionCacheService paymentSessionCacheService;
    private final CreditUsageService creditUsageService;
    private final CreditPaymentMapper mapper;
    private final PaymentEventProducer paymentEventProducer;

    public CreditPurchaseService(
            WorkspaceRepository workspaceRepository,
            CreditPackageRepository creditPackageRepository,
            CreditPurchaseOrderRepository creditPurchaseOrderRepository,
            PaymentProviderResolver paymentProviderResolver,
            PaymentProviderRouter paymentProviderRouter,
            PaymentTransactionService paymentTransactionService,
            InvoiceService invoiceService,
            PaymentSessionCacheService paymentSessionCacheService,
            CreditUsageService creditUsageService,
            CreditPaymentMapper mapper,
            PaymentEventProducer paymentEventProducer
    ) {
        this.workspaceRepository = workspaceRepository;
        this.creditPackageRepository = creditPackageRepository;
        this.creditPurchaseOrderRepository = creditPurchaseOrderRepository;
        this.paymentProviderResolver = paymentProviderResolver;
        this.paymentProviderRouter = paymentProviderRouter;
        this.paymentTransactionService = paymentTransactionService;
        this.invoiceService = invoiceService;
        this.paymentSessionCacheService = paymentSessionCacheService;
        this.creditUsageService = creditUsageService;
        this.mapper = mapper;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Transactional
    public CreditPurchasePaymentSessionView startCreditPurchase(CreditPurchaseCommand command) {
        validateWorkspace(command.workspaceId());
        CreditPackage creditPackage = requireActiveCreditPackage(command.creditPackageId());
        long totalCredits = totalCredits(creditPackage);

        CreditPurchaseOrder order = creditPurchaseOrderRepository.save(CreditPurchaseOrder.create(
                command.workspaceId(),
                creditPackage.getId(),
                requireRequestedBy(command.requestedBy()),
                totalCredits,
                creditPackage.getPrice(),
                creditPackage.getCurrency()
        ));

        PaymentSessionRequest providerProbe = paymentSessionRequest(
                order,
                null,
                command.environmentType(),
                command.preferredProviderCode(),
                creditPackage
        );
        PaymentProviderResolver.ResolvedPaymentProvider resolvedProvider = paymentProviderResolver.resolve(providerProbe);
        PaymentTransaction transaction = paymentTransactionService.createTransaction(
                order.getWorkspaceId(),
                order.getRequestedBy(),
                resolvedProvider.provider().getId(),
                PaymentPurpose.CREDIT_PURCHASE,
                CREDIT_PURCHASE_ORDER_REFERENCE,
                order.getId(),
                order.getAmount(),
                order.getCurrency()
        );
        order.markPaymentPending(transaction.getId());
        order = creditPurchaseOrderRepository.save(order);

        PaymentSessionResponse sessionResponse = paymentProviderRouter.createSession(paymentSessionRequest(
                order,
                transaction,
                command.environmentType(),
                resolvedProvider.provider().getCode(),
                creditPackage
        ));
        transaction = paymentTransactionService.markPending(transaction, sessionResponse);
        invoiceService.issueCreditPurchaseInvoice(
                order.getWorkspaceId(),
                transaction.getId(),
                order.getAmount(),
                order.getCurrency()
        );
        paymentSessionCacheService.cacheSession(mapper.toCacheEntry(order, transaction, sessionResponse));
        paymentEventProducer.creditPurchaseCreated(order, transaction, creditPackage.getCode());
        return mapper.toSessionView(order, transaction, creditPackage, sessionResponse);
    }

    @Transactional
    public CreditPurchaseSettlementView applySuccessfulCreditPurchase(
            UUID paymentTransactionId,
            String providerTransactionId,
            UUID confirmedBy
    ) {
        PaymentTransaction transaction = paymentTransactionService.requireTransaction(paymentTransactionId);
        if (transaction.getPaymentPurpose() != PaymentPurpose.CREDIT_PURCHASE) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Payment transaction is not a credit purchase");
        }
        CreditPurchaseOrder order = creditPurchaseOrderRepository.findByPaymentTransactionId(transaction.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Credit purchase order not found"));
        if (order.getStatus() == PaymentOrderStatus.PAID) {
            if (transaction.getStatus() != PaymentTransactionStatus.SUCCESS) {
                transaction = paymentTransactionService.markSuccess(transaction, providerTransactionId);
                paymentSessionCacheService.invalidate(transaction.getId());
            }
            return mapper.toSettlementView(order, transaction, null);
        }
        if (transaction.getStatus() == PaymentTransactionStatus.SUCCESS) {
            throw new BusinessException(
                    ErrorCode.BUSINESS_RULE_VIOLATION,
                    "Payment transaction is already successful but credits have not been granted");
        }

        CreditUsageResult usageResult = creditUsageService.addPurchasedCredits(new CreditPurchaseCreditCommand(
                order.getWorkspaceId(),
                BigDecimal.valueOf(order.getCredits()),
                CREDIT_PURCHASE_ORDER_REFERENCE,
                order.getId(),
                "Credits purchased through payment transaction " + transaction.getId(),
                confirmedBy == null ? order.getRequestedBy() : confirmedBy
        ));
        order.markPaid();
        order = creditPurchaseOrderRepository.save(order);
        transaction = paymentTransactionService.markSuccess(transaction, providerTransactionId);
        paymentSessionCacheService.invalidate(transaction.getId());
        paymentEventProducer.creditPurchaseCompleted(order, transaction, usageResult);
        return mapper.toSettlementView(order, transaction, usageResult);
    }

    private void validateWorkspace(UUID workspaceId) {
        if (workspaceId == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_CONTEXT_REQUIRED);
        }
        workspaceRepository.findByIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
    }

    private CreditPackage requireActiveCreditPackage(UUID creditPackageId) {
        if (creditPackageId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Credit package id is required");
        }
        return creditPackageRepository.findById(creditPackageId)
                .filter(CreditPackage::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Active credit package not found"));
    }

    private long totalCredits(CreditPackage creditPackage) {
        long total = creditPackage.getCredits() + creditPackage.getBonusCredits();
        if (total <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Credit package must include credits");
        }
        return total;
    }

    private UUID requireRequestedBy(UUID requestedBy) {
        if (requestedBy == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Payment requester is required");
        }
        return requestedBy;
    }

    private PaymentSessionRequest paymentSessionRequest(
            CreditPurchaseOrder order,
            PaymentTransaction transaction,
            com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType environmentType,
            String preferredProviderCode,
            CreditPackage creditPackage
    ) {
        return new PaymentSessionRequest(
                order.getWorkspaceId(),
                order.getRequestedBy(),
                PaymentPurpose.CREDIT_PURCHASE,
                order.getAmount(),
                order.getCurrency(),
                CREDIT_PURCHASE_ORDER_REFERENCE,
                order.getId(),
                environmentType,
                preferredProviderCode,
                transaction == null ? null : transaction.getId().toString(),
                Map.of(
                        "creditPurchaseOrderId", order.getId().toString(),
                        "creditPackageId", creditPackage.getId().toString(),
                        "creditPackageCode", creditPackage.getCode(),
                        "credits", Long.toString(order.getCredits())
                )
        );
    }

}
