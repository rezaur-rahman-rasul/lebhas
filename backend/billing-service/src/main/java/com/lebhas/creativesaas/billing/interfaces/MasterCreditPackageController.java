package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.payment.application.CreditPackageService;
import com.lebhas.creativesaas.payment.application.dto.CreditPackageCommand;
import com.lebhas.creativesaas.payment.application.dto.CreditPackageView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/credit-packages")
@Tag(name = "Master Credit Packages")
@SecurityRequirement(name = "bearerAuth")
public class MasterCreditPackageController {

    private final CreditPackageService creditPackageService;

    public MasterCreditPackageController(CreditPackageService creditPackageService) {
        this.creditPackageService = creditPackageService;
    }

    @PostMapping
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create a credit package")
    public ApiResponse<CreditPackageView> createCreditPackage(@Valid @RequestBody CreateCreditPackageRequest request) {
        return ApiResponse.success(creditPackageService.createCreditPackage(createCommand(null, request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List credit packages")
    public ApiResponse<List<CreditPackageView>> listCreditPackages() {
        return ApiResponse.success(creditPackageService.listCreditPackagesForMaster());
    }

    @GetMapping("/{creditPackageId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get credit package details")
    public ApiResponse<CreditPackageView> getCreditPackage(@PathVariable UUID creditPackageId) {
        return ApiResponse.success(creditPackageService.getCreditPackageForMaster(creditPackageId));
    }

    @PutMapping("/{creditPackageId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update a credit package")
    public ApiResponse<CreditPackageView> updateCreditPackage(
            @PathVariable UUID creditPackageId,
            @Valid @RequestBody UpdateCreditPackageRequest request
    ) {
        return ApiResponse.success(creditPackageService.updateCreditPackage(updateCommand(creditPackageId, request)));
    }

    @DeleteMapping("/{creditPackageId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Disable a credit package")
    public ApiResponse<CreditPackageView> disableCreditPackage(@PathVariable UUID creditPackageId) {
        return ApiResponse.success(creditPackageService.disableCreditPackage(creditPackageId));
    }

    private CreditPackageCommand createCommand(UUID creditPackageId, CreateCreditPackageRequest request) {
        return new CreditPackageCommand(
                creditPackageId,
                request.name(),
                request.code(),
                request.credits(),
                request.bonusCredits(),
                request.price(),
                request.currency(),
                request.active(),
                request.sortOrder()
        );
    }

    private CreditPackageCommand updateCommand(UUID creditPackageId, UpdateCreditPackageRequest request) {
        return new CreditPackageCommand(
                creditPackageId,
                request.name(),
                request.code(),
                request.credits(),
                request.bonusCredits(),
                request.price(),
                request.currency(),
                request.active(),
                request.sortOrder()
        );
    }
}
