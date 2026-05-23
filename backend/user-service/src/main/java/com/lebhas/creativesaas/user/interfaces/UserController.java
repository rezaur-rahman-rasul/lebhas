package com.lebhas.creativesaas.user.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.identity.application.UserManagementService;
import com.lebhas.creativesaas.identity.application.dto.UpdateUserCommand;
import com.lebhas.creativesaas.identity.application.dto.UpdateUserStatusCommand;
import com.lebhas.creativesaas.identity.application.dto.UserView;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserManagementService userManagementService;

    public UserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "List users for the active workspace or globally for master users")
    public ApiResponse<List<UserView>> listUsers(@RequestParam(required = false) UserStatus status) {
        return ApiResponse.success(userManagementService.listUsers(status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    @Operation(summary = "Get a single user")
    public ApiResponse<UserView> getUser(@PathVariable UUID id) {
        return ApiResponse.success(userManagementService.getUser(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @Operation(summary = "Update a user profile and workspace role")
    public ApiResponse<UserView> updateUser(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.success(userManagementService.updateUser(new UpdateUserCommand(
                id,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.role())));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('USER_STATUS_UPDATE')")
    @Operation(summary = "Update a user status")
    public ApiResponse<UserView> updateStatus(@PathVariable UUID id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.success(userManagementService.changeStatus(new UpdateUserStatusCommand(id, request.status())));
    }
}
