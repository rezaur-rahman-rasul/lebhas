package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.exception.BusinessException;
import com.lebhas.creativesaas.common.exception.ErrorCode;
import com.lebhas.creativesaas.common.security.context.CurrentUserContext;
import com.lebhas.creativesaas.payment.application.PaymentApiQueryService;
import com.lebhas.creativesaas.payment.application.PaymentWorkspaceApiService;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchaseOrderView;
import com.lebhas.creativesaas.payment.application.dto.CreditPurchasePaymentSessionView;
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
@RequestMapping("/api/v1/workspace/billing/credit-purchases")
@Tag(name = "Workspace Credit Purchases")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceCreditPurchaseController {

    private final CurrentUserContext currentUserContext;
    private final PaymentWorkspaceApiService paymentWorkspaceApiService;
    private final PaymentApiQueryService paymentApiQueryService;

    public WorkspaceCreditPurchaseController(
            CurrentUserContext currentUserContext,
            PaymentWorkspaceApiService paymentWorkspaceApiService,
            PaymentApiQueryService paymentApiQueryService
    ) {
        this.currentUserContext = currentUserContext;
        this.paymentWorkspaceApiService = paymentWorkspaceApiService;
        this.paymentApiQueryService = paymentApiQueryService;
    }

    @PostMapping("/initiate")
    @Operation(summary = "Initiate credit purchase for active workspace")
    public ApiResponse<CreditPurchasePaymentSessionView> initiate(@Valid @RequestBody CreditPurchaseRequest request) {
        return ApiResponse.success(paymentWorkspaceApiService.purchaseCredits(
                workspaceId(),
                request.creditPackageId(),
                request.environmentType(),
                request.preferredProviderCode()));
    }

    @GetMapping
    @Operation(summary = "List credit purchases for active workspace")
    public ApiResponse<List<CreditPurchaseOrderView>> list() {
        return ApiResponse.success(paymentApiQueryService.creditPurchaseOrders(workspaceId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get credit purchase for active workspace")
    public ApiResponse<CreditPurchaseOrderView> detail(@PathVariable UUID id) {
        return ApiResponse.success(paymentApiQueryService.creditPurchaseOrder(workspaceId(), id));
    }

    private UUID workspaceId() {
        UUID workspaceId = currentUserContext.requireCurrentUser().workspaceId();
        if (workspaceId == null) {
            throw new BusinessException(ErrorCode.WORKSPACE_CONTEXT_REQUIRED);
        }
        return workspaceId;
    }
}
