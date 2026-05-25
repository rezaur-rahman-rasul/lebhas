package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.payment.application.PaymentProviderConfigurationService;
import com.lebhas.creativesaas.payment.application.PaymentProviderManagementService;
import com.lebhas.creativesaas.payment.application.dto.PaymentProviderCommand;
import com.lebhas.creativesaas.payment.application.dto.PaymentProviderConfigurationCommand;
import com.lebhas.creativesaas.payment.application.dto.PaymentProviderConfigurationView;
import com.lebhas.creativesaas.payment.application.dto.PaymentProviderView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/v1/master")
@Tag(name = "Master Payment Providers")
@SecurityRequirement(name = "bearerAuth")
public class MasterPaymentProviderController {

    private final PaymentProviderManagementService providerManagementService;
    private final PaymentProviderConfigurationService configurationService;

    public MasterPaymentProviderController(
            PaymentProviderManagementService providerManagementService,
            PaymentProviderConfigurationService configurationService
    ) {
        this.providerManagementService = providerManagementService;
        this.configurationService = configurationService;
    }

    @PostMapping("/payment-providers")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create a payment provider")
    public ApiResponse<PaymentProviderView> createProvider(@Valid @RequestBody CreatePaymentProviderRequest request) {
        return ApiResponse.success(providerManagementService.createProvider(new PaymentProviderCommand(
                null,
                request.name(),
                request.code(),
                request.providerType(),
                request.enabled(),
                request.sandboxEnabled(),
                request.liveEnabled(),
                request.priority()
        )));
    }

    @GetMapping("/payment-providers")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List payment providers")
    public ApiResponse<List<PaymentProviderView>> listProviders() {
        return ApiResponse.success(providerManagementService.listProviders());
    }

    @GetMapping("/payment-providers/{providerId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get payment provider details")
    public ApiResponse<PaymentProviderView> getProvider(@PathVariable UUID providerId) {
        return ApiResponse.success(providerManagementService.getProvider(providerId));
    }

    @PutMapping("/payment-providers/{providerId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update a payment provider")
    public ApiResponse<PaymentProviderView> updateProvider(
            @PathVariable UUID providerId,
            @Valid @RequestBody UpdatePaymentProviderRequest request
    ) {
        return ApiResponse.success(providerManagementService.updateProvider(new PaymentProviderCommand(
                providerId,
                request.name(),
                request.code(),
                request.providerType(),
                request.enabled(),
                request.sandboxEnabled(),
                request.liveEnabled(),
                request.priority()
        )));
    }

    @PostMapping("/payment-providers/{providerId}/enable")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Enable a payment provider")
    public ApiResponse<PaymentProviderView> enableProvider(@PathVariable UUID providerId) {
        return ApiResponse.success(providerManagementService.enableProvider(providerId));
    }

    @PostMapping("/payment-providers/{providerId}/disable")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Disable a payment provider")
    public ApiResponse<PaymentProviderView> disableProvider(@PathVariable UUID providerId) {
        return ApiResponse.success(providerManagementService.disableProvider(providerId));
    }

    @PutMapping("/payment-providers/{providerId}/priority")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update payment provider priority")
    public ApiResponse<PaymentProviderView> updatePriority(
            @PathVariable UUID providerId,
            @Valid @RequestBody UpdatePaymentProviderPriorityRequest request
    ) {
        return ApiResponse.success(providerManagementService.prioritizeProvider(providerId, request.priority()));
    }

    @PostMapping("/payment-providers/{providerId}/configurations")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create payment provider configuration")
    public ApiResponse<PaymentProviderConfigurationView> createConfiguration(
            @PathVariable UUID providerId,
            @Valid @RequestBody UpsertPaymentProviderConfigurationRequest request
    ) {
        return ApiResponse.success(configurationService.createConfiguration(configurationCommand(null, providerId, request)));
    }

    @PutMapping("/payment-provider-configurations/{configurationId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update payment provider configuration")
    public ApiResponse<PaymentProviderConfigurationView> updateConfiguration(
            @PathVariable UUID configurationId,
            @Valid @RequestBody UpsertPaymentProviderConfigurationRequest request
    ) {
        return ApiResponse.success(configurationService.updateConfiguration(configurationCommand(configurationId, null, request)));
    }

    @PostMapping("/payment-provider-configurations/{configurationId}/activate")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Activate payment provider configuration")
    public ApiResponse<PaymentProviderConfigurationView> activateConfiguration(@PathVariable UUID configurationId) {
        return ApiResponse.success(configurationService.activateConfiguration(configurationId));
    }

    @PostMapping("/payment-provider-configurations/{configurationId}/deactivate")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Deactivate payment provider configuration")
    public ApiResponse<PaymentProviderConfigurationView> deactivateConfiguration(@PathVariable UUID configurationId) {
        return ApiResponse.success(configurationService.deactivateConfiguration(configurationId));
    }

    private PaymentProviderConfigurationCommand configurationCommand(
            UUID configurationId,
            UUID providerId,
            UpsertPaymentProviderConfigurationRequest request
    ) {
        return new PaymentProviderConfigurationCommand(
                configurationId,
                providerId,
                request.environmentType(),
                request.apiBaseUrl(),
                request.merchantId(),
                request.apiKey(),
                request.secret(),
                request.webhookSecret(),
                request.successUrl(),
                request.failureUrl(),
                request.cancelUrl(),
                request.active()
        );
    }
}
