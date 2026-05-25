package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.payment.application.CreditPackageService;
import com.lebhas.creativesaas.payment.application.dto.CreditPackageView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/credit-packages")
@Tag(name = "Credit Packages")
public class CreditPackageController {

    private final CreditPackageService creditPackageService;

    public CreditPackageController(CreditPackageService creditPackageService) {
        this.creditPackageService = creditPackageService;
    }

    @GetMapping("/public")
    @Operation(summary = "List active credit packages", security = {})
    public ApiResponse<List<CreditPackageView>> listPublicCreditPackages() {
        return ApiResponse.success(creditPackageService.listActiveCreditPackages());
    }
}
