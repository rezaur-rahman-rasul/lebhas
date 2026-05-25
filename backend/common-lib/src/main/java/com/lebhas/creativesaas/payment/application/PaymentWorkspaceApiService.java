package com.lebhas.creativesaas.payment.application;

import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.Permission;
import com.lebhas.creativesaas.common.security.Role;
import com.lebhas.creativesaas.identity.application.WorkspaceAuthorizationService;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchaseCommand;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchasePaymentSessionView;
import com.lebhas.creativesaas.payment.application.dto.PlanChangeCommand;
import com.lebhas.creativesaas.payment.application.dto.SubscriptionPaymentSessionView;
import com.lebhas.creativesaas.payment.application.dto.SubscriptionPurchaseCommand;
import com.lebhas.creativesaas.payment.domain.BillingCycle;
import com.lebhas.creativesaas.payment.domain.PaymentEnvironmentType;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentWorkspaceApiService {

    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final SubscriptionPurchaseService subscriptionPurchaseService;
    private final PlanChangeService planChangeService;
    private final CreditPurchaseService creditPurchaseService;

    public PaymentWorkspaceApiService(
            WorkspaceAuthorizationService workspaceAuthorizationService,
            SubscriptionPurchaseService subscriptionPurchaseService,
            PlanChangeService planChangeService,
            CreditPurchaseService creditPurchaseService
    ) {
        this.workspaceAuthorizationService = workspaceAuthorizationService;
        this.subscriptionPurchaseService = subscriptionPurchaseService;
        this.planChangeService = planChangeService;
        this.creditPurchaseService = creditPurchaseService;
    }

    public SubscriptionPaymentSessionView purchaseSubscription(
            UUID workspaceId,
            UUID pricingPlanId,
            BillingCycle billingCycle,
            PaymentEnvironmentType environmentType,
            String preferredProviderCode
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requirePurchaseAccess(workspaceId);
        return subscriptionPurchaseService.startSubscriptionPurchase(new SubscriptionPurchaseCommand(
                access.workspace().getId(),
                access.currentUser().userId(),
                pricingPlanId,
                billingCycle,
                environmentType,
                preferredProviderCode
        ));
    }

    public SubscriptionPaymentSessionView changePlan(
            UUID workspaceId,
            UUID targetPricingPlanId,
            BillingCycle billingCycle,
            PaymentEnvironmentType environmentType,
            String preferredProviderCode
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requirePurchaseAccess(workspaceId);
        return planChangeService.startPlanChange(new PlanChangeCommand(
                access.workspace().getId(),
                access.currentUser().userId(),
                targetPricingPlanId,
                billingCycle,
                environmentType,
                preferredProviderCode
        ));
    }

    public CreditPurchasePaymentSessionView purchaseCredits(
            UUID workspaceId,
            UUID creditPackageId,
            PaymentEnvironmentType environmentType,
            String preferredProviderCode
    ) {
        WorkspaceAuthorizationService.WorkspaceAccess access = requirePurchaseAccess(workspaceId);
        return creditPurchaseService.startCreditPurchase(new CreditPurchaseCommand(
                access.workspace().getId(),
                access.currentUser().userId(),
                creditPackageId,
                environmentType,
                preferredProviderCode
        ));
    }

    private WorkspaceAuthorizationService.WorkspaceAccess requirePurchaseAccess(UUID workspaceId) {
        WorkspaceAuthorizationService.WorkspaceAccess access = workspaceAuthorizationService.requireWorkspaceContext(workspaceId);
        if (access.effectiveRole().isMaster()
                || access.effectiveRole() == Role.ADMIN
                || access.permissions().contains(Permission.WORKSPACE_UPDATE)) {
            return access;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN, "Payment purchase is not permitted for this user");
    }
}
