package com.lebhas.creativesaas.creative.interfaces;

import com.lebhas.ai.application.MasterAiProviderManagementService;
import com.lebhas.ai.application.MasterAiProviderToolRegistryService;
import com.lebhas.ai.application.MasterProviderSettingsService;
import com.lebhas.ai.application.dto.CreateMasterProviderRequest;
import com.lebhas.ai.application.dto.AiProviderCommand;
import com.lebhas.ai.application.dto.AiProviderCredentialCommand;
import com.lebhas.ai.application.dto.AiProviderCredentialView;
import com.lebhas.ai.application.dto.AiProviderView;
import com.lebhas.ai.application.dto.CreativeToolCommand;
import com.lebhas.ai.application.dto.CreativeToolView;
import com.lebhas.ai.application.dto.MasterProviderView;
import com.lebhas.ai.application.dto.MasterMonitoringResponse;
import com.lebhas.ai.application.dto.ProviderHealthSnapshotView;
import com.lebhas.ai.application.dto.ProviderHealthSummary;
import com.lebhas.ai.application.dto.ProviderRoutingPolicyCommand;
import com.lebhas.ai.application.dto.ProviderRoutingPolicyView;
import com.lebhas.ai.application.dto.ProviderConnectionTestResult;
import com.lebhas.ai.application.dto.ProviderCredentialSavedView;
import com.lebhas.ai.application.dto.ProviderModelsJsonView;
import com.lebhas.ai.application.dto.SaveProviderCredentialRequest;
import com.lebhas.ai.application.dto.TestProviderConnectionRequest;
import com.lebhas.ai.application.dto.UpdateMasterProviderRequest;
import com.lebhas.ai.application.dto.UpdateProviderStatusRequest;
import com.lebhas.ai.credit.application.MasterCreditMonitoringService;
import com.lebhas.ai.credit.application.ProviderCreditExchangePolicyService;
import com.lebhas.ai.credit.application.ProviderCreditPoolService;
import com.lebhas.ai.credit.application.dto.MasterCreditOverviewView;
import com.lebhas.ai.credit.application.dto.MasterWorkspaceCreditView;
import com.lebhas.ai.credit.application.dto.ProviderCreditExchangePolicyCommand;
import com.lebhas.ai.credit.application.dto.ProviderCreditExchangePolicyView;
import com.lebhas.ai.credit.application.dto.ProviderCreditLedgerView;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolAdjustmentCommand;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolCommand;
import com.lebhas.ai.credit.application.dto.ProviderCreditPoolView;
import com.lebhas.ai.domain.ProviderEnvironment;
import com.lebhas.ai.domain.ProviderStatus;
import com.lebhas.ai.domain.ProviderType;
import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.common.api.PagedResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master")
@Tag(name = "Master AI Provider Registry")
@SecurityRequirement(name = "bearerAuth")
public class MasterAiProviderRegistryController {

    private final MasterAiProviderManagementService providerService;
    private final MasterAiProviderToolRegistryService registryService;
    private final MasterProviderSettingsService providerSettingsService;
    private final ProviderCreditPoolService providerCreditPoolService;
    private final ProviderCreditExchangePolicyService providerCreditExchangePolicyService;
    private final MasterCreditMonitoringService masterCreditMonitoringService;

    public MasterAiProviderRegistryController(
            MasterAiProviderManagementService providerService,
            MasterAiProviderToolRegistryService registryService,
            MasterProviderSettingsService providerSettingsService,
            ProviderCreditPoolService providerCreditPoolService,
            ProviderCreditExchangePolicyService providerCreditExchangePolicyService,
            MasterCreditMonitoringService masterCreditMonitoringService
    ) {
        this.providerService = providerService;
        this.registryService = registryService;
        this.providerSettingsService = providerSettingsService;
        this.providerCreditPoolService = providerCreditPoolService;
        this.providerCreditExchangePolicyService = providerCreditExchangePolicyService;
        this.masterCreditMonitoringService = masterCreditMonitoringService;
    }

    @GetMapping("/providers")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List configurable providers")
    public ApiResponse<List<MasterProviderView>> listConfigurableProviders(
            @RequestParam(required = false) ProviderType type,
            @RequestParam(required = false) ProviderStatus status,
            @RequestParam(required = false) ProviderEnvironment environment
    ) {
        return ApiResponse.success(
                "Providers loaded",
                providerSettingsService.listProviders(type, status, environment));
    }

    @PostMapping("/providers")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create configurable provider")
    public ApiResponse<MasterProviderView> createConfigurableProvider(
            @Valid @RequestBody CreateMasterProviderRequest request
    ) {
        return ApiResponse.success("Provider created", providerSettingsService.createProvider(request));
    }

    @GetMapping("/providers/{providerId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get configurable provider detail")
    public ApiResponse<MasterProviderView> getConfigurableProvider(@PathVariable String providerId) {
        return ApiResponse.success("Provider loaded", providerSettingsService.getProvider(providerId));
    }

    @PutMapping("/providers/{providerId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update configurable provider metadata")
    public ApiResponse<MasterProviderView> updateConfigurableProvider(
            @PathVariable UUID providerId,
            @Valid @RequestBody UpdateMasterProviderRequest request
    ) {
        return ApiResponse.success("Provider updated", providerSettingsService.updateProvider(providerId, request));
    }

    @PutMapping("/providers/{providerId}/credentials")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Save configurable provider credential")
    public ApiResponse<ProviderCredentialSavedView> saveConfigurableProviderCredential(
            @PathVariable String providerId,
            @Valid @RequestBody SaveProviderCredentialRequest request
    ) {
        return ApiResponse.success(
                "Provider credential saved",
                providerSettingsService.saveCredential(providerId, request));
    }

    @PostMapping("/providers/{providerId}/test-connection")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Test configurable provider connection")
    public ApiResponse<ProviderConnectionTestResult> testConfigurableProviderConnection(
            @PathVariable String providerId,
            @Valid @RequestBody TestProviderConnectionRequest request
    ) {
        return ApiResponse.success(
                "Provider connection test completed",
                providerSettingsService.testConnection(providerId, request));
    }

    @PostMapping("/providers/{providerId}/models-json")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Fetch OpenAI provider models JSON")
    public ApiResponse<ProviderModelsJsonView> fetchConfigurableProviderModelsJson(
            @PathVariable String providerId,
            @Valid @RequestBody TestProviderConnectionRequest request
    ) {
        return ApiResponse.success(
                "Provider models JSON loaded",
                providerSettingsService.fetchModelsJson(providerId, request));
    }

    @DeleteMapping("/providers/{providerId}/credentials")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Revoke configurable provider credential")
    public ApiResponse<ProviderCredentialSavedView> revokeConfigurableProviderCredential(
            @PathVariable UUID providerId,
            @RequestParam(defaultValue = "SANDBOX") ProviderEnvironment environment
    ) {
        return ApiResponse.success(
                "Provider credential revoked",
                providerSettingsService.revokeCredential(providerId, environment));
    }

    @PatchMapping("/providers/{providerId}/status")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update configurable provider status")
    public ApiResponse<MasterProviderView> updateConfigurableProviderStatus(
            @PathVariable String providerId,
            @Valid @RequestBody UpdateProviderStatusRequest request
    ) {
        return ApiResponse.success("Provider status updated", providerSettingsService.updateProviderStatus(providerId, request));
    }

    @DeleteMapping("/providers/{providerId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Delete configurable provider")
    public ApiResponse<Void> deleteConfigurableProvider(@PathVariable String providerId) {
        providerSettingsService.deleteProvider(providerId);
        return ApiResponse.success("Provider deleted", null);
    }

    @PostMapping("/ai-providers")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create AI provider")
    public ApiResponse<AiProviderView> createProvider(@Valid @RequestBody AiProviderCommand request) {
        return ApiResponse.success(providerService.createProvider(request));
    }

    @GetMapping("/ai-providers")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List AI providers")
    public ApiResponse<List<AiProviderView>> listProviders() {
        return ApiResponse.success(providerService.listProviders());
    }

    @PutMapping("/ai-providers/{providerId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update AI provider")
    public ApiResponse<AiProviderView> updateProvider(
            @PathVariable UUID providerId,
            @Valid @RequestBody AiProviderCommand request
    ) {
        return ApiResponse.success(providerService.updateProvider(providerId, request));
    }

    @PostMapping("/ai-providers/{providerId}/credentials")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create AI provider credential")
    public ApiResponse<AiProviderCredentialView> createCredential(
            @PathVariable UUID providerId,
            @Valid @RequestBody AiProviderCredentialCommand request
    ) {
        return ApiResponse.success(registryService.createCredential(providerId, request));
    }

    @PutMapping("/ai-providers/{providerId}/credentials/{credentialId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update AI provider credential")
    public ApiResponse<AiProviderCredentialView> updateCredential(
            @PathVariable UUID providerId,
            @PathVariable UUID credentialId,
            @Valid @RequestBody AiProviderCredentialCommand request
    ) {
        return ApiResponse.success(registryService.updateCredential(providerId, credentialId, request));
    }

    @DeleteMapping("/ai-providers/{providerId}/credentials/{credentialId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Deactivate AI provider credential")
    public ApiResponse<AiProviderCredentialView> deleteCredential(
            @PathVariable UUID providerId,
            @PathVariable UUID credentialId
    ) {
        return ApiResponse.success("AI provider credential deactivated", registryService.revokeCredential(providerId, credentialId));
    }

    @GetMapping("/ai-providers/{providerId}/credentials")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List AI provider credentials")
    public ApiResponse<List<AiProviderCredentialView>> listCredentials(@PathVariable UUID providerId) {
        return ApiResponse.success("AI provider credentials loaded", registryService.listCredentials(providerId));
    }

    @PostMapping("/creative-tools")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create creative tool")
    public ApiResponse<CreativeToolView> createTool(@Valid @RequestBody CreativeToolCommand request) {
        return ApiResponse.success(registryService.createTool(request));
    }

    @GetMapping("/creative-tools")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List creative tools")
    public ApiResponse<List<CreativeToolView>> listTools() {
        return ApiResponse.success("Creative tools loaded", registryService.listTools());
    }

    @GetMapping("/creative-tools/{toolId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get creative tool")
    public ApiResponse<CreativeToolView> getTool(@PathVariable UUID toolId) {
        return ApiResponse.success("Creative tool loaded", registryService.getTool(toolId));
    }

    @PutMapping("/creative-tools/{toolId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update creative tool")
    public ApiResponse<CreativeToolView> updateTool(
            @PathVariable UUID toolId,
            @Valid @RequestBody CreativeToolCommand request
    ) {
        return ApiResponse.success(registryService.updateTool(toolId, request));
    }

    @PatchMapping("/creative-tools/{toolId}/status")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update creative tool status")
    public ApiResponse<Map<String, Object>> updateToolStatus(@PathVariable UUID toolId) {
        return ApiResponse.success("Creative tool status endpoint is available",
                Map.of("toolId", toolId, "status", "foundation_pending"));
    }

    @PostMapping("/provider-routing-policies")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create provider routing policy")
    public ApiResponse<ProviderRoutingPolicyView> createRoutingPolicy(@Valid @RequestBody ProviderRoutingPolicyCommand request) {
        return ApiResponse.success(registryService.createRoutingPolicy(request));
    }

    @GetMapping("/provider-routing-policies")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List provider routing policies")
    public ApiResponse<List<ProviderRoutingPolicyView>> listRoutingPolicies() {
        return ApiResponse.success("Provider routing policies loaded", registryService.listRoutingPolicies());
    }

    @GetMapping("/provider-routing-policies/{policyId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get provider routing policy")
    public ApiResponse<ProviderRoutingPolicyView> getRoutingPolicy(@PathVariable UUID policyId) {
        return ApiResponse.success("Provider routing policy loaded", registryService.getRoutingPolicy(policyId));
    }

    @PutMapping("/provider-routing-policies/{policyId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update provider routing policy")
    public ApiResponse<ProviderRoutingPolicyView> updateRoutingPolicy(
            @PathVariable UUID policyId,
            @Valid @RequestBody ProviderRoutingPolicyCommand request
    ) {
        return ApiResponse.success(registryService.updateRoutingPolicy(policyId, request));
    }

    @PatchMapping("/provider-routing-policies/{policyId}/status")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update provider routing policy status")
    public ApiResponse<Map<String, Object>> updateRoutingPolicyStatus(@PathVariable UUID policyId) {
        return ApiResponse.success("Provider routing policy status endpoint is available",
                Map.of("policyId", policyId, "status", "foundation_pending"));
    }

    @GetMapping("/ai-providers/health")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List AI provider health snapshots")
    public ApiResponse<List<ProviderHealthSnapshotView>> listProviderHealth() {
        return ApiResponse.success("Provider health loaded", registryService.listHealth());
    }

    @GetMapping("/ai/provider-health")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List AI provider health snapshots")
    public ApiResponse<MasterMonitoringResponse<ProviderHealthSummary, ProviderHealthSnapshotView>> listProviderHealthForAiOperations() {
        List<ProviderHealthSnapshotView> items = registryService.listHealth();
        ProviderHealthSummary summary = new ProviderHealthSummary(
                items.size(),
                items.stream().filter(item -> "HEALTHY".equalsIgnoreCase(item.status())).count(),
                items.stream().filter(item -> "DEGRADED".equalsIgnoreCase(item.status()) || "COOLDOWN".equalsIgnoreCase(item.status())).count(),
                items.stream().filter(item -> "DOWN".equalsIgnoreCase(item.status()) || "FAILED".equalsIgnoreCase(item.status())).count());
        return ApiResponse.success(items.isEmpty() ? "No records found" : "Provider health loaded",
                MasterMonitoringResponse.of(summary, items));
    }

    @PostMapping("/providers/{providerId}/credit-pool")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create or replace provider credit pool")
    public ApiResponse<ProviderCreditPoolView> createProviderCreditPool(
            @PathVariable UUID providerId,
            @Valid @RequestBody ProviderCreditPoolCommand request
    ) {
        return ApiResponse.success("Provider credit pool saved", providerCreditPoolService.createOrReplacePool(providerId, request));
    }

    @GetMapping("/providers/{providerId}/credit-pool")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get provider credit pool")
    public ApiResponse<ProviderCreditPoolView> getProviderCreditPool(@PathVariable UUID providerId) {
        return ApiResponse.success("Provider credit pool loaded", providerCreditPoolService.getPool(providerId));
    }

    @PutMapping("/providers/{providerId}/credit-pool")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update provider credit pool")
    public ApiResponse<ProviderCreditPoolView> updateProviderCreditPool(
            @PathVariable UUID providerId,
            @Valid @RequestBody ProviderCreditPoolCommand request
    ) {
        return ApiResponse.success("Provider credit pool updated", providerCreditPoolService.createOrReplacePool(providerId, request));
    }

    @PostMapping("/providers/{providerId}/credit-pool/adjust")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Adjust provider credit pool")
    public ApiResponse<ProviderCreditPoolView> adjustProviderCreditPool(
            @PathVariable UUID providerId,
            @Valid @RequestBody ProviderCreditPoolAdjustmentCommand request
    ) {
        return ApiResponse.success("Provider credit pool adjusted", providerCreditPoolService.adjustPool(providerId, request));
    }

    @GetMapping("/providers/{providerId}/credit-ledger")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List provider credit ledger")
    public ApiResponse<PagedResult<ProviderCreditLedgerView>> listProviderCreditLedger(
            @PathVariable UUID providerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ApiResponse.success("Provider credit ledger loaded",
                providerCreditPoolService.listLedger(providerId, PageRequest.of(page, Math.min(size, 100))));
    }

    @PostMapping("/providers/{providerId}/exchange-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create or replace provider exchange policy")
    public ApiResponse<ProviderCreditExchangePolicyView> createProviderExchangePolicy(
            @PathVariable UUID providerId,
            @Valid @RequestBody ProviderCreditExchangePolicyCommand request
    ) {
        return ApiResponse.success("Provider exchange policy saved",
                providerCreditExchangePolicyService.createOrReplacePolicy(providerId, request));
    }

    @GetMapping("/providers/{providerId}/exchange-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get provider exchange policy")
    public ApiResponse<ProviderCreditExchangePolicyView> getProviderExchangePolicy(@PathVariable UUID providerId) {
        return ApiResponse.success("Provider exchange policy loaded", providerCreditExchangePolicyService.getPolicy(providerId));
    }

    @PutMapping("/providers/{providerId}/exchange-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update provider exchange policy")
    public ApiResponse<ProviderCreditExchangePolicyView> updateProviderExchangePolicy(
            @PathVariable UUID providerId,
            @Valid @RequestBody ProviderCreditExchangePolicyCommand request
    ) {
        return ApiResponse.success("Provider exchange policy updated",
                providerCreditExchangePolicyService.createOrReplacePolicy(providerId, request));
    }

    @GetMapping("/credits/overview")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get Master credit overview")
    public ApiResponse<MasterCreditOverviewView> getMasterCreditOverview() {
        return ApiResponse.success("Master credit overview loaded", masterCreditMonitoringService.overview());
    }

    @GetMapping("/workspaces/{workspaceId}/credits")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get workspace credit account and ledger")
    public ApiResponse<MasterWorkspaceCreditView> getWorkspaceCredits(@PathVariable UUID workspaceId) {
        return ApiResponse.success("Workspace credits loaded", masterCreditMonitoringService.workspaceCredits(workspaceId));
    }
}
