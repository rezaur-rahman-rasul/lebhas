package com.lebhas.creativesaas.user.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.profile.application.ProfileRewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/profile")
@Tag(name = "Profile Rewards")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class ProfileRewardController {

    private final ProfileRewardService profileRewardService;

    public ProfileRewardController(ProfileRewardService profileRewardService) {
        this.profileRewardService = profileRewardService;
    }

    @PostMapping("/email")
    @Operation(summary = "Add or update current user's email and claim reward once")
    public ApiResponse<ProfileRewardService.ProfileRewardResult> updateEmail(@Valid @RequestBody ProfileEmailRequest request) {
        return ApiResponse.success(profileRewardService.updateEmail(request.email()));
    }

    @PostMapping("/social-connections/facebook")
    @Operation(summary = "Connect Facebook profile/page and claim reward once")
    public ApiResponse<ProfileRewardService.ProfileRewardResult> connectFacebook(@Valid @RequestBody SocialConnectionRequest request) {
        return ApiResponse.success(profileRewardService.connectSocial("FACEBOOK", request.profileUrl()));
    }

    @PostMapping("/social-connections/instagram")
    @Operation(summary = "Connect Instagram profile/page and claim reward once")
    public ApiResponse<ProfileRewardService.ProfileRewardResult> connectInstagram(@Valid @RequestBody SocialConnectionRequest request) {
        return ApiResponse.success(profileRewardService.connectSocial("INSTAGRAM", request.profileUrl()));
    }

    @GetMapping("/rewards")
    @Operation(summary = "Get current user's profile reward claim status")
    public ApiResponse<Map<String, Boolean>> rewardStatus() {
        return ApiResponse.success(profileRewardService.rewardStatus());
    }
}
