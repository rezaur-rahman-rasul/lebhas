package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchaseOrderView;
import com.lebhas.creativesaas.payment.application.dto.InvoiceView;
import com.lebhas.creativesaas.payment.application.dto.PaymentTransactionView;
import com.lebhas.creativesaas.payment.application.dto.SubscriptionOrderView;
import com.lebhas.creativesaas.payment.infrastructure.persistence.CreditPurchaseOrderRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.InvoiceRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.PaymentTransactionRepository;
import com.lebhas.creativesaas.payment.infrastructure.persistence.SubscriptionOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class PaymentApiQueryService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final SubscriptionOrderRepository subscriptionOrderRepository;
    private final CreditPurchaseOrderRepository creditPurchaseOrderRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentApiMapper mapper;

    public PaymentApiQueryService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            SubscriptionOrderRepository subscriptionOrderRepository,
            CreditPurchaseOrderRepository creditPurchaseOrderRepository,
            PaymentTransactionRepository paymentTransactionRepository,
            InvoiceRepository invoiceRepository,
            PaymentApiMapper mapper
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.subscriptionOrderRepository = subscriptionOrderRepository;
        this.creditPurchaseOrderRepository = creditPurchaseOrderRepository;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.invoiceRepository = invoiceRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<SubscriptionOrderView> subscriptionOrders(UUID workspaceId) {
        UUID effectiveWorkspaceId = requireWorkspaceAccess(workspaceId);
        return mapper.toSubscriptionOrderViews(subscriptionOrderRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(effectiveWorkspaceId));
    }

    @Transactional(readOnly = true)
    public List<CreditPurchaseOrderView> creditPurchaseOrders(UUID workspaceId) {
        UUID effectiveWorkspaceId = requireWorkspaceAccess(workspaceId);
        return mapper.toCreditPurchaseOrderViews(creditPurchaseOrderRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(effectiveWorkspaceId));
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionView> paymentTransactions(UUID workspaceId) {
        UUID effectiveWorkspaceId = requireWorkspaceAccess(workspaceId);
        return mapper.toPaymentTransactionViews(paymentTransactionRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(effectiveWorkspaceId));
    }

    @Transactional(readOnly = true)
    public PaymentTransactionView paymentTransaction(UUID workspaceId, UUID paymentTransactionId) {
        UUID effectiveWorkspaceId = requireWorkspaceAccess(workspaceId);
        return paymentTransactionRepository.findById(paymentTransactionId)
                .filter(transaction -> transaction.getWorkspaceId().equals(effectiveWorkspaceId))
                .map(mapper::toPaymentTransactionView)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Payment transaction not found"));
    }

    @Transactional(readOnly = true)
    public List<InvoiceView> invoices(UUID workspaceId) {
        UUID effectiveWorkspaceId = requireWorkspaceAccess(workspaceId);
        return mapper.toInvoiceViews(invoiceRepository.findAllByWorkspaceIdOrderByCreatedAtDesc(effectiveWorkspaceId));
    }

    private UUID requireWorkspaceAccess(UUID workspaceId) {
        return workspaceAuthorizationService.requireWorkspaceContext(workspaceId).workspace().getId();
    }
}
