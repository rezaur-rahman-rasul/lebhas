package com.lebhas.creativesaas.auth.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.identity.application.AuthenticationService;
import com.lebhas.creativesaas.identity.application.dto.AuthSessionView;
import com.lebhas.creativesaas.identity.application.dto.LoginCommand;
import com.lebhas.creativesaas.identity.application.dto.LogoutCommand;
import com.lebhas.creativesaas.identity.application.dto.RefreshSessionCommand;
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
                        request.phone(),
                        request.password(),
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
        authenticationService.logout(new LogoutCommand(request.refreshToken(), request.logoutAllDevices()));
        return ApiResponse.success("Logout completed", null);
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
