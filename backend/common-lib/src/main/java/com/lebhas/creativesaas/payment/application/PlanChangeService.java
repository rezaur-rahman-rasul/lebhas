package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.payment.application.dto.PlanChangeCommand;
import com.lebhas.creativesaas.payment.application.dto.SubscriptionPaymentSessionView;
import com.lebhas.creativesaas.payment.cache.PaymentSessionCacheService;
import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.Invoice;
import com.lebhas.creativesaas.payment.domain.PaymentPurpose;
import com.lebhas.creativesaas.payment.domain.PaymentTransaction;
import com.lebhas.creativesaas.payment.domain.SubscriptionOrder;
import com.lebhas.creativesaas.payment.infrastructure.persistence.SubscriptionOrderRepository;
import com.lebhas.pricing.PricingPlan;
import com.lebhas.pricing.WorkspaceSubscription;
import com.lebhas.pricing.WorkspaceSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PlanChangeService {

    private final SubscriptionPurchaseService subscriptionPurchaseService;
    private final WorkspaceSubscriptionRepository workspaceSubscriptionRepository;
    private final SubscriptionOrderRepository subscriptionOrderRepository;
    private final PaymentProviderResolver paymentProviderResolver;
    private final PaymentProviderRouter paymentProviderRouter;
    private final PaymentTransactionService paymentTransactionService;
    private final InvoiceService invoiceService;
    private final PaymentSessionCacheService paymentSessionCacheService;
    private final SubscriptionPaymentMapper mapper;
    private final PaymentEventProducer paymentEventProducer;

    public PlanChangeService(
            SubscriptionPurchaseService subscriptionPurchaseService,
            WorkspaceSubscriptionRepository workspaceSubscriptionRepository,
            SubscriptionOrderRepository subscriptionOrderRepository,
            PaymentProviderResolver paymentProviderResolver,
            PaymentProviderRouter paymentProviderRouter,
            PaymentTransactionService paymentTransactionService,
            InvoiceService invoiceService,
            PaymentSessionCacheService paymentSessionCacheService,
            SubscriptionPaymentMapper mapper,
            PaymentEventProducer paymentEventProducer
    ) {
        this.subscriptionPurchaseService = subscriptionPurchaseService;
        this.workspaceSubscriptionRepository = workspaceSubscriptionRepository;
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
    public SubscriptionPaymentSessionView startPlanChange(PlanChangeCommand command) {
        subscriptionPurchaseService.validateWorkspace(command.workspaceId());
        PricingPlan targetPlan = subscriptionPurchaseService.requireActivePricingPlan(command.targetPricingPlanId());
        WorkspaceSubscription currentSubscription = workspaceSubscriptionRepository
                .findFirstByWorkspaceIdAndDeletedFalse(command.workspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PLAN_SUBSCRIPTION_INACTIVE));
        if (currentSubscription.getPricingPlanId().equals(targetPlan.getId())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Workspace is already assigned to this pricing plan");
        }

        BillingCycle billingCycle = mapper.billingCycleOrDefault(command.billingCycle());
        BigDecimal amount = subscriptionPurchaseService.amountFor(targetPlan, billingCycle);
        SubscriptionOrder order = subscriptionOrderRepository.save(SubscriptionOrder.create(
                command.workspaceId(),
                targetPlan.getId(),
                subscriptionPurchaseService.requireRequestedBy(command.requestedBy()),
                billingCycle,
                amount,
                targetPlan.getCurrency(),
                null,
                null
        ));

        PaymentSessionRequest providerProbe = subscriptionPurchaseService.paymentSessionRequest(
                order,
                null,
                PaymentPurpose.PLAN_UPGRADE,
                command.environmentType(),
                command.preferredProviderCode()
        );
        PaymentProviderResolver.ResolvedPaymentProvider resolvedProvider = paymentProviderResolver.resolve(providerProbe);
        PaymentTransaction transaction = paymentTransactionService.createTransaction(
                order.getWorkspaceId(),
                order.getRequestedBy(),
                resolvedProvider.provider().getId(),
                PaymentPurpose.PLAN_UPGRADE,
                "subscription_order",
                order.getId(),
                order.getAmount(),
                order.getCurrency()
        );
        order.markPaymentPending(transaction.getId());
        order = subscriptionOrderRepository.save(order);

        PaymentSessionResponse sessionResponse = paymentProviderRouter.createSession(subscriptionPurchaseService.paymentSessionRequest(
                order,
                transaction,
                PaymentPurpose.PLAN_UPGRADE,
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
        paymentEventProducer.subscriptionChanged(order, transaction, targetPlan);
        return mapper.toSessionView(order, transaction, invoice, targetPlan, sessionResponse);
    }
}
