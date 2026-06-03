package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.pricing.application.PlanFeaturePolicyService;
import com.lebhas.creativesaas.pricing.application.PricingPlanQueryService;
import com.lebhas.creativesaas.pricing.application.PricingPlanService;
import com.lebhas.creativesaas.pricing.application.dto.CreatePricingPlanCommand;
import com.lebhas.creativesaas.pricing.application.dto.UpdatePlanFeaturePolicyCommand;
import com.lebhas.creativesaas.pricing.application.dto.UpdatePricingPlanCommand;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/pricing-plans")
@Tag(name = "Master Pricing")
@SecurityRequirement(name = "bearerAuth")
public class MasterPricingPlanController {

    private final PricingPlanService pricingPlanService;
    private final PricingPlanQueryService pricingPlanQueryService;
    private final PlanFeaturePolicyService planFeaturePolicyService;
    private final PricingApiMapper pricingApiMapper;

    public MasterPricingPlanController(
            PricingPlanService pricingPlanService,
            PricingPlanQueryService pricingPlanQueryService,
            PlanFeaturePolicyService planFeaturePolicyService,
            PricingApiMapper pricingApiMapper
    ) {
        this.pricingPlanService = pricingPlanService;
        this.pricingPlanQueryService = pricingPlanQueryService;
        this.planFeaturePolicyService = planFeaturePolicyService;
        this.pricingApiMapper = pricingApiMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create a pricing plan")
    public ApiResponse<PricingPlanDetailResponse> createPricingPlan(@Valid @RequestBody CreatePricingPlanRequest request) {
        UUID pricingPlanId = pricingPlanService.createPricingPlan(new CreatePricingPlanCommand(
                request.name(),
                request.code(),
                request.description(),
                request.monthlyPrice(),
                request.yearlyPrice(),
                request.currency(),
                request.defaultPlan(),
                request.active(),
                request.sortOrder())).id();
        return ApiResponse.success(pricingApiMapper.toPricingPlanDetailResponse(
                pricingPlanQueryService.getPricingPlanForMaster(pricingPlanId)));
    }

    @GetMapping
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "List all pricing plans")
    public ApiResponse<List<PricingPlanDetailResponse>> listPricingPlans() {
        return ApiResponse.success(pricingApiMapper.toPricingPlanDetailResponses(
                pricingPlanQueryService.listAllPricingPlansForMaster()));
    }

    @GetMapping("/{pricingPlanId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get pricing plan details")
    public ApiResponse<PricingPlanDetailResponse> getPricingPlan(@PathVariable UUID pricingPlanId) {
        return ApiResponse.success(pricingApiMapper.toPricingPlanDetailResponse(
                pricingPlanQueryService.getPricingPlanForMaster(pricingPlanId)));
    }

    @PutMapping("/{pricingPlanId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Update a pricing plan")
    public ApiResponse<PricingPlanDetailResponse> updatePricingPlan(
            @PathVariable UUID pricingPlanId,
            @Valid @RequestBody UpdatePricingPlanRequest request
    ) {
        pricingPlanService.updatePricingPlan(new UpdatePricingPlanCommand(
                pricingPlanId,
                request.name(),
                request.code(),
                request.description(),
                request.monthlyPrice(),
                request.yearlyPrice(),
                request.currency(),
                request.defaultPlan(),
                request.active(),
                request.sortOrder()));
        return ApiResponse.success(pricingApiMapper.toPricingPlanDetailResponse(
                pricingPlanQueryService.getPricingPlanForMaster(pricingPlanId)));
    }

    @DeleteMapping("/{pricingPlanId}")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Disable a pricing plan")
    public ApiResponse<PricingPlanDetailResponse> disablePricingPlan(@PathVariable UUID pricingPlanId) {
        pricingPlanService.disablePricingPlan(pricingPlanId);
        return ApiResponse.success(pricingApiMapper.toPricingPlanDetailResponse(
                pricingPlanQueryService.getPricingPlanForMaster(pricingPlanId)));
    }

    @PatchMapping("/{pricingPlanId}/activate")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Activate a pricing plan")
    public ApiResponse<PricingPlanDetailResponse> activatePricingPlan(@PathVariable UUID pricingPlanId) {
        pricingPlanService.activatePricingPlan(pricingPlanId);
        return ApiResponse.success(pricingApiMapper.toPricingPlanDetailResponse(
                pricingPlanQueryService.getPricingPlanForMaster(pricingPlanId)));
    }

    @PatchMapping("/{pricingPlanId}/deactivate")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Deactivate a pricing plan")
    public ApiResponse<PricingPlanDetailResponse> deactivatePricingPlan(@PathVariable UUID pricingPlanId) {
        pricingPlanService.disablePricingPlan(pricingPlanId);
        return ApiResponse.success(pricingApiMapper.toPricingPlanDetailResponse(
                pricingPlanQueryService.getPricingPlanForMaster(pricingPlanId)));
    }

    @PostMapping("/{pricingPlanId}/feature-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create pricing plan feature policy")
    public ApiResponse<PlanFeaturePolicyResponse> createFeaturePolicy(
            @PathVariable UUID pricingPlanId,
            @Valid @RequestBody UpdatePlanFeaturePolicyRequest request
    ) {
        return upsertFeaturePolicy(pricingPlanId, request);
    }

    @GetMapping("/{pricingPlanId}/feature-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Get pricing plan feature policy")
    public ApiResponse<PlanFeaturePolicyResponse> getFeaturePolicy(@PathVariable UUID pricingPlanId) {
        return ApiResponse.success(pricingApiMapper.toPlanFeaturePolicyResponse(
                planFeaturePolicyService.getFeaturePolicyForMaster(pricingPlanId).orElse(null)));
    }

    @PutMapping("/{pricingPlanId}/feature-policy")
    @PreAuthorize("hasRole('MASTER')")
    @Operation(summary = "Create or update pricing plan feature policy")
    public ApiResponse<PlanFeaturePolicyResponse> updateFeaturePolicy(
            @PathVariable UUID pricingPlanId,
            @Valid @RequestBody UpdatePlanFeaturePolicyRequest request
    ) {
        return upsertFeaturePolicy(pricingPlanId, request);
    }

    private ApiResponse<PlanFeaturePolicyResponse> upsertFeaturePolicy(
            UUID pricingPlanId,
            UpdatePlanFeaturePolicyRequest request
    ) {
        return ApiResponse.success(pricingApiMapper.toPlanFeaturePolicyResponse(
                planFeaturePolicyService.updateFeaturePolicy(new UpdatePlanFeaturePolicyCommand(
                        pricingPlanId,
                        request.maxGeneratedVersionsPerRequest(),
                        request.maxBrands(),
                        request.maxProductServices(),
                        request.maxProjects(),
                        request.maxAssets(),
                        request.maxCreativeRequests(),
                        request.maxTeamMembers(),
                        request.maxGeneratedVersionsPerCreativeRequest(),
                        request.maxStorageGb(),
                        request.maxStorageBytes(),
                        request.monthlyCreditLimit(),
                        request.promptEnhancementEnabled(),
                        request.creativeGenerationEnabled(),
                        request.allowApprovalWorkflow(),
                        request.downloadEnabled(),
                        request.shareEnabled(),
                        request.allowPublicShareLinks(),
                        request.assetUploadEnabled(),
                        request.premiumQualityEnabled(),
                        request.allowVideoGeneration(),
                        request.voiceoverGenerationEnabled(),
                        request.allowAdvancedPromptIntelligence(),
                        request.allowTeamCollaboration(),
                        request.allowExportWithoutWatermark(),
                        request.enabledCreativeToolCodes()))));
    }
}
