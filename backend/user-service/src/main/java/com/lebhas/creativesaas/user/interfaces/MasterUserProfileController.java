package com.lebhas.creativesaas.user.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.profile.application.MasterUserProfileSupportService;
import com.lebhas.creativesaas.profile.application.UserSecurityActivityService;
import com.lebhas.creativesaas.profile.application.dto.MasterUserProfileView;
import com.lebhas.creativesaas.profile.application.dto.SecurityActivityView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/users")
@Tag(name = "Master User Profile Support")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MASTER')")
public class MasterUserProfileController {

    private final MasterUserProfileSupportService masterUserProfileSupportService;
    private final UserSecurityActivityService userSecurityActivityService;

    public MasterUserProfileController(
            MasterUserProfileSupportService masterUserProfileSupportService,
            UserSecurityActivityService userSecurityActivityService
    ) {
        this.masterUserProfileSupportService = masterUserProfileSupportService;
        this.userSecurityActivityService = userSecurityActivityService;
    }

    @GetMapping("/{userId}/profile")
    @Operation(summary = "View masked profile metadata for a user")
    public ApiResponse<MasterUserProfileView> viewUserProfile(@PathVariable UUID userId) {
        return ApiResponse.success(masterUserProfileSupportService.viewUserProfileMetadata(userId));
    }

    @GetMapping("/{userId}/security-activity")
    @Operation(summary = "View recent security activity for a user")
    public ApiResponse<List<SecurityActivityView>> viewUserSecurityActivity(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.success(userSecurityActivityService.listRecentSecurityActivities(userId, limit));
    }
}
