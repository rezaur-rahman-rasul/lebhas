package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.usage.application.CreditBalanceService;
import com.lebhas.creativesaas.usage.application.CreditUsageService;
import com.lebhas.creativesaas.usage.application.UsageBillingApiQueryService;
import com.lebhas.creativesaas.usage.application.dto.CreditBalanceView;
import com.lebhas.creativesaas.usage.application.dto.CreditLedgerView;
import com.lebhas.creativesaas.usage.application.dto.CreditUsageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Workspace Credits")
@SecurityRequirement(name = "bearerAuth")
public class WorkspaceCreditController {

    private final CreditBalanceService creditBalanceService;
    private final UsageBillingApiQueryService usageBillingApiQueryService;
    private final CreditUsageService creditUsageService;

    public WorkspaceCreditController(
            CreditBalanceService creditBalanceService,
            UsageBillingApiQueryService usageBillingApiQueryService,
            CreditUsageService creditUsageService
    ) {
        this.creditBalanceService = creditBalanceService;
        this.usageBillingApiQueryService = usageBillingApiQueryService;
        this.creditUsageService = creditUsageService;
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/credits")
    @Operation(summary = "Get workspace credit account")
    public ApiResponse<CreditBalanceView> getCredits(@PathVariable UUID workspaceId) {
        return ApiResponse.success(creditBalanceService.getBalance(workspaceId));
    }

    @GetMapping("/api/v1/workspaces/{workspaceId}/credits/ledger/all")
    @Operation(summary = "List workspace credit ledger entries")
    public ApiResponse<List<CreditLedgerView>> getCreditLedger(@PathVariable UUID workspaceId) {
        return ApiResponse.success(usageBillingApiQueryService.creditLedger(workspaceId));
    }

    @PostMapping("/api/v1/master/workspaces/{workspaceId}/credits/adjust")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Adjust workspace credits")
    public ApiResponse<CreditUsageResult> adjustCredits(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody MasterCreditAdjustmentRequest request
    ) {
        return ApiResponse.success(creditUsageService.adjustCredits(
                workspaceId,
                request.creditsAmount(),
                request.referenceType(),
                request.referenceId(),
                request.description()));
    }
}
