package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.payment.application.PaymentApiQueryService;
import com.lebhas.creativesaas.payment.application.PaymentWorkspaceApiService;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchaseOrderView;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchasePaymentSessionView;
import com.lebhas.creativesaas.payment.application.dto.InvoiceView;
import com.lebhas.creativesaas.payment.application.dto.PaymentProviderView;
import com.lebhas.creativesaas.payment.application.dto.PaymentTransactionView;
import com.lebhas.creativesaas.payment.application.dto.SubscriptionOrderView;
import com.lebhas.creativesaas.payment.application.dto.SubscriptionPaymentSessionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}")
@Tag(name = "Workspace Payments")
@SecurityRequirement(name = "bearerAuth")
public class WorkspacePaymentController {

    private final PaymentWorkspaceApiService paymentWorkspaceApiService;
    private final PaymentApiQueryService paymentApiQueryService;

    public WorkspacePaymentController(
            PaymentWorkspaceApiService paymentWorkspaceApiService,
            PaymentApiQueryService paymentApiQueryService
    ) {
        this.paymentWorkspaceApiService = paymentWorkspaceApiService;
        this.paymentApiQueryService = paymentApiQueryService;
    }

    @PostMapping("/subscriptions/purchase")
    @Operation(summary = "Start subscription purchase payment")
    public ApiResponse<SubscriptionPaymentSessionView> purchaseSubscription(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody SubscriptionPurchaseRequest request
    ) {
        return ApiResponse.success(paymentWorkspaceApiService.purchaseSubscription(
                workspaceId,
                request.pricingPlanId(),
                request.billingCycle(),
                request.environmentType(),
                request.preferredProviderCode()));
    }

    @PostMapping("/subscriptions/change-plan")
    @Operation(summary = "Start subscription plan change payment")
    public ApiResponse<SubscriptionPaymentSessionView> changePlan(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody PlanChangeRequest request
    ) {
        return ApiResponse.success(paymentWorkspaceApiService.changePlan(
                workspaceId,
                request.targetPricingPlanId(),
                request.billingCycle(),
                request.environmentType(),
                request.preferredProviderCode()));
    }

    @PostMapping("/subscriptions/upgrade")
    @Operation(summary = "Start subscription upgrade payment")
    public ApiResponse<SubscriptionPaymentSessionView> upgradeSubscription(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody PlanChangeRequest request
    ) {
        return changePlan(workspaceId, request);
    }

    @PostMapping("/subscriptions/renew")
    @Operation(summary = "Start subscription renewal payment")
    public ApiResponse<SubscriptionPaymentSessionView> renewSubscription(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody SubscriptionRenewRequest request
    ) {
        return ApiResponse.success(paymentWorkspaceApiService.renewSubscription(
                workspaceId,
                request.billingCycle(),
                request.environmentType(),
                request.preferredProviderCode()));
    }

    @GetMapping("/subscriptions/orders")
    @Operation(summary = "List subscription orders")
    public ApiResponse<List<SubscriptionOrderView>> subscriptionOrders(@PathVariable UUID workspaceId) {
        return ApiResponse.success(paymentApiQueryService.subscriptionOrders(workspaceId));
    }

    @PostMapping("/credits/purchase")
    @Operation(summary = "Start credit purchase payment")
    public ApiResponse<CreditPurchasePaymentSessionView> purchaseCredits(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody CreditPurchaseRequest request
    ) {
        return ApiResponse.success(paymentWorkspaceApiService.purchaseCredits(
                workspaceId,
                request.creditPackageId(),
                request.environmentType(),
                request.preferredProviderCode()));
    }

    @GetMapping("/payment-gateways")
    @Operation(summary = "List enabled payment gateways available to a workspace")
    public ApiResponse<List<PaymentProviderView>> paymentGateways(@PathVariable UUID workspaceId) {
        return ApiResponse.success(paymentApiQueryService.enabledPaymentGateways(workspaceId));
    }

    @GetMapping("/credits/purchase-orders")
    @Operation(summary = "List credit purchase orders")
    public ApiResponse<List<CreditPurchaseOrderView>> creditPurchaseOrders(@PathVariable UUID workspaceId) {
        return ApiResponse.success(paymentApiQueryService.creditPurchaseOrders(workspaceId));
    }

    @GetMapping("/payments/transactions")
    @Operation(summary = "List payment transactions")
    public ApiResponse<List<PaymentTransactionView>> paymentTransactions(@PathVariable UUID workspaceId) {
        return ApiResponse.success(paymentApiQueryService.paymentTransactions(workspaceId));
    }

    @GetMapping("/payments")
    @Operation(summary = "List payment transactions")
    public ApiResponse<List<PaymentTransactionView>> payments(@PathVariable UUID workspaceId) {
        return paymentTransactions(workspaceId);
    }

    @GetMapping("/payments/{paymentTransactionId}")
    @Operation(summary = "Get payment transaction details")
    public ApiResponse<PaymentTransactionView> payment(
            @PathVariable UUID workspaceId,
            @PathVariable UUID paymentTransactionId
    ) {
        return ApiResponse.success(paymentApiQueryService.paymentTransaction(workspaceId, paymentTransactionId));
    }

    @GetMapping("/invoices")
    @Operation(summary = "List invoices")
    public ApiResponse<List<InvoiceView>> invoices(@PathVariable UUID workspaceId) {
        return ApiResponse.success(paymentApiQueryService.invoices(workspaceId));
    }
}
