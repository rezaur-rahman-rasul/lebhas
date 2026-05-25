package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.payment.application.dto.SubscriptionPaymentSessionView;
import com.lebhas.creativesaas.payment.application.dto.SubscriptionPurchaseCommand;
import com.lebhas.creativesaas.payment.cache.PaymentSessionCacheService;
import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.Invoice;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.SubscriptionOrder;
import com.lebhas.creativesaas.payment.infrastructure.persistence.SubscriptionOrderRepository;
import com.lebhas.creativesaas.workspace.infrastructure.persistence.WorkspaceRepository;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.PricingPlanRepository;
import com.lebhas.pricing.PlanFeaturePolicyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Service
public class SubscriptionPurchaseService {

    private final WorkspaceRepository workspaceRepository;
    private final PricingPlanRepository pricingPlanRepository;
    private final PlanFeaturePolicyRepository planFeaturePolicyRepository;
    private final SubscriptionOrderRepository subscriptionOrderRepository;
    private final PaymentProviderResolver paymentProviderResolver;
    private final PaymentProviderRouter paymentProviderRouter;
    private final PaymentTransactionService paymentTransactionService;
    private final InvoiceService invoiceService;
    private final PaymentSessionCacheService paymentSessionCacheService;
    private final SubscriptionPaymentMapper mapper;
    private final PaymentEventProducer paymentEventProducer;

    public SubscriptionPurchaseService(
            WorkspaceRepository workspaceRepository,
            PricingPlanRepository pricingPlanRepository,
            PlanFeaturePolicyRepository planFeaturePolicyRepository,
            SubscriptionOrderRepository subscriptionOrderRepository,
            PaymentProviderResolver paymentProviderResolver,
            PaymentProviderRouter paymentProviderRouter,
            PaymentTransactionService paymentTransactionService,
            InvoiceService invoiceService,
            PaymentSessionCacheService paymentSessionCacheService,
            SubscriptionPaymentMapper mapper,
            PaymentEventProducer paymentEventProducer
    ) {
        this.workspaceRepository = workspaceRepository;
        this.pricingPlanRepository = pricingPlanRepository;
        this.planFeaturePolicyRepository = planFeaturePolicyRepository;
        this.subscriptionOrderRepository = subscriptionOrderRepository;
        this.paymentProviderResolver = paymentProviderResolver;
        this.paymentProviderRouter = paymentProviderRouter;
        this.paymentTransactionService = paymentTransactionService;
        this.invoiceService = invoiceService;
        this.paymentSessionCacheService = paymentSessionCacheService;
        this.mapper = mapper;
        this.paymentEventProducer = paymentEventProducer;
    }

    @Transactional
    public SubscriptionPaymentSessionView startSubscriptionPurchase(SubscriptionPurchaseCommand command) {
        validateWorkspace(command.workspaceId());
        PricingPlan pricingPlan = requireActivePricingPlan(command.pricingPlanId());
        BillingCycle billingCycle = mapper.billingCycleOrDefault(command.billingCycle());
        BigDecimal amount = amountFor(pricingPlan, billingCycle);

        SubscriptionOrder order = subscriptionOrderRepository.save(SubscriptionOrder.create(
                command.workspaceId(),
                pricingPlan.getId(),
                requireRequestedBy(command.requestedBy()),
                billingCycle,
                amount,
                pricingPlan.getCurrency(),
                null,
                null
        ));

        PaymentSessionRequest providerProbe = paymentSessionRequest(
                order,
                null,
                PaymentPurpose.SUBSCRIPTION_PURCHASE,
                command.environmentType(),
                command.preferredProviderCode()
        );
        PaymentProviderResolver.ResolvedPaymentProvider resolvedProvider = paymentProviderResolver.resolve(providerProbe);
        PaymentTransaction transaction = paymentTransactionService.createTransaction(
                order.getWorkspaceId(),
                order.getRequestedBy(),
                resolvedProvider.provider().getId(),
                PaymentPurpose.SUBSCRIPTION_PURCHASE,
                "subscription_order",
                order.getId(),
                order.getAmount(),
                order.getCurrency()
        );
        order.markPaymentPending(transaction.getId());
        order = subscriptionOrderRepository.save(order);

        PaymentSessionResponse sessionResponse = paymentProviderRouter.createSession(paymentSessionRequest(
                order,
                transaction,
                PaymentPurpose.SUBSCRIPTION_PURCHASE,
                command.environmentType(),
                resolvedProvider.provider().getCode()
        ));
        transaction = paymentTransactionService.markPending(transaction, sessionResponse);
        Invoice invoice = invoiceService.issueSubscriptionInvoice(
                order.getWorkspaceId(),
                transaction.getId(),
                order.getAmount(),
                order.getCurrency()
        );
        paymentSessionCacheService.cacheSession(mapper.toCacheEntry(order, transaction, invoice, sessionResponse));
        paymentEventProducer.subscriptionOrderCreated(order, transaction, pricingPlan);
        return mapper.toSessionView(order, transaction, invoice, pricingPlan, sessionResponse);
    }

    protected void validateWorkspace(UUID workspaceId) {
        if (workspaceId == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_CONTEXT_REQUIRED);
        }
        workspaceRepository.findByIdAndDeletedFalse(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND));
    }

    protected PricingPlan requireActivePricingPlan(UUID pricingPlanId) {
        if (pricingPlanId == null) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Pricing plan id is required");
        }
        return pricingPlanRepository.findByIdAndDeletedFalse(pricingPlanId)
                .filter(PricingPlan::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Active pricing plan not found"));
    }

    protected BigDecimal amountFor(PricingPlan pricingPlan, BillingCycle billingCycle) {
        return switch (billingCycle) {
            case MONTHLY -> pricingPlan.getMonthlyPrice();
            case YEARLY -> pricingPlan.getYearlyPrice();
        };
    }

    protected UUID requireRequestedBy(UUID requestedBy) {
        if (requestedBy == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Payment requester is required");
        }
        return requestedBy;
    }

    protected PaymentSessionRequest paymentSessionRequest(
            SubscriptionOrder order,
            PaymentTransaction transaction,
            PaymentPurpose paymentPurpose,
            com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType environmentType,
            String preferredProviderCode
    ) {
        return new PaymentSessionRequest(
                order.getWorkspaceId(),
                order.getRequestedBy(),
                paymentPurpose,
                order.getAmount(),
                order.getCurrency(),
                "subscription_order",
                order.getId(),
                environmentType,
                preferredProviderCode,
                transaction == null ? null : transaction.getId().toString(),
                Map.of("subscriptionOrderId", order.getId().toString(), "billingCycle", order.getBillingCycle().name())
        );
    }

}
