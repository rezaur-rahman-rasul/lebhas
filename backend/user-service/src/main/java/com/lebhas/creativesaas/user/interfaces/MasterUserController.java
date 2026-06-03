package com.lebhas.creativesaas.user.interfaces;

import com.lebhas.creativesaas.common.api.ApiResponse;
import com.lebhas.creativesaas.identity.application.UserManagementService;
import com.lebhas.creativesaas.identity.application.dto.UpdateUserStatusCommand;
import com.lebhas.creativesaas.identity.application.dto.UserView;
import com.lebhas.creativesaas.identity.domain.UserStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master/users")
@Tag(name = "Master Users")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MASTER')")
public class MasterUserController {

    private final UserManagementService userManagementService;

    public MasterUserController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    @GetMapping
    @Operation(summary = "List users")
    public ApiResponse<List<UserView>> listUsers(@RequestParam(required = false) UserStatus status) {
        return ApiResponse.success(userManagementService.listUsers(status));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a user")
    public ApiResponse<UserView> getUser(@PathVariable UUID userId) {
        return ApiResponse.success(userManagementService.getUser(userId));
    }

    @PutMapping("/{userId}/status")
    @Operation(summary = "Update user status")
    public ApiResponse<UserView> updateStatus(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        return ApiResponse.success(userManagementService.changeStatus(new UpdateUserStatusCommand(userId, request.status())));
    }
}
