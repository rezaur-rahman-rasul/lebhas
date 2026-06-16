package com.lebhas.creativesaas.auth.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.identity.application.AuthenticationService;
import com.lebhas.creativesaas.identity.application.dto.AuthSessionView;
import com.lebhas.creativesaas.identity.application.dto.LoginCommand;
import com.lebhas.creativesaas.identity.application.dto.LogoutCommand;
import com.lebhas.creativesaas.identity.application.dto.MobileOtpStartCommand;
import com.lebhas.creativesaas.identity.application.dto.MobileOtpStartView;
import com.lebhas.creativesaas.identity.application.dto.MobileOtpVerifyCommand;
import com.lebhas.creativesaas.identity.application.dto.MobileOtpVerifyView;
import com.lebhas.creativesaas.identity.application.dto.RefreshSessionCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationBrandCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationEmailStartCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationEmailVerifyCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationPasswordCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationProductServiceCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationProjectCampaignCommand;
import com.lebhas.creativesaas.identity.application.dto.RegistrationStepView;
import com.lebhas.creativesaas.identity.application.dto.RegisterUserCommand;
import com.lebhas.creativesaas.identity.application.dto.UserView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new workspace admin or accept an invitation")
    public ApiResponse<AuthSessionView> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authenticationService.register(
                new RegisterUserCommand(
                        request.firstName(),
                        request.lastName(),
                        request.email(),
                        request.resolvedPhoneNumber(),
                        request.password(),
                        request.confirmPassword(),
                        request.workspaceName(),
                        request.workspaceId(),
                        request.invitationToken()),
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent")));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate with email and password")
    public ApiResponse<AuthSessionView> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authenticationService.login(new LoginCommand(
                request.email(),
                request.password(),
                request.workspaceId(),
                request.deviceId(),
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"))));
    }

    @PostMapping("/mobile/start")
    @Operation(summary = "Start mobile-number OTP login or onboarding")
    public ApiResponse<MobileOtpStartView> startMobileOtp(
            @Valid @RequestBody MobileOtpStartRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authenticationService.startMobileOtp(new MobileOtpStartCommand(
                request.mobileNumber(),
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"))));
    }

    @PostMapping("/mobile/verify")
    @Operation(summary = "Verify mobile OTP and issue a session")
    public ApiResponse<MobileOtpVerifyView> verifyMobileOtp(
            @Valid @RequestBody MobileOtpVerifyRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authenticationService.verifyMobileOtp(new MobileOtpVerifyCommand(
                request.otpToken(),
                request.otp(),
                request.deviceId(),
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"))));
    }

    @PostMapping("/register/mobile/start")
    @Operation(summary = "Start mobile OTP registration without issuing a session")
    public ApiResponse<RegistrationStepView> startRegistrationMobile(
            @Valid @RequestBody MobileOtpStartRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authenticationService.startRegistrationMobile(new MobileOtpStartCommand(
                request.mobileNumber(),
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"))));
    }

    @PostMapping("/register/mobile/verify")
    @Operation(summary = "Verify registration mobile OTP and advance onboarding")
    public ApiResponse<RegistrationStepView> verifyRegistrationMobile(
            @Valid @RequestBody MobileOtpVerifyRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authenticationService.verifyRegistrationMobile(new MobileOtpVerifyCommand(
                request.otpToken(),
                request.otp(),
                request.deviceId(),
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"))));
    }

    @PostMapping("/register/email/skip")
    @Operation(summary = "Skip optional registration email")
    public ApiResponse<RegistrationStepView> skipRegistrationEmail(@Valid @RequestBody RegistrationSessionRequest request) {
        return ApiResponse.success(authenticationService.skipRegistrationEmail(request.registrationSessionToken()));
    }

    @PostMapping("/register/email/start")
    @Operation(summary = "Start optional registration email OTP")
    public ApiResponse<RegistrationStepView> startRegistrationEmail(@Valid @RequestBody RegistrationEmailStartRequest request) {
        return ApiResponse.success(authenticationService.startRegistrationEmail(new RegistrationEmailStartCommand(
                request.registrationSessionToken(),
                request.email())));
    }

    @PostMapping("/register/email/verify")
    @Operation(summary = "Verify registration email OTP")
    public ApiResponse<RegistrationStepView> verifyRegistrationEmail(@Valid @RequestBody RegistrationEmailVerifyRequest request) {
        return ApiResponse.success(authenticationService.verifyRegistrationEmail(new RegistrationEmailVerifyCommand(
                request.registrationSessionToken(),
                request.otp())));
    }

    @PostMapping("/register/password")
    @Operation(summary = "Set registration password after email verification")
    public ApiResponse<RegistrationStepView> setRegistrationPassword(@Valid @RequestBody RegistrationPasswordRequest request) {
        return ApiResponse.success(authenticationService.setRegistrationPassword(new RegistrationPasswordCommand(
                request.registrationSessionToken(),
                request.password(),
                request.confirmPassword())));
    }

    @PostMapping("/register/brand")
    @Operation(summary = "Create or reuse onboarding brand and advance registration")
    public ApiResponse<RegistrationStepView> completeRegistrationBrand(@Valid @RequestBody RegistrationBrandRequest request) {
        return ApiResponse.success(authenticationService.completeRegistrationBrand(new RegistrationBrandCommand(
                request.registrationSessionToken(),
                request.brandName(),
                null,
                null,
                null)));
    }

    @PostMapping("/register/product-service")
    @Operation(summary = "Create or reuse onboarding product/service and advance registration")
    public ApiResponse<RegistrationStepView> completeRegistrationProductService(
            @Valid @RequestBody RegistrationProductServiceRequest request
    ) {
        return ApiResponse.success(authenticationService.completeRegistrationProductService(new RegistrationProductServiceCommand(
                request.registrationSessionToken(),
                request.productServiceName())));
    }

    @PostMapping("/register/project-campaign")
    @Operation(summary = "Create or reuse onboarding project/campaign, complete onboarding, and issue a session")
    public ApiResponse<AuthSessionView> completeRegistrationProjectCampaign(
            @Valid @RequestBody RegistrationProjectCampaignRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authenticationService.completeRegistrationProjectCampaign(new RegistrationProjectCampaignCommand(
                request.registrationSessionToken(),
                request.projectCampaignName(),
                request.deviceId(),
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"))));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token and issue a new access token")
    public ApiResponse<AuthSessionView> refresh(
            @Valid @RequestBody RefreshRequest request,
            HttpServletRequest httpServletRequest
    ) {
        return ApiResponse.success(authenticationService.refresh(new RefreshSessionCommand(
                request.refreshToken(),
                request.deviceId(),
                resolveClientIp(httpServletRequest),
                httpServletRequest.getHeader("User-Agent"))));
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Invalidate the current session")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(new LogoutCommand(request.refreshToken(), request.shouldLogoutAllDevices()));
        return ApiResponse.success("Logout completed", null);
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Invalidate all sessions for the current user")
    public ApiResponse<Void> logoutAll() {
        authenticationService.logout(new LogoutCommand(null, true));
        return ApiResponse.success("All sessions logged out", null);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset")
    public ApiResponse<Map<String, String>> forgotPassword(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(
                "Password reset request accepted",
                Map.of("status", "accepted", "delivery", "foundation_pending"));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with a reset token")
    public ApiResponse<Map<String, String>> resetPassword(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(
                "Password reset foundation endpoint is available",
                Map.of("status", "foundation_pending"));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify an email address")
    public ApiResponse<Map<String, String>> verifyEmail(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(
                "Email verification foundation endpoint is available",
                Map.of("status", "foundation_pending"));
    }

    @PostMapping("/resend-verification")
    @Operation(summary = "Resend email verification")
    public ApiResponse<Map<String, String>> resendVerification(@RequestBody(required = false) Map<String, Object> request) {
        return ApiResponse.success(
                "Verification resend request accepted",
                Map.of("status", "accepted", "delivery", "foundation_pending"));
    }

    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get the current authenticated user")
    public ApiResponse<UserView> me() {
        return ApiResponse.success(authenticationService.currentUser());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
