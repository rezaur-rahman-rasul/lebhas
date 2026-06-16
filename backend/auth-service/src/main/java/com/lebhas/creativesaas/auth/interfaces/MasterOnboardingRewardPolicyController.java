package com.lebhas.creativesaas.auth.interfaces;

import com.lebhas.creativesaas.identity.application.OnboardingRewardPolicyService;
import com.lebhas.creativesaas.identity.application.dto.OnboardingRewardPolicyCommand;
import com.lebhas.creativesaas.identity.application.dto.OnboardingRewardPolicyView;
import com.lebhas.creativesaas.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/master/onboarding-reward-policy")
@Tag(name = "Master Onboarding Reward Policy")
@SecurityRequirement(name = "bearerAuth")
public class MasterOnboardingRewardPolicyController {

    private final OnboardingRewardPolicyService service;

    public MasterOnboardingRewardPolicyController(OnboardingRewardPolicyService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Get active onboarding reward and mobile OTP policy")
    public ApiResponse<OnboardingRewardPolicyView> get() {
        return ApiResponse.success(service.getActivePolicyView());
    }

    @PutMapping
    @Operation(summary = "Update onboarding reward and mobile OTP policy")
    public ApiResponse<OnboardingRewardPolicyView> update(@Valid @RequestBody OnboardingRewardPolicyCommand command) {
        return ApiResponse.success(service.save(command));
    }
}
