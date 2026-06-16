package com.lebhas.creativesaas.user.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.profile.application.ProfileImageService;
import com.lebhas.creativesaas.profile.application.ProfilePasswordService;
import com.lebhas.creativesaas.profile.application.ProfileSessionService;
import com.lebhas.creativesaas.profile.application.UserAccountSettingsService;
import com.lebhas.creativesaas.profile.application.UserProfileService;
import com.lebhas.creativesaas.profile.application.UserSecurityActivityService;
import com.lebhas.creativesaas.profile.application.dto.ChangePasswordRequest;
import com.lebhas.creativesaas.profile.application.dto.ConfirmProfileImageUploadRequest;
import com.lebhas.creativesaas.profile.application.dto.ProfileImageUploadUrlRequest;
import com.lebhas.creativesaas.profile.application.dto.ProfileImageUploadUrlResponse;
import com.lebhas.creativesaas.profile.application.dto.SecurityActivityView;
import com.lebhas.creativesaas.profile.application.dto.UpdateAccountSettingsRequest;
import com.lebhas.creativesaas.profile.application.dto.UpdateProfileRequest;
import com.lebhas.creativesaas.profile.application.dto.UserProfileView;
import com.lebhas.creativesaas.profile.application.dto.UserSessionView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/profile/me")
@Tag(name = "Profile")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final UserProfileService userProfileService;
    private final UserAccountSettingsService userAccountSettingsService;
    private final ProfilePasswordService profilePasswordService;
    private final ProfileImageService profileImageService;
    private final UserSecurityActivityService userSecurityActivityService;
    private final ProfileSessionService profileSessionService;

    public ProfileController(
            UserProfileService userProfileService,
            UserAccountSettingsService userAccountSettingsService,
            ProfilePasswordService profilePasswordService,
            ProfileImageService profileImageService,
            UserSecurityActivityService userSecurityActivityService,
            ProfileSessionService profileSessionService
    ) {
        this.userProfileService = userProfileService;
        this.userAccountSettingsService = userAccountSettingsService;
        this.profilePasswordService = profilePasswordService;
        this.profileImageService = profileImageService;
        this.userSecurityActivityService = userSecurityActivityService;
        this.profileSessionService = profileSessionService;
    }

    @GetMapping
    @Operation(summary = "View the current user's profile")
    public ApiResponse<UserProfileView> viewOwnProfile() {
        return ApiResponse.success(userProfileService.viewOwnProfile());
    }

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update the current user's profile")
    public ApiResponse<UserProfileView> updateOwnProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(userProfileService.updateOwnProfile(
                request,
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")));
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update the current user's profile with an optional profile image")
    public ApiResponse<UserProfileView> updateOwnProfileMultipart(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String displayName,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String jobTitle,
            @RequestParam(required = false) String bio,
            @RequestParam String timezone,
            @RequestParam String locale,
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            HttpServletRequest httpServletRequest
    ) {
        UpdateProfileRequest request = new UpdateProfileRequest(
                firstName,
                lastName,
                displayName,
                phoneNumber,
                jobTitle,
                bio,
                timezone,
                locale);
        return ApiResponse.success(userProfileService.updateOwnProfile(
                request,
                profileImage,
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")));
    }

    @GetMapping("/account-settings")
    @Operation(summary = "View the current user's account settings")
    public ApiResponse<UserProfileView.AccountSettingsView> viewOwnAccountSettings() {
        return ApiResponse.success(userAccountSettingsService.viewOwnAccountSettings());
    }

    @PutMapping("/account-settings")
    @Operation(summary = "Update the current user's account settings")
    public ApiResponse<UserProfileView.AccountSettingsView> updateOwnAccountSettings(
            @Valid @RequestBody UpdateAccountSettingsRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(userAccountSettingsService.updateOwnAccountSettings(
                request,
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change the current user's password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpServletRequest
    ) {
        profilePasswordService.changeOwnPassword(
                request,
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"));
        return ApiResponse.success("Password changed", null);
    }

    @PostMapping("/profile-image/upload-url")
    @Operation(summary = "Create a signed profile image upload URL")
    public ApiResponse<ProfileImageUploadUrlResponse> createProfileImageUploadUrl(
            @Valid @RequestBody ProfileImageUploadUrlRequest request
    ) {
        return ApiResponse.success(profileImageService.requestSignedUploadUrl(request));
    }

    @PostMapping(value = "/profile-image", consumes = "multipart/form-data")
    @Operation(summary = "Upload the current user's profile image")
    public ApiResponse<UserProfileView> uploadProfileImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(profileImageService.uploadDirect(
                file,
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")));
    }

    @PostMapping("/profile-image/confirm")
    @Operation(summary = "Confirm a profile image upload")
    public ApiResponse<UserProfileView> confirmProfileImageUpload(
            @Valid @RequestBody ConfirmProfileImageUploadRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(profileImageService.confirmUpload(
                request,
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")));
    }

    @DeleteMapping("/profile-image")
    @Operation(summary = "Remove the current user's profile image")
    public ApiResponse<UserProfileView> removeProfileImage(HttpServletRequest httpServletRequest) {
        return ApiResponse.success(profileImageService.removeProfileImage(
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")));
    }

    @GetMapping("/security-activity")
    @Operation(summary = "List recent security activity for the current user")
    public ApiResponse<List<SecurityActivityView>> listOwnSecurityActivity(@RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(userSecurityActivityService.listOwnRecentSecurityActivities(limit));
    }

    @GetMapping("/sessions")
    @Operation(summary = "List active and recent sessions for the current user")
    public ApiResponse<List<UserSessionView>> listOwnSessions() {
        return ApiResponse.success(profileSessionService.listOwnSessions());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "Revoke one of the current user's sessions")
    public ApiResponse<UserSessionView> deleteOwnSession(@PathVariable String sessionId) {
        return ApiResponse.success("Session revoked", profileSessionService.revokeOwnSession(sessionId));
    }

    @DeleteMapping("/sessions/others")
    @Operation(summary = "Revoke all other sessions for the current user")
    public ApiResponse<List<UserSessionView>> deleteOtherSessions() {
        return ApiResponse.success("Other sessions revoked", profileSessionService.revokeOtherOwnSessions());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
