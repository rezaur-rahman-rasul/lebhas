package com.lebhas.creativesaas.billing.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.pricing.application.PricingPlanQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pricing-plans")
@Tag(name = "Pricing Plans")
public class PublicPricingPlanController {

    private final PricingPlanQueryService pricingPlanQueryService;
    private final PricingApiMapper pricingApiMapper;

    public PublicPricingPlanController(
            PricingPlanQueryService pricingPlanQueryService,
            PricingApiMapper pricingApiMapper
    ) {
        this.pricingPlanQueryService = pricingPlanQueryService;
        this.pricingApiMapper = pricingApiMapper;
    }

    @GetMapping("/public")
    @Operation(summary = "List active pricing plans for public discovery", security = {})
    public ApiResponse<List<PricingPlanDetailResponse>> listPublicPricingPlans() {
        return activePricingPlans();
    }

    @GetMapping
    @Operation(summary = "List active pricing plans", security = {})
    public ApiResponse<List<PricingPlanDetailResponse>> listActivePricingPlans() {
        return activePricingPlans();
    }

    private ApiResponse<List<PricingPlanDetailResponse>> activePricingPlans() {
        return ApiResponse.success(pricingApiMapper.toPricingPlanDetailResponses(
                pricingPlanQueryService.listActivePricingPlans()));
    }
}
